package tim260.project;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.Version;

public class Recommend {
	private static Index index;
	public static String repoID;
	
	public Recommend(Date current) throws Exception{
		Properties prop = new Properties();
		prop.load(new FileInputStream("config"));
		repoID = prop.getProperty("RepositoryID");
		index = Index.getInstace();
		
		Integer latestID = getLatestCommit(current);
		if(latestID == null)
			return;
		if(latestID > index.commidID){
			Connection conn = DatabaseManager.getConnection();
			Statement stmt = conn.createStatement();
			// Ensure all indices are in place
			try {
				stmt.executeUpdate("create index commit_id_file_id on line_blames(commit_id, file_id)");
			} catch (SQLException e) {
				if(e.getErrorCode() != 1061)
					throw e;
			}
			ResultSet changedFiles = stmt.executeQuery("select file_id, max(commit_id) as cid" +
					" from actions where commit_id > "
					+ index.commidID + " and commit_id <=" + latestID + " group by file_id");
			while(changedFiles.next()){
				int fileID = changedFiles.getInt("file_id");
				int commitID = changedFiles.getInt("cid");
				deleteFile(fileID);
				addFile(fileID, commitID);
			}
			changedFiles.close();
			stmt.close();
			System.out.println("Number of docs in index: "+index.getReader().maxDoc());
			index.commidID = latestID;
		}else if(latestID < index.commidID){
			System.err.println("Can to go backward!");
			System.exit(1);
		}
		
	}
		
	public ScoreDoc[] getHunks(String desc) throws Exception {
		String[] cAndT = separateCodeAndText(desc);
		IndexReader reader = index.getReader();
		IndexSearcher searcher = new IndexSearcher(reader);
		Version ver = Version.LUCENE_34;
		QueryParser parser = new QueryParser(ver, "text", new MyAnalyzer(ver));
		String text = cAndT[1];
		LinkedList<String> fileNames = extractFileNames(text);
		text = text.replaceAll("\\W", " ");
		StringBuilder queryString = new StringBuilder(text);
		if(cAndT[0].length()>0)
		{
			queryString.append(" code:(");
			queryString.append(cAndT[0]);
			queryString.append(")");
		}
		if(fileNames.size()>0){
			queryString.append(" AND (");
			for(String f : fileNames){
				queryString.append(" path:");
				queryString.append(f);
				queryString.append(" OR ");
			}
			queryString.delete(queryString.length()-3, queryString.length());
			queryString.append(")");		
		}
		
		Query query = parser.parse(queryString.toString());
		ScoreDoc[] scoreDocs = searcher.search(query, 1000).scoreDocs;
		return scoreDocs;
	}
	
