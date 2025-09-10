package com.example.attendance.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtils{
	private static final String URL = "jdbc:postgresql://localhost:5432/attendance_db";
	private static final String USER = "postgres";
	private static final String PASSWORD = "postgres";
	
	static {
        try {
            // PostgreSQL JDBCドライバを明示的にロード
            Class.forName("org.postgresql.Driver");
            System.out.println("PostgreSQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found.");
            e.printStackTrace();
            // ドライバが見つからない場合はRuntimeExceptionをスロー
            throw new RuntimeException("JDBCドライバのロードに失敗しました。", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        System.out.println("Attempting to connect to database...");
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        System.out.println("Database connection successful.");
        return conn;
    }
	
}