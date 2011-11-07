package tim260.project;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscoverLinks {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		prop.load(new FileInputStream("config"));
		String repoID = prop.getProperty("RepositoryID");
		Connection conn = DatabaseManager.getConnection();
		Statement stmt = conn.createStatement();
		//Parse commit logs, new links found: 3566
		ResultSet commits = stmt.executeQuery("select id, message from scmlog where repository_id="
				+repoID);
		while(commits.next()){
			LinkedList<String> numbers = parseIssueNumber(commits.getString("message"));
			for(String n : numbers){
				String issueID = getIssueID(repoID, n);
				if(issueID != null)
					insertIssueCommit(issueID, commits.getString("id"));
			}
		}
		// Parse issues
		ResultSet issues = stmt.executeQuery("select * from issues where repository_id="
				+repoID);
		while(issues.next()){
			String body = issues.getString("body");
			String id = issues.getString("id");
			String title = issues.getString("title");
			// Parse Revisions
			LinkedList<String> revs = parseRev(body);
			processRevs(id, revs);
			// Parse issue numbers
			LinkedList<String> relatedIssues = parseIssueNumber(body);
			processRelatedIssues(id, relatedIssues, repoID);// 1766
			relatedIssues=parseIssueNumber(title);
			processRelatedIssues(id, relatedIssues, repoID);//629
		}
		issues.close();
		stmt.close();
		conn.close();
	}
	
	private static String getIssueID(String repoID, String number) throws Exception{
		String id = null;
		Connection conn = DatabaseManager.getConnection();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select id from issues where issue_number = "
				+number+" and repository_id="+repoID);
		if(rs.next()){
			id = rs.getString(1);
		}
		stmt.close();
		return id;
	}

	private static void processRevs(String id,
			LinkedList<String> revs) throws Exception {
		Connection conn = DatabaseManager.getConnection();
		Statement stmt = conn.createStatement();
		for(String r : revs){
			ResultSet commit = stmt.executeQuery("select id from scmlog where rev like '"+r+"%'");
			if(commit.next()){
				String commitID = commit.getString(1);
				insertIssueCommit(id, commitID);
			}
			commit.close();
		}
		stmt.close();
	}
	
	private static void processRelatedIssues(String id,
			LinkedList<String> relatedIssues, String repoID) throws Exception{
		Connection conn = DatabaseManager.getConnection();
		Statement stmt = conn.createStatement();
		LinkedList<String> relatedCommits = new LinkedList<String>();
		ResultSet rs = stmt.executeQuery("select commit_id from issues_commits where issue_id="+id);
		while(rs.next()){
			relatedCommits.add(rs.getString(1));
		}
		rs.close();
		for(String n : relatedIssues){
			String issueID = getIssueID(repoID, n);
			if(issueID != null){
				for(String commitID : relatedCommits){
					insertIssueCommit(issueID, commitID);
				}
			}
		}
		stmt.close();
	}
	
	private static void insertIssueCommit(String issueID, String commitID) throws Exception{
		Connection conn = DatabaseManager.getConnection();
		Statement stmt = conn.createStatement();
		try {
			stmt.executeUpdate("insert into issues_commits "
					+ "values(" + issueID + "," + commitID + ")");
		} catch (SQLException e) {
			if(e.getErrorCode() != 1062)
				throw e;
		}
	}

	public static LinkedList<String> parseRev(String text){
		LinkedList<String> revs = new LinkedList<String>();
		Pattern pattern = Pattern.compile("([a-f0-9]{5,40})");
		Matcher matcher = pattern.matcher(text);
		while(matcher.find()){
			String hash = matcher.group(1);
			if(hash.matches(".+[0-9].+") && hash.matches(".+[a-f].+"))
				revs.add(hash);
		}
		return revs;
	}
	
	public static LinkedList<String> parseIssueNumber(String text){
		LinkedList<String> numbers = new LinkedList<String>();
		Pattern pattern = Pattern.compile("#([0-9]{1,4})");
		Matcher matcher = pattern.matcher(text);
		while(matcher.find()){
			String n = matcher.group(1);
			numbers.add(n);
		}
		return numbers;
	}
	
}
