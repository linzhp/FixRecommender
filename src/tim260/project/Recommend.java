package tim260.project;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

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
			index.commidID = latestID;
		}else if(latestID < index.commidID){
			System.err.println("Can to go backward!");
			System.exit(1);
		}
		
	}
		
	public ArrayList<FixCandicate> getHunks(String desc){
		ArrayList<FixCandicate> result = new ArrayList<FixCandicate>();
		return result;
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
			// add comments
			int start = hunks.getInt("start");
			int end = hunks.getInt("end");
			if(content != null){
				text.append(getComment(content, start, end));
			}
			// add commit logs
			int blameCommitID = hunks.getInt("blame_commit_id");
			Statement stmt2 = conn.createStatement();
			ResultSet msg = stmt2.executeQuery("select message from scmlog where id="+blameCommitID);
			msg.next();
			text.append(msg.getString("message"));
			msg.close();
			// add related issues
			ResultSet issues = stmt2.executeQuery("select i.title, i.body " +
					"from issues i join issues_commits ic on i.id=ic.issue_id " +
					"where ic.commit_id="+blameCommitID);
			while(issues.next()){
				text.append(issues.getString(1));
				text.append(issues.getString(2));
			}
			issues.close();
			// Construct Document
			Document doc = new Document();
			Field textField = new Field("text", text.toString(), Field.Store.YES, Field.Index.ANALYZED);
			doc.add(textField);
			doc.add(new Field("file_id", String.valueOf(fileID), Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("start", String.valueOf(start), Field.Store.YES, Field.Index.NO));
			doc.add(new Field("end", String.valueOf(start), Field.Store.YES, Field.Index.NO));
			doc.add(new Field("path", path, Field.Store.YES, Field.Index.NO));
			writer.addDocument(doc);
		}
		writer.commit();
		hunks.close();
		stmt.close();
		
	}
	
	public static String getComment(String[] content, int start, int end){
		// Comments before the hunk
		int i = start-2;
		while(i> 0 && !content[i].trim().startsWith("#"))
		{i--;}
		StringBuilder comment = new StringBuilder();
		while(i>0 && content[i].trim().startsWith("#")){
			int commentIdx = content[i].indexOf("#");
			comment.append(content[i].substring(commentIdx+1));
			i--;
		}
		// Inline comments of the hunk
		for(i=start-1; i<end; i++){
			int commentIdx = content[i].indexOf("#");
			if(commentIdx != -1){
				comment.append(content[i].substring(commentIdx+1));
			}
		}
		return comment.toString();
	}
}
