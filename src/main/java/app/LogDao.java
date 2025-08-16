package app;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogDao {

    // Add a new log
    public static Long addLog(long userId, Map<String, Object> logData) throws SQLException {
        String date = (String) logData.get("date");
        Integer steps = asInteger(logData.get("steps"));
        Integer calories = asInteger(logData.get("calories"));
        Double protein = asDouble(logData.get("protein"));
        Double carbohydrates = asDouble(logData.get("carbohydrates"));
        Double fats = asDouble(logData.get("fats"));
        String workoutType = (String) logData.get("workout_type");
        String notes = (String) logData.get("notes");

        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO daily_logs(user_id, date, steps, calories, protein, carbohydrates, fats, workout_type, notes) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, userId);
            ps.setString(2, date);
            ps.setObject(3, steps);
            ps.setObject(4, calories);
            ps.setObject(5, protein);
            ps.setObject(6, carbohydrates);
            ps.setObject(7, fats);
            ps.setString(8, workoutType);
            ps.setString(9, notes);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    // Get all logs for a user
    public static List<Map<String, Object>> getLogs(long userId) throws SQLException {
        List<Map<String, Object>> logs = new ArrayList<>();
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM daily_logs WHERE user_id = ? ORDER BY date DESC")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(Map.of(
                            "id", rs.getLong("id"),
                            "date", rs.getString("date"),
                            "steps", rs.getInt("steps"),
                            "calories", rs.getInt("calories"),
                            "protein", rs.getDouble("protein"),
                            "carbohydrates", rs.getDouble("carbohydrates"),
                            "fats", rs.getDouble("fats"),
                            "workout_type", rs.getString("workout_type"),
                            "notes", rs.getString("notes")
                    ));
                }
            }
        }
        return logs;
    }

    // Sum totals for a given date (for progress tracking)
    public static Map<String, Object> sumForDate(long userId, String date) throws SQLException {
        String sql = """
            SELECT
              COALESCE(SUM(steps),0) as steps,
              COALESCE(SUM(calories),0) as calories,
              COALESCE(SUM(protein),0) as protein,
              COALESCE(SUM(carbohydrates),0) as carbohydrates,
              COALESCE(SUM(fats),0) as fats
            FROM daily_logs
            WHERE user_id=? AND date=?
        """;
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return Map.of(
                        "steps", rs.getInt("steps"),
                        "calories", rs.getInt("calories"),
                        "protein", rs.getDouble("protein"),
                        "carbohydrates", rs.getDouble("carbohydrates"),
                        "fats", rs.getDouble("fats")
                );
            }
        }
    }

    //  Helper methods for safe casting
    private static Integer asInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        return Integer.valueOf(o.toString());
    }

    private static Double asDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        return Double.valueOf(o.toString());
    }
    // Get all logs for a specific date
    public static List<Map<String, Object>> getLogsForDate(long userId, String date) throws SQLException {
        List<Map<String, Object>> logs = new ArrayList<>();
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM daily_logs WHERE user_id = ? AND date = ? ORDER BY id ASC")) {
            ps.setLong(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(Map.of(
                            "id", rs.getLong("id"),
                            "date", rs.getString("date"),
                            "steps", rs.getInt("steps"),
                            "calories", rs.getInt("calories"),
                            "protein", rs.getDouble("protein"),
                            "carbohydrates", rs.getDouble("carbohydrates"),
                            "fats", rs.getDouble("fats"),
                            "workout_type", rs.getString("workout_type"),
                            "notes", rs.getString("notes")
                    ));
                }
            }
        }
        return logs;
    }

}
