# LogMyFit – Comprehensive Fitness Tracking Web Application

LogMyFit is a full-stack fitness tracking application built using **Java (Javalin), SQLite, JDBC, and Vanilla JavaScript + Chart.js**.  
It enables users to **log daily activities**, **track workouts**, **set fitness goals**, and **visualize progress** through interactive dashboards.

This project demonstrates **REST API design, authentication security, DAO pattern, responsive UI, and real-time data visualization.**

---

##  Features

###  User Authentication
- Registration & login with **BCrypt password hashing**
- Session-based authentication (secure cookies)
- Unauthorized request protection

###  Fitness & Activity Logging
- Track steps, calories, macros, workout types, notes
- Daily logs stored with timestamps
- Data validation (client + server side)

### Goal Management
- Steps, calorie, protein & macro targets
- `UPSERT` logic using SQLite (`ON CONFLICT`)
- Real-time progress tracking

### Workout Tracking
- Exercise name, sets, reps, notes
- Workout frequency analytics
- History tab with date-based view

###  Analytics & Visualization (Chart.js)
- Weekly calorie & step trends  
- Daily macro comparison  
- Workout frequency analytics  
- Real-time progress charts  

---

##  System Architecture (3-Tier)
┌─────────────────────────────────────┐
│ Presentation Layer (Client) │
│ HTML5 + CSS3 + JavaScript │
│ Chart.js for Visualizations │
└──────────────┬──────────────────────┘
│ HTTP/REST API
┌──────────────▼──────────────────────┐
│ Business Logic Layer (Server) │
│ Javalin Framework + Java │
│ Authentication & Validation │
│ API Endpoints & Routing │
└──────────────┬──────────────────────┘
│ JDBC
┌──────────────▼──────────────────────┐
│ Data Access Layer (Database) │
│ SQLite Database + DAO Pattern │
└─────────────────────────────────────┘
## Tech Stack

| Layer | Technologies Used |
|------|---------------------|
| Frontend | HTML5, CSS3, JavaScript (ES6+), Chart.js |
| Backend | Java 17, Javalin Framework |
| Database | SQLite + JDBC |
| Security | BCrypt (password hashing), Session Auth |
| Build Tool | Gradle |
| Version Control | Git & GitHub |

---

##  Folder Structure
├── src/main/java/... # Java backend (routes, DAO, models)
├── src/main/resources/ # Static HTML/CSS/JS files
├── data/ # SQLite database (NOT in GitHub)
├── README.md
└── build.gradle
## Security 

| Mechanism                | Implementation                              |
| ------------------------ | ------------------------------------------- |
| Password Hashing         | BCrypt (`jBCrypt`)                          |
| SQL Injection Prevention | Prepared Statements                         |
| Session Management       | `credentials: 'include'` + HttpOnly cookies |
| CORS                     | Configured in Javalin                       |
| Input Validation         | Client & server-side checks                 |


## API example(Frontend call)
async function apiCall(endpoint, options = {}) {
  const response = await fetch(`${API_BASE}${endpoint}`, {
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      ...options
  });
  if (!response.ok) throw new Error("Request failed");
  return response.json();
}



