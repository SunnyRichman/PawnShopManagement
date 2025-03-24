import java.sql.*;

public class db_Connection {
	
	private static final String URL = "jdbc:mysql://localhost:3306/yaowarat pawn shop?useUnicode=true&characterEncoding=UTF-8";
	private static final String USER = "yaowarat_admin";
	private static final String PASS = "yaowarat_admin";
	
	public db_Connection() {
		
	}
	
	public static Connection getConnection() throws SQLException{
		return DriverManager.getConnection(URL, USER, PASS); 
	}
}