	public static Integer getLatestCommit(Date date) throws Exception{
		Connection conn = DatabaseManager.getConnection();
		PreparedStatement stmt = conn
				.prepareStatement("select id from scmlog" +
						" where commit_date < ? and repository_id=?" +
						" order by commit_date desc limit 1");
		stmt.setDate(1, new java.sql.Date(date.getTime()));
		stmt.setString(2, repoID);

		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			int id = rs.getInt("id");
			rs.close();
			stmt.close();
			return id;
		} else {
			return null;
		}
	}
	
	public void deleteFile(int fileID) throws Exception{
		Term term = new Term("file_id", String.valueOf(fileID));
		IndexWriter writer = index.getWriter();
		writer.deleteDocuments(term);
		writer.commit();
	}
	
	public void addFile(int fileID, int commitID) throws Exception{
		Connection conn = DatabaseManager.getConnection();
		IndexWriter writer = index.getWriter();
		Statement stmt = conn.createStatement();
		ResultSet action = stmt.executeQuery("select type, current_file_path from actions " +
				"where file_id="+fileID+" and commit_id="+commitID);
		action.next();
		String path = action.getString("current_file_path");
		String type = action.getString("type");
		if(type.equals("D"))
			return;
		action.close();
		
		String[] content = null;
		ResultSet contentRS = stmt
				.executeQuery("select content from content where file_id="
						+ fileID + " and commit_id=" + commitID);
		if(contentRS.next()){
			content = contentRS.getString("content").split("\n");
		}
		contentRS.close();
		
		ResultSet hunks = stmt.executeQuery("select * from line_blames where file_id="
				+ fileID + " and commit_id=" + commitID);
		while(hunks.next()){
			StringBuilder text = new StringBuilder();
			StringBuilder code = new StringBuilder();
			// add comments
			int start = hunks.getInt("start");
			int end = hunks.getInt("end");
			if(content != null){
				String[] cAndC = separateCodeAndComment(content, start, end);
				code.append(cAndC[0]);
				text.append(cAndC[1]);
			}
			// add commit logs
			int blameCommitID = hunks.getInt("blame_commit_id");
			Statement stmt2 = conn.createStatement();
			ResultSet msg = stmt2.executeQuery("select message from scmlog where id="+blameCommitID);
			msg.next();
			text.append(msg.getString("message"));
			msg.close();
			// add related issues
			ResultSet issues = stmt2.executeQuery("select title, body " +
					"from issues i join issues_commits ic on i.id=ic.issue_id " +
					"where ic.commit_id="+blameCommitID);
			while(issues.next()){
				text.append(issues.getString("title"));
				String[] cAndT = separateCodeAndText(issues.getString("body"));
				code.append(cAndT[0]);
				text.append(cAndT[1]);
			}
			issues.close();
			// Construct Document
			Document doc = new Document();
			Field textField = new Field("text", text.toString().replaceAll("\\W", " "), Field.Store.YES, Field.Index.ANALYZED);
			doc.add(textField);
			doc.add(new Field("code", code.toString(), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("file_id", String.valueOf(fileID), Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("start", String.valueOf(start), Field.Store.YES, Field.Index.NO));
			doc.add(new Field("end", String.valueOf(start), Field.Store.YES, Field.Index.NO));
			doc.add(new Field("path", path, Field.Store.YES, Field.Index.NOT_ANALYZED));
			writer.addDocument(doc);
		}
		writer.commit();
		hunks.close();
		stmt.close();
		
	}
	
	public static String[] separateCodeAndComment(String[] content, int start, int end){
		// Comments before the hunk
		int i = start-2;
		while(i> 0 && !content[i].trim().startsWith("#"))
		{i--;}
		StringBuilder comment = new StringBuilder();
		StringBuilder code = new StringBuilder();
		while(i>0 && content[i].trim().startsWith("#")){
			comment.append(StringUtils.substringAfter(content[i], "#").trim());
			i--;
		}
		// Inline comments of the hunk
		for(i=start-1; i<end; i++){
			code.append(StringUtils.substringBefore(content[i], "#").trim());
			comment.append(StringUtils.substringAfter(content[i], "#").trim());
		}
		return new String[]{code.toString(), comment.toString()};
	}
	
	public static String[] separateCodeAndText(String string){
		StringBuilder code = new StringBuilder();
		StringBuilder temp = new StringBuilder();
		StringBuilder text = new StringBuilder();
		while(string != null && string.length()>0){
			temp.append(StringUtils.substringBefore(string, "```"));
			String snippet = StringUtils.substringBetween(string, "```");
			if(snippet != null){
				code.append(snippet.trim());
				string = StringUtils.substringAfter(string, "```");
			}
			string = StringUtils.substringAfter(string, "```");		
		}
		
		String[] lines = temp.toString().split("\\n");
		for(String line : lines){
			if(line.startsWith("    ")){
				code.append(line.trim());
			}else{
				text.append(line.trim());
			}
		}
		
		return new String[]{code.toString(), temp.toString()};
	}
	
	public static LinkedList<String> extractFileNames(String text){
		LinkedList<String> names = new LinkedList<String>();
		String regex = "((ci|activerecord|actionmailer|actionpack|activemodel|activerecord|activeresource|activesupport|railties)[^\\s]+\\.rb)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(text);
		while(matcher.find()){
			String fileName = matcher.group(1);
			names.add(fileName);
		}
		text.replaceAll(regex, " ");
		return names;
	}
}
