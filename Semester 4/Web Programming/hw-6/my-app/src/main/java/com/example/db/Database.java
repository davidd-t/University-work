package com.example.db;

import java.sql.*;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:route.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            initializeDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        conn.setAutoCommit(true);
        return conn;
    }

    private static void initializeDB() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            
            // Create users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL)");
            
            // Create cities table
            stmt.execute("CREATE TABLE IF NOT EXISTS cities (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT UNIQUE NOT NULL)");
            
            // Create connections table (neighboring cities)
            stmt.execute("CREATE TABLE IF NOT EXISTS connections (" +
                "from_city_id INTEGER, " +
                "to_city_id INTEGER, " +
                "FOREIGN KEY(from_city_id) REFERENCES cities(id), " +
                "FOREIGN KEY(to_city_id) REFERENCES cities(id), " +
                "PRIMARY KEY(from_city_id, to_city_id))");
            
            // Create routes table (user journey)
            stmt.execute("CREATE TABLE IF NOT EXISTS routes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "current_city_id INTEGER, " +
                "completed INTEGER DEFAULT 0, " +
                "FOREIGN KEY(user_id) REFERENCES users(id), " +
                "FOREIGN KEY(current_city_id) REFERENCES cities(id))");
            
            // Create route_steps table (path history)
            stmt.execute("CREATE TABLE IF NOT EXISTS route_steps (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "route_id INTEGER, " +
                "city_id INTEGER, " +
                "step_order INTEGER, " +
                "FOREIGN KEY(route_id) REFERENCES routes(id), " +
                "FOREIGN KEY(city_id) REFERENCES cities(id))");
            
            // Insert sample data
            insertSampleData(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void insertSampleData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Check if already initialized
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            rs.next();
            if (rs.getInt(1) > 0) return;
            
            // Add users
            stmt.execute("INSERT INTO users(username, password) VALUES('user1', 'pass1')");
            stmt.execute("INSERT INTO users(username, password) VALUES('user2', 'pass2')");
            
            // Add cities
            stmt.execute("INSERT INTO cities(name) VALUES('New York')");
            stmt.execute("INSERT INTO cities(name) VALUES('Boston')");
            stmt.execute("INSERT INTO cities(name) VALUES('Philadelphia')");
            stmt.execute("INSERT INTO cities(name) VALUES('Washington DC')");
            stmt.execute("INSERT INTO cities(name) VALUES('Chicago')");
            
            // Add connections
            stmt.execute("INSERT INTO connections VALUES(1, 2)"); // NY -> Boston
            stmt.execute("INSERT INTO connections VALUES(2, 1)"); // Boston -> NY
            stmt.execute("INSERT INTO connections VALUES(1, 3)"); // NY -> Philadelphia
            stmt.execute("INSERT INTO connections VALUES(3, 1)"); // Philadelphia -> NY
            stmt.execute("INSERT INTO connections VALUES(3, 4)"); // Philadelphia -> DC
            stmt.execute("INSERT INTO connections VALUES(4, 3)"); // DC -> Philadelphia
            stmt.execute("INSERT INTO connections VALUES(1, 5)"); // NY -> Chicago
            stmt.execute("INSERT INTO connections VALUES(5, 1)"); // Chicago -> NY
        }
    }
}
