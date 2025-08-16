package app;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            // Enable logging (development mode)
            config.plugins.enableDevLogging();

            // Enable CORS (allow all origins for development)
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.allowHost("http://localhost:63342");
                    it.allowHost("http://localhost:7070");
                    it.anyHost(); // Allow any host for development
                });
            });

        }).start(7070);

        // Serve static files manually
        app.get("/", ctx -> {
            var inputStream = Main.class.getClassLoader().getResourceAsStream("public/index.html");
            if (inputStream != null) {
                ctx.contentType("text/html");
                ctx.result(inputStream);
            } else {
                ctx.html("<h1>Welcome to FitGirl Tracker</h1><p>Frontend files not found. Please check your build setup.</p>");
            }
        });

        // Serve JavaScript and CSS files
        app.get("/script.js", ctx -> {
            var inputStream = Main.class.getClassLoader().getResourceAsStream("public/script.js");
            if (inputStream != null) {
                ctx.contentType("application/javascript");
                ctx.result(inputStream);
            } else {
                ctx.status(404).result("script.js not found");
            }
        });

        app.get("/styles.css", ctx -> {
            var inputStream = Main.class.getClassLoader().getResourceAsStream("public/styles.css");
            if (inputStream != null) {
                ctx.contentType("text/css");
                ctx.result(inputStream);
            } else {
                ctx.status(404).result("styles.css not found");
            }
        });

        app.get("/./script.js", ctx -> ctx.redirect("/script.js"));
        app.get("/./styles.css", ctx -> ctx.redirect("/styles.css"));

        // Health check
        app.get("/api/health", ctx -> ctx.json(Map.of("status", "ok")));

        // Enhanced authentication helper
        app.before("/api/*", ctx -> {
            // Skip auth for login, signup, and health endpoints
            String path = ctx.path();
            if (path.equals("/api/login") || path.equals("/api/signup") || path.equals("/api/health")) {
                return;
            }

            // Debug: Print session info
            System.out.println("Session ID: " + ctx.req().getSession(false));
            System.out.println("User in session: " + ctx.sessionAttribute("user"));

            String username = ctx.sessionAttribute("user");
            if (username == null || username.trim().isEmpty()) {
                System.out.println("Unauthorized access attempt to: " + path);
                ctx.status(401).json(Map.of("error", "Unauthorized"));
                return;
            }

            // Verify user exists in database
            try {
                Long userId = UserDao.getUserIdByUsername(username);
                if (userId == null) {
                    System.out.println("User not found in database: " + username);
                    ctx.status(401).json(Map.of("error", "Unauthorized"));
                    return;
                }
                // Store userId for use in handlers
                ctx.attribute("userId", userId);
                ctx.attribute("username", username);
            } catch (Exception e) {
                System.out.println("Database error during auth: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).json(Map.of("error", "Database error"));
                return;
            }
        });

        // User signup
        app.post("/api/signup", ctx -> {
            try {
                var body = ctx.bodyAsClass(Map.class);
                String username = (String) body.get("username");
                String password = (String) body.get("password");
                String email = (String) body.get("email");

                if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                    ctx.status(400).json(Map.of("error", "username and password required"));
                    return;
                }

                Long id = UserDao.createUser(username, password, email);
                System.out.println("User created: " + username + " with ID: " + id);
                ctx.json(Map.of("message", "User created", "userId", id));
            } catch (Exception e) {
                System.out.println("Signup error: " + e.getMessage());
                e.printStackTrace();
                if (e.getMessage().contains("USERNAME_TAKEN")) {
                    ctx.status(409).json(Map.of("error", "Username already taken"));
                } else {
                    ctx.status(500).json(Map.of("error", "Failed to create user: " + e.getMessage()));
                }
            }
        });

        // User login
        app.post("/api/login", ctx -> {
            try {
                var body = ctx.bodyAsClass(Map.class);
                String username = (String) body.get("username");
                String password = (String) body.get("password");

                if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                    ctx.status(400).json(Map.of("error", "username and password required"));
                    return;
                }

                if (UserDao.authenticate(username, password)) {
                    // Create session
                    ctx.req().getSession(true); // Force create session
                    ctx.sessionAttribute("user", username.trim());
                    System.out.println("User logged in: " + username + ", Session: " + ctx.req().getSession().getId());
                    ctx.json(Map.of("message", "Login successful", "user", username.trim()));
                } else {
                    System.out.println("Failed login attempt: " + username);
                    ctx.status(401).json(Map.of("error", "Invalid credentials"));
                }
            } catch (Exception e) {
                System.out.println("Login error: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).json(Map.of("error", "Login failed: " + e.getMessage()));
            }
        });

        // User logout
        app.post("/api/logout", ctx -> {
            try {
                String username = ctx.attribute("username");
                System.out.println("User logging out: " + username);
                ctx.req().getSession().invalidate();
                ctx.json(Map.of("message", "Logged out"));
            } catch (Exception e) {
                System.out.println("Logout error: " + e.getMessage());
                ctx.json(Map.of("message", "Logged out")); // Still return success
            }
        });

        //  Daily Logs

        // Add a daily log
        app.post("/api/log", ctx -> {
            try {
                Long userId = ctx.attribute("userId");
                String username = ctx.attribute("username");

                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String date = pickDate(ctx, body);
                body.put("date", date);

                System.out.println("Adding log for user: " + username + " (ID: " + userId + ") on date: " + date);

                Long logId = LogDao.addLog(userId, body);
                ctx.json(Map.of("message", "log-saved", "logId", logId, "date", date));
            } catch (Exception e) {
                System.out.println("Log creation error: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).json(Map.of("error", "Failed to save log: " + e.getMessage()));
            }
        });

        // List my logs
        app.get("/api/logs", ctx -> {
            try {
                Long userId = ctx.attribute("userId");
                String username = ctx.attribute("username");

                System.out.println("Fetching logs for user: " + username + " (ID: " + userId + ")");

                List<Map<String, Object>> logs = LogDao.getLogs(userId);
                System.out.println("Found " + logs.size() + " logs");
                ctx.json(logs);
            } catch (Exception e) {
                System.out.println("Error fetching logs: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).json(Map.of("error", "Failed to fetch logs: " + e.getMessage()));
            }
        });

        //  Daily Goals and Progress

        // Set/update goals for a specific date
        app.post("/api/goals", ctx -> {
            try {
                Long userId = ctx.attribute("userId");
                String username = ctx.attribute("username");

                var body = ctx.bodyAsClass(Map.class);
                String date = pickDate(ctx, body);

                Integer stepsGoal    = asInteger(body.get("steps_goal"));
                Integer caloriesGoal = asInteger(body.get("calories_goal"));
                Double  proteinGoal  = asDouble(body.get("protein_goal"));
                Double  carbsGoal    = asDouble(body.get("carbs_goal"));
                Double  fatsGoal     = asDouble(body.get("fats_goal"));

                System.out.println("Setting goals for user: " + username + " on date: " + date);

                GoalDao.upsert(userId, date, stepsGoal, caloriesGoal, proteinGoal, carbsGoal, fatsGoal);
                ctx.json(Map.of("message", "goals-saved", "date", date));
            } catch (Exception e) {
                System.out.println("Goals setting error: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).json(Map.of("error", "Failed to save goals: " + e.getMessage()));
            }
        });

        // Progress for a specific date
        app.get("/api/progress", ctx -> {
            try {
                Long userId = ctx.attribute("userId");
                String username = ctx.attribute("username");

                String date = pickDate(ctx, null);
                System.out.println("Fetching progress for user: " + username + " on date: " + date);

                var sums  = LogDao.sumForDate(userId, date);
                var goals = GoalDao.get(userId, date);

                Map<String, Object> resp = new HashMap<>();
                resp.put("date", date);
                resp.put("sum", sums);
                resp.put("goals", goals);

                if (goals != null && sums != null) {
                    int stepsDone   = ((Number) sums.get("steps")).intValue();
                    int calDone     = ((Number) sums.get("calories")).intValue();
                    double protDone = ((Number) sums.get("protein")).doubleValue();
                    double carbsDone= ((Number) sums.get("carbohydrates")).doubleValue();
                    double fatsDone = ((Number) sums.get("fats")).doubleValue();

                    resp.put("leftSteps",    Math.max(0, (goals.steps    == null ? 0 : goals.steps)    - stepsDone));
                    resp.put("leftCalories", Math.max(0, (goals.calories == null ? 0 : goals.calories) - calDone));
                    resp.put("leftProtein",  Math.max(0, (goals.protein  == null ? 0.0 : goals.protein)  - protDone));
                    resp.put("leftCarbs",    Math.max(0, (goals.carbs    == null ? 0.0 : goals.carbs)    - carbsDone));
                    resp.put("leftFats",     Math.max(0, (goals.fats     == null ? 0.0 : goals.fats)     - fatsDone));
                }

                ctx.json(resp);
            } catch (Exception e) {
                System.out.println("Progress fetching error: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).json(Map.of("error", "Failed to fetch progress: " + e.getMessage()));
            }
        });

        // Workout Logs

        // Add a workout log
        app.post("/api/workout-log", ctx -> {
            try {
                Long userId = ctx.attribute("userId");
                String username = ctx.attribute("username");

                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                String date = pickDate(ctx, body);
                body.put("date", date);

                System.out.println("Adding workout log for user: " + username + " on date: " + date);

                Long workoutLogId = WorkoutDao.addWorkoutLog(userId, body);
                ctx.json(Map.of("message", "Workout log saved", "workoutLogId", workoutLogId, "date", date));
            } catch (Exception e) {
                System.out.println("Workout log error: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).json(Map.of("error", "Failed to save workout log: " + e.getMessage()));
            }
        });

        // List all workout logs for the user
        app.get("/api/workout-logs", ctx -> {
            try {
                Long userId = ctx.attribute("userId");
                String username = ctx.attribute("username");

                System.out.println("Fetching workout logs for user: " + username + " (ID: " + userId + ")");

                List<Map<String, Object>> workoutLogs = WorkoutDao.getWorkoutLogs(userId);
                System.out.println("Found " + workoutLogs.size() + " workout logs");
                ctx.json(workoutLogs);
            } catch (Exception e) {
                System.out.println("Error fetching workout logs: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).json(Map.of("error", "Failed to fetch workout logs: " + e.getMessage()));
            }
        });

    }

    // Helper methods
    private static Integer asInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double asDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Choose date
    private static String pickDate(io.javalin.http.Context ctx, Map<String, Object> body) {
        String qp = ctx.queryParam("date");
        if (qp != null && !qp.isBlank()) return qp;
        Object bd = (body == null) ? null : body.get("date");
        if (bd instanceof String s && !s.isBlank()) return s;
        return LocalDate.now().toString();
    }
}