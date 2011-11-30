package tim260.project;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;

import static java.lang.System.out;

public class PullIssues {
	private static void createTables(Connection conn) throws Exception {
		Statement stmt = conn.createStatement();
//		stmt.executeUpdate("DROP TABLE IF EXISTS issues");
//		stmt.executeUpdate("DROP TABLE IF EXISTS issues_commits");
		String query = "CREATE TABLE IF NOT EXISTS issues("
			+ "id INTEGER PRIMARY KEY AUTO_INCREMENT,"
			+ "issue_number INTEGER NOT NULL,"
			+ "repository_id INTEGER REFERENCES repositories(id),"
			+ "title VARCHAR(255), body MEDIUMTEXT," +
					"created_at DATETIME)";
		stmt.executeUpdate(query);
		try {
			stmt.executeUpdate("CREATE INDEX repository_id ON issues(repository_id)");
		} catch (SQLException e) {
			if(e.getErrorCode() != 1061){
				throw e;
			}
		}
		try {
			stmt.executeUpdate("CREATE UNIQUE INDEX issue_number_repository_id ON" +
					" issues(issue_number, repository_id)");
		} catch (SQLException e) {
			if(e.getErrorCode() != 1061)
				throw e;
		}
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS issues_commits("
				+ "issue_id INTEGER REFERENCES issues(id),"
				+ "commit_id INTEGER REFERENCES scmlog(id),"
				+ "PRIMARY KEY (issue_id, commit_id)" + ")");
		try {
			stmt.executeUpdate("CREATE INDEX issue_id ON issues_commits(issue_id)");
		} catch (SQLException e) {
			if(e.getErrorCode() != 1061)
				throw e;
		}
		stmt.close();
	}

	public static void main(String args[]) throws Exception {
		Connection conn = DatabaseManager.getConnection();
		createTables(conn);
		// Load config
		MyIssueService service = new MyIssueService();
		Properties prop = new Properties();
		prop.load(new FileInputStream("config"));
		String repoID = prop.getProperty("RepositoryID");
		// Pull data from Github
		HashMap<String, String> filter = new HashMap<String, String>();
		filter.put("state", "closed");
		List<Issue> issues = service.getIssues("rails", "rails", filter);
		IRepositoryIdProvider repo = RepositoryId.create("rails", "rails");
		// Get existing issue numbers
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select id, issue_number from issues where repository_id="+repoID);
		HashMap<Integer, Integer> existingIssues = new HashMap<Integer, Integer>();
		while(rs.next()){
			existingIssues.put(rs.getInt("issue_number"), rs.getInt("id"));
		}
		rs.close();
		// Construct prepared statements
		PreparedStatement insertStmt = conn
				.prepareStatement(
						"INSERT INTO issues(title, body, created_at, issue_number, repository_id)"
								+ "VALUES(?, ?, ?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
		PreparedStatement updateStmt = conn.prepareStatement("update issues set title=?," +
				" body=?, created_at=?" +
				" where issue_number=? and repository_id=?");
		// Process each issue
		for (Issue i : issues) {
			int issueNumber = i.getNumber();
			Date createdAt = i.getCreatedAt();
			out.println("Processing issue #" + issueNumber);
			StringBuilder body = new StringBuilder(i.getBody());
			if (i.getComments() > 0) {
				List<Comment> comments = service.getComments("rails", "rails",
						issueNumber);
				for (Comment c : comments) {
					body.append('\n');
					body.append(c.getBody());
				}
			}
			Integer issueID = existingIssues.get(issueNumber);
			PreparedStatement pStmt;
			if(issueID == null){
				pStmt = insertStmt;
			}else{
				pStmt = updateStmt;
			}
			pStmt.setString(1, i.getTitle());
			pStmt.setString(2, body.toString());
			pStmt.setDate(3, new java.sql.Date(createdAt.getTime()));
			pStmt.setInt(4, issueNumber);
			pStmt.setString(5, repoID);
			pStmt.execute();
			if (issueID == null) {
				// newly inserted record
				rs = pStmt.getGeneratedKeys();
				rs.next();
				issueID = rs.getInt(1);
			}
			rs.close();
			if (i.getPullRequest().getPatchUrl() != null) {
				List<RepositoryCommit> commits = service.getCommits(repo,
						issueNumber);
				for (RepositoryCommit c : commits) {
					rs = stmt.executeQuery("SELECT id FROM scmlog WHERE rev='"
							+ c.getSha() + "'");
					if (rs.next())
						try {
							stmt.executeUpdate("INSERT INTO issues_commits "
									+ "VALUES(" + issueID + ',' + rs.getInt(1)
									+ ")");
						} catch (SQLException e) {
							if(e.getErrorCode() != 1062)
								throw e;
						}
					rs.close();
				}
			}

		}
		stmt.close();
		insertStmt.close();
		updateStmt.close();
		conn.close();
	}
}
