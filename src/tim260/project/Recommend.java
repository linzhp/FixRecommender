package tim260.project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.lucene.store.RAMDirectory;

public class Recommend {
	private static Date current;
	private static RAMDirectory index;
	public static int latestCommitID;
	
	public Recommend(Date current){
		DateFormat.getDateInstance().format(current);
		
		
		Recommend.current = current;
	}
	
	public ArrayList<Integer> getHunks(String desc){
		ArrayList<Integer> result = new ArrayList<Integer>();
		return result;
	}
	
	public boolean indexDated(Date date) throws Exception{
		if(latestCommitID == 0){
			return true;
		}
		Connection conn = DatabaseManager.getConnection();
		PreparedStatement stmt = conn
				.prepareStatement("select id from scmlog" +
						" where commit_date < ?" +
						" order by commit_date desc limit 1");
		stmt.setDate(1, new java.sql.Date(date.getTime()));
		ResultSet rs = stmt.executeQuery();
		rs.next();
		if(rs.getInt("id") == latestCommitID)
			return false;
		else
			return true;
	}
}
