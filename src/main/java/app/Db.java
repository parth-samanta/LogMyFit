package app;

import java.sql.*;

public class Db {
    private static final String URL = "jdbc:sqlite:fitgirl.db";

    static {
        // Create tables on first load
        try (Connection c = get(); Statement st = c.createStatement()) {

            // Users
            st.execute("""
                CREATE TABLE IF NOT EXISTS users(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  username TEXT UNIQUE NOT NULL,
                  password_hash TEXT NOT NULL,
                  email TEXT,
                  created_at TEXT DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // logs
            st.execute("""
                CREATE TABLE IF NOT EXISTS daily_logs(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  user_id INTEGER NOT NULL,
                  date TEXT NOT NULL,              -- YYYY-MM-DD
                  steps INTEGER DEFAULT 0,
                  calories INTEGER DEFAULT 0,
                  protein REAL DEFAULT 0,
                  carbohydrates REAL DEFAULT 0,
                  fats REAL DEFAULT 0,
                  workout_type TEXT,
                  notes TEXT,
                  FOREIGN KEY(user_id) REFERENCES users(id)
                );
            """);

            // goals
            st.execute("""
                CREATE TABLE IF NOT EXISTS daily_goals(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  user_id INTEGER NOT NULL,
                  date TEXT NOT NULL,              -- YYYY-MM-DD
                  steps_goal INTEGER DEFAULT 0,
                  calories_goal INTEGER DEFAULT 0,
                  protein_goal REAL DEFAULT 0,
                  carbs_goal REAL DEFAULT 0,
                  fats_goal REAL DEFAULT 0,
                  UNIQUE(user_id, date),
                  FOREIGN KEY(user_id) REFERENCES users(id)
                );
            """);
            // workout logs
            st.execute("""
    CREATE TABLE IF NOT EXISTS workout_logs (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL,
      date TEXT NOT NULL,
      workout_type TEXT,
      exercise TEXT,
      sets INTEGER,
      reps INTEGER,
      notes TEXT,
      FOREIGN KEY(user_id) REFERENCES users(id)
    );
""");


        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
