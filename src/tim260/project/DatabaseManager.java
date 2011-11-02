package tim260.project;


import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DatabaseManager {

    private static String databasename, username, password;
    private static Connection conn = null;

    private static void initCredentials() throws Exception{
        File file = new File("config");
        FileInputStream fis = null;
        fis = new FileInputStream(file);
        Properties prop = new Properties();
        prop.load(fis);
        databasename = (String) prop.get("URL");
        username = (String) prop.get("UserName");
        password = (String) prop.get("UserPass");
        fis.close();
    }
    
    public static Connection getConnection() throws Exception {
        if (conn == null || conn.isClosed()) {
        	if(databasename == null){
        		initCredentials();
        	}
            conn = DriverManager
            .getConnection(databasename, username, password);
        }
        return conn;
    }

}