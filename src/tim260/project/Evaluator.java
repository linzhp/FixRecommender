package tim260.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;

public class Evaluator {
	private static final class ScoreDocComparator implements
			Comparator<ScoreDoc> {
		private IndexReader reader;

		public ScoreDocComparator() throws Exception{
			Index index;
			index = Index.getInstace();
			reader = index.getReader();
			
		}
		
		@Override
		public int compare(ScoreDoc o1, ScoreDoc o2) {
			try {
				int start1 = Integer.valueOf(reader.document(o1.doc).get("start"));
				int start2 = Integer.valueOf(reader.document(o2.doc).get("start"));
				return start1 - start2;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}
	}

	public static void main(String[] args) throws Exception{
		Properties prop = new Properties();
		prop.load(new FileInputStream("config"));
		String repoID = prop.getProperty("RepositoryID");
		

		Connection conn = DatabaseManager.getConnection();
		Statement stmt = conn.createStatement();
		ResultSet issues = stmt.executeQuery("select * from issues where repository_id="+repoID+" order by created_at");
		FileWriter writer = new FileWriter(new File("results.csv"));
		writer.write("issue_id,predNum,predHits,actNum,recallNum\n");
		while(issues.next()){
			int issueID = issues.getInt("id");
			String title = issues.getString("title");
			String body = issues.getString("body");
			Date date = issues.getDate("created_at");
			
			System.out.println("processing issue "+issueID);
			Statement stmt2 = conn.createStatement();
			ResultSet commits = stmt2.executeQuery("select count(*) from issues_commits where issue_id="
					+issueID+" and type = 'Attach'");
			commits.next();
			if(commits.getInt(1)==0)
				continue;
			commits.close();
			
			int predHits = 0;
			int recalled = 0;
			int totalActHunks = 0;
			Recommend rec = new Recommend(date);
			ScoreDoc[] predHunks = rec.getHunks(title+" "+body);
			HashMap<Integer, SortedSet<ScoreDoc>> files = new HashMap<Integer, SortedSet<ScoreDoc>>();
			for(ScoreDoc sDoc : predHunks){
				Integer fileID = getValue(sDoc, "file_id");
				SortedSet<ScoreDoc> hunkSet = files.get(fileID);
				if(hunkSet == null){
					hunkSet = new TreeSet<ScoreDoc>(new ScoreDocComparator());
					files.put(fileID, hunkSet);
				}
				hunkSet.add(sDoc);
			}
			
			stmt2 = conn.createStatement();
			commits = stmt2.executeQuery("select * from issues_commits where issue_id="
					+issueID+" and type = 'Attach'");
			while(commits.next()){
				int commitID = commits.getInt("commit_id");
				Statement stmt3 = conn.createStatement();
				ResultSet actHunks = stmt3.executeQuery("select * from hunks where commit_id="
						+commitID+" order by file_id,old_start_line");
				ScoreDoc curHunk = null;
				Iterator<ScoreDoc> iterator=null;
				while(actHunks.next()){
					totalActHunks++;
					boolean isRecalled = false;
					int fileID = actHunks.getInt("file_id");
					if(fileID != getValue(curHunk, "file_id")){
						SortedSet<ScoreDoc> file = files.get(fileID);
						if(file == null){
							continue;
						}
						iterator = file.iterator();
						curHunk = iterator.next();
					}
					// pred.end < act.start: not yet overlap
					while(curHunk != null && 
							getValue(curHunk,"end") 
							< actHunks.getInt("old_start_line")){
						curHunk = iterator.next();
					}
					// pred.start <= act.end: still overlapping
					int actEnd = actHunks.getInt("old_end_line");
					while(curHunk != null &&
							getValue(curHunk,"start")
							<= actEnd){
						isRecalled = true;
						predHits++;
						if(actEnd < getValue(curHunk,"end")){
							// act.end < pred.end
							// maybe more actual hunks overlapped with this predict hunk
							break;
						}
						curHunk = iterator.next();
					}
					recalled+=(isRecalled? 1:0);
				}// end hunks loop
				actHunks.close();
				stmt3.close();
			}// end commits loop
			commits.close();
			stmt2.close();
			StringBuilder resultLine = new StringBuilder();
			resultLine.append(issueID);
			resultLine.append(",");
			resultLine.append(predHunks.length);
			resultLine.append(",");
			resultLine.append(predHits);
			resultLine.append(",");
			resultLine.append(totalActHunks);
			resultLine.append(",");
			resultLine.append(recalled);
			resultLine.append('\n');
			writer.write(resultLine.toString());
			writer.flush();
		}// end issues loop
		writer.close();
		issues.close();
		stmt.close();
	}
	
	public static int getValue(ScoreDoc sDoc, String field) throws Exception{
		if(sDoc == null)
			return -1;
		Index index = Index.getInstace();
		IndexReader reader = index.getReader();
		Document doc = reader.document(sDoc.doc);
		return Integer.valueOf(doc.get(field));
	}
}
