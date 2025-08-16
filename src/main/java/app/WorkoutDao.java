package app;

import java.sql.*;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class WorkoutDao {

    // Insert a new workout log
    public static Long addWorkoutLog(long userId, Map<String, Object> logData) throws SQLException {
        String sql = """
            INSERT INTO workout_logs(user_id, date, workout_type, exercise, sets, reps, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, (String) logData.get("date"));
            ps.setString(3, (String) logData.get("workout_type"));
            ps.setString(4, (String) logData.get("exercise"));

            // Handle potential null or invalid numbers
            Object setsObj = logData.get("sets");
            Object repsObj = logData.get("reps");

            if (setsObj == null || repsObj == null) {
                throw new SQLException("Sets and reps are required");
            }

            int sets, reps;
            try {
                sets = setsObj instanceof Number ? ((Number) setsObj).intValue() : Integer.parseInt(setsObj.toString());
                reps = repsObj instanceof Number ? ((Number) repsObj).intValue() : Integer.parseInt(repsObj.toString());
            } catch (NumberFormatException e) {
                throw new SQLException("Invalid sets or reps value");
            }

            if (sets <= 0 || reps <= 0) {
                throw new SQLException("Sets and reps must be positive numbers");
            }

            ps.setInt(5, sets);
            ps.setInt(6, reps);
            ps.setString(7, (String) logData.get("notes"));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    // Get all workout logs for a user
    public static List<Map<String, Object>> getWorkoutLogs(long userId) throws SQLException {
        List<Map<String, Object>> logs = new ArrayList<>();
        String sql = "SELECT * FROM workout_logs WHERE user_id = ? ORDER BY date DESC";

        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> log = new java.util.HashMap<>();
                    log.put("id", rs.getLong("id"));
                    log.put("date", rs.getString("date"));
                    log.put("workout_type", rs.getString("workout_type"));
                    log.put("exercise", rs.getString("exercise"));
                    log.put("sets", rs.getInt("sets"));
                    log.put("reps", rs.getInt("reps"));
                    log.put("notes", rs.getString("notes"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }

    // Get workout logs
    public static List<Map<String, Object>> getWorkoutLogsForDate(long userId, String date) throws SQLException {
        List<Map<String, Object>> logs = new ArrayList<>();
        String sql = "SELECT * FROM workout_logs WHERE user_id = ? AND date = ? ORDER BY id ASC";

        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, date);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> log = new java.util.HashMap<>();
                    log.put("id", rs.getLong("id"));
                    log.put("date", rs.getString("date"));
                    log.put("workout_type", rs.getString("workout_type"));
                    log.put("exercise", rs.getString("exercise"));
                    log.put("sets", rs.getInt("sets"));
                    log.put("reps", rs.getInt("reps"));
                    log.put("notes", rs.getString("notes"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }

    // Count workouts by type
    public static Map<String, Integer> getWorkoutTypeCounts(long userId) throws SQLException {
        Map<String, Integer> counts = new java.util.HashMap<>();
        String sql = "SELECT workout_type, COUNT(*) as count FROM workout_logs WHERE user_id = ? AND workout_type IS NOT NULL GROUP BY workout_type";

        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String workoutType = rs.getString("workout_type");
                    int count = rs.getInt("count");
                    counts.put(workoutType, count);
                }
            }
        }
        return counts;
    }
}