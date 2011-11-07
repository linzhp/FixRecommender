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

public class ParseIssues {

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
		ResultSet issues = stmt.executeQuery("select * from issues where repository_id="
				+repoID);
		while(issues.next()){
			String body = issues.getString("body");
			String id = issues.getString("id");
			LinkedList<String> revs = parseRev(body);
			Statement stmt2 = conn.createStatement();
			for(String r : revs){
				ResultSet commit = stmt2.executeQuery("select id from scmlog where rev like '"+r+"%'");
				if(commit.next()){
					String commitID = commit.getString(1);
					try {
						stmt2.executeUpdate("insert into issues_commits "
								+ "values(" + id + "," + commitID + ")");
					} catch (SQLException e) {
						if(e.getErrorCode() != 1062)
							throw e;
					}
				}
				commit.close();
			}
			stmt2.close();
		}
		issues.close();
		stmt.close();
		conn.close();
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
}
