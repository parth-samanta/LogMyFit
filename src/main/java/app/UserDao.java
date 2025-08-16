package app;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class UserDao {

    public static Long createUser(String username, String rawPassword, String email) throws SQLException {
        if (username == null || rawPassword == null) {
            throw new IllegalArgumentException("username and password required");
        }
        username = username.trim();
        if (username.isEmpty()) {
            throw new IllegalArgumentException("username and password required");
        }
        if (email != null) {
            email = email.trim();
            if (email.isEmpty()) email = null;
        }

        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users(username, password_hash, email) VALUES(?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, email);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("unique") && msg.contains("users.username")) {
                throw new SQLException("USERNAME_TAKEN");
            }
            throw e;
        }
    }

    public static boolean authenticate(String username, String rawPassword) throws SQLException {
        if (username == null || rawPassword == null) return false;
        username = username.trim();
        if (username.isEmpty()) return false;

        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT password_hash FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String storedHash = rs.getString("password_hash");
                return storedHash != null && BCrypt.checkpw(rawPassword, storedHash);
            }
        }
    }

    public static Long getUserIdByUsername(String username) throws SQLException {
        if (username == null) return null;
        username = username.trim();
        if (username.isEmpty()) return null;

        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }
}
