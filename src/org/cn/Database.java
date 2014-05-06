package org.cn;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;

public class Database {
	public static final String NAME = "appointments";
	
	public static void main(String[] args) throws SQLException {
		createTable(Appointment.TABLE_NAME);
	}
	
	public static void createTable(String tableName) throws SQLException {
		Connection conn = getConnection();
		Statement stmt = conn.createStatement();
		String sql = "CREATE TABLE IF NOT EXISTS " + tableName +
                " (id               INTEGER      PRIMARY KEY, " +
                " uuid              VARCHAR(255) UNIQUE NOT NULL, " + 
                " header            VARCHAR(255), " + 
                " startAt           VARCHAR(255), " +
                " durationInMinutes VARCHAR(255), " +
                " comment           TEXT)"; 
		stmt.executeUpdate(sql);
	    stmt.close();
	    conn.close();
	    System.out.println("Table is created.");
	}
	
	public static Connection getConnection() {
		Connection conn = null;
	    try {
	      Class.forName("org.sqlite.JDBC");
	      conn = DriverManager.getConnection("jdbc:sqlite:" + NAME +".db");
	    } catch ( Exception e ) {
	      System.err.println(e.getClass().getName() + ": " + e.getMessage() );
	    }
	    return conn;
	}
}
