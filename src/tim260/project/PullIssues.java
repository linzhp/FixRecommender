package tim260.project;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;

import static java.lang.System.out;

public class PullIssues {
	private static void createTables(Connection conn) throws Exception {
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("DROP TABLE IF EXISTS issues");
		stmt.executeUpdate("DROP TABLE IF EXISTS issues_commits");
		stmt.executeUpdate("CREATE TABLE issues("
				+ "id INTEGER PRIMARY KEY AUTO_INCREMENT,"
				+ "issue_number INTEGER NOT NULL,"
				+ "repository_id INTEGER REFERENCES repositories(id),"
				+ "title VARCHAR(255)," + "body MEDIUMTEXT" + ")");
		stmt.executeUpdate("CREATE INDEX repository_id ON issues(repository_id)");
		stmt.executeUpdate("CREATE TABLE issues_commits("
				+ "issue_id INTEGER REFERENCES issues(id),"
				+ "commit_id INTEGER REFERENCES scmlog(id),"
				+ "PRIMARY KEY (issue_id, commit_id)" + ")");
		stmt.close();
	}

	public static void main(String args[]) throws Exception {
		Connection conn = DatabaseManager.getConnection();
		createTables(conn);

		MyIssueService service = new MyIssueService();
		Properties prop = new Properties();
		prop.load(new FileInputStream("config"));
		String repoID = prop.getProperty("RepositoryID");

		TreeMap<String, String> filter = new TreeMap<String, String>();
		filter.put("state", "closed");
		List<Issue> issues = service.getIssues("rails", "rails", filter);
		IRepositoryIdProvider repo = RepositoryId.create("rails", "rails");
		PreparedStatement pStmt = conn
				.prepareStatement(
						"INSERT INTO issues(issue_number, repository_id, title, body)"
								+ "VALUES(?, ?, ?, ?)",
						Statement.RETURN_GENERATED_KEYS);
		Statement stmt = conn.createStatement();
		for (Issue i : issues) {
			int issueNumber = i.getNumber();
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
			pStmt.setInt(1, issueNumber);
			pStmt.setString(2, repoID);
			pStmt.setString(3, i.getTitle());
			pStmt.setString(4, body.toString());
			pStmt.execute();
			ResultSet rs = pStmt.getGeneratedKeys();
			rs.next();
			int issueID = rs.getInt(1);
			rs.close();
			if (i.getPullRequest().getPatchUrl() != null) {
				List<RepositoryCommit> commits = service.getCommits(repo,
						issueNumber);
				for (RepositoryCommit c : commits) {
					rs = stmt.executeQuery("SELECT id FROM scmlog WHERE rev='"
							+ c.getSha() + "'");
					if (rs.next())
						stmt.executeUpdate("INSERT INTO issues_commits "
								+ "VALUES(" + issueID + ',' + rs.getInt(1)
								+ ")");
					rs.close();
				}
			}

		}
		stmt.close();
		pStmt.close();
		conn.close();
	}
}
