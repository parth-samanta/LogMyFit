package app;

import java.sql.*;

public class GoalDao {

    //Insert or update goals
    public static void upsert(long userId, String date,
                              Integer steps, Integer calories,
                              Double protein, Double carbs, Double fats) throws SQLException {
        String sql = """
            INSERT INTO daily_goals(user_id, date, steps_goal, calories_goal, protein_goal, carbs_goal, fats_goal)
            VALUES(?,?,?,?,?,?,?)
            ON CONFLICT(user_id, date) DO UPDATE SET
              steps_goal=excluded.steps_goal,
              calories_goal=excluded.calories_goal,
              protein_goal=excluded.protein_goal,
              carbs_goal=excluded.carbs_goal,
              fats_goal=excluded.fats_goal
        """;
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, date);  // Always use the date provided or the current date
            ps.setObject(3, steps);
            ps.setObject(4, calories);
            ps.setObject(5, protein);
            ps.setObject(6, carbs);
            ps.setObject(7, fats);
            ps.executeUpdate();
        }
    }

    // Fetch goals
    public static Goals get(long userId, String date) throws SQLException {
        String sql = "SELECT * FROM daily_goals WHERE user_id=? AND date=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, date);  // Fetch goals for the specific date
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;  // No goals found for the user and date
                Goals g = new Goals();
                g.userId   = userId;
                g.date     = date;
                g.steps    = (Integer) rs.getObject("steps_goal");
                g.calories = (Integer) rs.getObject("calories_goal");
                g.protein  = (Double)  rs.getObject("protein_goal");
                g.carbs    = (Double)  rs.getObject("carbs_goal");
                g.fats     = (Double)  rs.getObject("fats_goal");
                return g;
            }
        }
    }

    //simple container to hold goal details
    public static class Goals {
        public long userId;
        public String date;
        public Integer steps, calories;
        public Double protein, carbs, fats;
    }
}
