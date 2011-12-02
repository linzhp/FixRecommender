package tim260.project;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.apache.lucene.store.RAMDirectory;

public class Recommend {
	private static Date current;
	private static RAMDirectory index;
	public static int latestCommitID;
	private String repoID;
	
	public Recommend(Date current) throws Exception{
		Properties prop = new Properties();
		prop.load(new FileInputStream("config"));
		repoID = prop.getProperty("RepositoryID");
		
		DateFormat.getDateInstance().format(current);
		if(indexDated(current)){
			
		}
		
		Recommend.current = current;
	}
	
	public ArrayList<Integer> getHunks(String desc){
		ArrayList<Integer> result = new ArrayList<Integer>();
		return result;
	}
	
	public boolean indexDated(Date date) throws Exception{
		Connection conn = DatabaseManager.getConnection();
		PreparedStatement stmt = conn
				.prepareStatement("select id from scmlog" +
						" where commit_date < ? and repository_id=?" +
						" order by commit_date desc limit 1");
		stmt.setDate(1, new java.sql.Date(date.getTime()));
		stmt.setString(2, repoID);

		ResultSet rs = stmt.executeQuery();
		rs.next();
		int id = rs.getInt("id");
		if(id == latestCommitID)
			return false;
		else
		{
			latestCommitID = id;
			return true;
		}
	}
}
