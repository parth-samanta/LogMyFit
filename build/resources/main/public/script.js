const API_BASE = window.location.origin + '/api';
let currentUser = null;

// Chart instances
let progressChart = null;
let macroChart = null;
let weeklyStepsChart = null;
let weeklyCaloriesChart = null;
let weeklyMacroChart = null;
let workoutFrequencyChart = null;

// Tab switching functions
function showTab(tabName) {
    document.querySelectorAll('#auth-section .tab-content').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('#auth-section .tab-btn').forEach(btn => btn.classList.remove('active'));

    document.getElementById(tabName + '-tab').classList.add('active');
    event.target.classList.add('active');
}

function showAppTab(tabName) {
    document.querySelectorAll('#app-section .tab-content').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('#app-section .tab-btn').forEach(btn => btn.classList.remove('active'));

    document.getElementById(tabName + '-tab').classList.add('active');
    event.target.classList.add('active');

    // Load analytics
    if (tabName === 'analytics') {
        setTimeout(() => {
            loadWeeklyAnalytics();
            loadWorkoutAnalytics();
        }, 100);
    }
}

function showHistoryTab(tabName) {
    document.getElementById('activity-history').classList.toggle('hidden', tabName !== 'activity');
    document.getElementById('workout-history').classList.toggle('hidden', tabName !== 'workouts');

    // Update active button in history section
    document.querySelectorAll('#history-tab .tab-btn').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
}

// Message display
function showMessage(message, type = 'success') {
    const messagesDiv = document.getElementById('messages');
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    messagesDiv.appendChild(alertDiv);

    // Auto-remove after delay
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.remove();
        }
    }, type === 'error' ? 8000 : 5000);

    // Allow manual dismissal
    alertDiv.addEventListener('click', () => alertDiv.remove());
    alertDiv.style.cursor = 'pointer';
    alertDiv.title = 'Click to dismiss';
}

async function apiCall(endpoint, options = {}) {
    try {
        console.log(`API Call: ${endpoint}`, options);

        const response = await fetch(`${API_BASE}${endpoint}`, {
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        });

        console.log(`API Response: ${response.status} ${response.statusText}`);

        if (!response.ok) {
            let errorMessage = 'Request failed';
            let errorData = null;

            try {
                errorData = await response.json();
                errorMessage = errorData.error || errorMessage;
            } catch (e) {
                errorMessage = `HTTP ${response.status}: ${response.statusText}`;
            }

            //Handle specific error cases
            if (response.status === 401) {
                console.log('Unauthorized - redirecting to login');
                handleUnauthorized();
                throw new Error('Please login to continue');
            }

            throw new Error(errorMessage);
        }

        const data = await response.json();
        console.log('API Response data:', data);
        return data;
    } catch (error) {
        console.error('API Error:', error);

        if (!endpoint.includes('/progress') || options.showError !== false) {
            showMessage(error.message, 'error');
        }

        throw error;
    }
}

// Handle unauthorized access
function handleUnauthorized() {
    currentUser = null;
    document.getElementById('auth-section').classList.remove('hidden');
    document.getElementById('app-section').classList.add('hidden');

    // Clear sensitive data
    clearAppData();
}

// Clear app data on logout
function clearAppData() {
    // Clear all charts
    if (progressChart) { progressChart.destroy(); progressChart = null; }
    if (macroChart) { macroChart.destroy(); macroChart = null; }
    if (weeklyStepsChart) { weeklyStepsChart.destroy(); weeklyStepsChart = null; }
    if (weeklyCaloriesChart) { weeklyCaloriesChart.destroy(); weeklyCaloriesChart = null; }
    if (weeklyMacroChart) { weeklyMacroChart.destroy(); weeklyMacroChart = null; }
    if (workoutFrequencyChart) { workoutFrequencyChart.destroy(); workoutFrequencyChart = null; }

    // Clear forms
    document.querySelectorAll('form').forEach(form => form.reset());

    // Reset displays
    document.getElementById('progress-display').innerHTML = '<p>Loading your progress...</p>';
    document.getElementById('activity-logs').innerHTML = '';
    document.getElementById('workout-logs').innerHTML = '';
    document.getElementById('current-user').textContent = '';
}

// Chart creation utilities
function createProgressChart(progress) {
    const ctx = document.getElementById('progress-chart');
    if (!ctx) return;

    if (progressChart) {
        progressChart.destroy();
    }

    const { sum, goals } = progress;

    if (!goals || (!goals.steps && !goals.calories && !goals.protein && !goals.carbs && !goals.fats)) {
        const context = ctx.getContext('2d');
        context.clearRect(0, 0, ctx.width, ctx.height);
        context.font = '16px Arial';
        context.fillStyle = '#666';
        context.textAlign = 'center';
        context.fillText('Set goals to see progress chart', ctx.width/2, ctx.height/2);
        return;
    }

    const data = {
        labels: ['Steps', 'Calories', 'Protein (g)', 'Carbs (g)', 'Fats (g)'],
        datasets: [
            {
                label: 'Current',
                data: [sum.steps, sum.calories, sum.protein, sum.carbohydrates, sum.fats],
                backgroundColor: 'rgba(102, 126, 234, 0.6)',
                borderColor: 'rgba(102, 126, 234, 1)',
                borderWidth: 2
            },
            {
                label: 'Goal',
                data: [goals.steps || 0, goals.calories || 0, goals.protein || 0, goals.carbs || 0, goals.fats || 0],
                backgroundColor: 'rgba(118, 75, 162, 0.6)',
                borderColor: 'rgba(118, 75, 162, 1)',
                borderWidth: 2
            }
        ]
    };

    progressChart = new Chart(ctx, {
        type: 'bar',
        data: data,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: 'Current vs Goals'
                },
                legend: {
                    position: 'top'
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function createMacroChart(sum) {
    const ctx = document.getElementById('macro-chart');
    if (!ctx) return;

    if (macroChart) {
        macroChart.destroy();
    }

    const totalMacros = sum.protein + sum.carbohydrates + sum.fats;
    if (totalMacros === 0) {
        const context = ctx.getContext('2d');
        context.clearRect(0, 0, ctx.width, ctx.height);
        context.font = '16px Arial';
        context.fillStyle = '#666';
        context.textAlign = 'center';
        context.fillText('Log some food to see macro breakdown', ctx.width/2, ctx.height/2);
        return;
    }

    macroChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Protein', 'Carbohydrates', 'Fats'],
            datasets: [{
                data: [sum.protein, sum.carbohydrates, sum.fats],
                backgroundColor: [
                    'rgba(255, 99, 132, 0.8)',
                    'rgba(54, 162, 235, 0.8)',
                    'rgba(255, 205, 86, 0.8)'
                ],
                borderColor: [
                    'rgba(255, 99, 132, 1)',
                    'rgba(54, 162, 235, 1)',
                    'rgba(255, 205, 86, 1)'
                ],
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: 'Today\'s Macros'
                },
                legend: {
                    position: 'bottom'
                }
            }
        }
    });
}

// Authentication functions
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;

    if (!username || !password) {
        showMessage('Please enter both username and password', 'error');
        return;
    }

    try {
        const response = await apiCall('/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });

        currentUser = username;
        document.getElementById('current-user').textContent = `Welcome, ${username}!`;
        document.getElementById('auth-section').classList.add('hidden');
        document.getElementById('app-section').classList.remove('hidden');

        //Clear login form
        document.getElementById('login-form').reset();

        //Load initial data
        await loadProgress();
        showMessage('Login successful!');
    } catch (error) {
        console.error('Login failed:', error);
    }
});

document.getElementById('signup-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('signup-username').value.trim();
    const password = document.getElementById('signup-password').value;
    const email = document.getElementById('signup-email').value.trim();

    if (!username || !password) {
        showMessage('Please enter both username and password', 'error');
        return;
    }

    if (username.length < 3) {
        showMessage('Username must be at least 3 characters long', 'error');
        return;
    }

    if (password.length < 6) {
        showMessage('Password must be at least 6 characters long', 'error');
        return;
    }

    try {
        await apiCall('/signup', {
            method: 'POST',
            body: JSON.stringify({ username, password, email: email || null })
        });

        showMessage('Account created successfully! Please login.');
        showTab('login');

        //Clear form
        document.getElementById('signup-form').reset();

        // Pre fill login form
        document.getElementById('login-username').value = username;
        document.getElementById('login-password').focus();
    } catch (error) {
        console.error('Signup failed:', error);
    }
});

//Logout function
async function logout() {
    try {
        await apiCall('/logout', { method: 'POST' });

        handleUnauthorized();
        showMessage('Logged out successfully!');
    } catch (error) {
        console.error('Logout error:', error);
        // Even if logout API fails, still clear local state
        handleUnauthorized();
    }
}

// Load progress function
async function loadProgress() {
    try {
        console.log('Loading progress...');
        const progress = await apiCall('/progress');
        const progressDiv = document.getElementById('progress-display');

        const { sum, goals } = progress;

        let html = `<h3>Today (${progress.date})</h3>`;

        if (goals && (goals.steps || goals.calories || goals.protein || goals.carbs || goals.fats)) {
            html += `
                <div class="progress-item">
                    <span>Steps:</span>
                    <span>${sum.steps} / ${goals.steps || 0} (${Math.max(0, (goals.steps || 0) - sum.steps)} left)</span>
                </div>
                <div class="progress-item">
                    <span>Calories:</span>
                    <span>${sum.calories} / ${goals.calories || 0} (${Math.max(0, (goals.calories || 0) - sum.calories)} left)</span>
                </div>
                <div class="progress-item">
                    <span>Protein:</span>
                    <span>${sum.protein}g / ${goals.protein || 0}g (${Math.max(0, (goals.protein || 0) - sum.protein).toFixed(1)}g left)</span>
                </div>
                <div class="progress-item">
                    <span>Carbs:</span>
                    <span>${sum.carbohydrates}g / ${goals.carbs || 0}g (${Math.max(0, (goals.carbs || 0) - sum.carbohydrates).toFixed(1)}g left)</span>
                </div>
                <div class="progress-item">
                    <span>Fats:</span>
                    <span>${sum.fats}g / ${goals.fats || 0}g (${Math.max(0, (goals.fats || 0) - sum.fats).toFixed(1)}g left)</span>
                </div>
            `;
        } else {
            html += `
                <div class="progress-item">
                    <span>Steps:</span>
                    <span>${sum.steps}</span>
                </div>
                <div class="progress-item">
                    <span>Calories:</span>
                    <span>${sum.calories}</span>
                </div>
                <div class="progress-item">
                    <span>Protein:</span>
                    <span>${sum.protein}g</span>
                </div>
                <div class="progress-item">
                    <span>Carbs:</span>
                    <span>${sum.carbohydrates}g</span>
                </div>
                <div class="progress-item">
                    <span>Fats:</span>
                    <span>${sum.fats}g</span>
                </div>
                <p style="margin-top: 15px; font-style: italic; color: #6c757d;">
                    <em>Set your goals to track progress!</em>
                </p>
            `;
        }

        progressDiv.innerHTML = html;

        //Create charts with delay to ensure DOM is ready
        setTimeout(() => {
            createProgressChart(progress);
            createMacroChart(sum);
        }, 100);

    } catch (error) {
        console.error('Failed to load progress:', error);
        document.getElementById('progress-display').innerHTML = '<p style="color: red;">Failed to load progress. Please try refreshing or logging in again.</p>';
    }
}

// Log activity
document.getElementById('activity-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const logData = {
        steps: parseInt(document.getElementById('activity-steps').value) || 0,
        calories: parseInt(document.getElementById('activity-calories').value) || 0,
        protein: parseFloat(document.getElementById('activity-protein').value) || 0,
        carbohydrates: parseFloat(document.getElementById('activity-carbs').value) || 0,
        fats: parseFloat(document.getElementById('activity-fats').value) || 0,
        workout_type: document.getElementById('activity-workout-type').value,
        notes: document.getElementById('activity-notes').value
    };

    try {
        await apiCall('/log', {
            method: 'POST',
            body: JSON.stringify(logData)
        });

        showMessage('Activity logged successfully!');
        document.getElementById('activity-form').reset();
        await loadProgress(); // Refresh progress
    } catch (error) {
        console.error('Failed to log activity:', error);
    }
});

// Log workout
document.getElementById('workout-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const workoutData = {
        workout_type: document.getElementById('workout-type').value,
        exercise: document.getElementById('workout-exercise').value,
        sets: parseInt(document.getElementById('workout-sets').value),
        reps: parseInt(document.getElementById('workout-reps').value),
        notes: document.getElementById('workout-notes').value
    };

    if (!workoutData.workout_type || !workoutData.exercise || !workoutData.sets || !workoutData.reps) {
        showMessage('Please fill in all required fields', 'error');
        return;
    }

    try {
        await apiCall('/workout-log', {
            method: 'POST',
            body: JSON.stringify(workoutData)
        });

        showMessage('Workout logged successfully!');
        document.getElementById('workout-form').reset();
    } catch (error) {
        console.error('Failed to log workout:', error);
    }
});

// Set goals
document.getElementById('goals-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const goalsData = {
        steps_goal: parseInt(document.getElementById('goals-steps').value) || null,
        calories_goal: parseInt(document.getElementById('goals-calories').value) || null,
        protein_goal: parseFloat(document.getElementById('goals-protein').value) || null,
        carbs_goal: parseFloat(document.getElementById('goals-carbs').value) || null,
        fats_goal: parseFloat(document.getElementById('goals-fats').value) || null
    };

    // Check if at least one goal is set
    const hasGoals = Object.values(goalsData).some(val => val !== null && val > 0);
    if (!hasGoals) {
        showMessage('Please set at least one goal', 'error');
        return;
    }

    try {
        await apiCall('/goals', {
            method: 'POST',
            body: JSON.stringify(goalsData)
        });

        showMessage('Goals saved successfully!');
        await loadProgress();
    } catch (error) {
        console.error('Failed to save goals:', error);
    }
});

//Load activity logs
async function loadActivityLogs() {
    const logsDiv = document.getElementById('activity-logs');

    try {
        logsDiv.innerHTML = '<p>Loading activity logs...</p>';
        const logs = await apiCall('/logs');

        if (logs.length === 0) {
            logsDiv.innerHTML = '<p>No activity logs found. Start logging your activities!</p>';
            return;
        }

        logsDiv.innerHTML = logs.map(log => `
            <div class="log-item">
                <div class="log-date">${log.date}</div>
                <p><strong>Steps:</strong> ${log.steps} | <strong>Calories:</strong> ${log.calories}</p>
                <p><strong>Macros:</strong> P: ${log.protein}g, C: ${log.carbohydrates}g, F: ${log.fats}g</p>
                ${log.workout_type ? `<p><strong>Activity:</strong> ${log.workout_type}</p>` : ''}
                ${log.notes ? `<p><strong>Notes:</strong> ${log.notes}</p>` : ''}
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load activity logs:', error);
        logsDiv.innerHTML = '<p style="color: red;">Failed to load activity logs. Please try again.</p>';
    }
}

// Load workout logs
async function loadWorkoutLogs() {
    const logsDiv = document.getElementById('workout-logs');

    try {
        logsDiv.innerHTML = '<p>Loading workout logs...</p>';
        const logs = await apiCall('/workout-logs');

        if (logs.length === 0) {
            logsDiv.innerHTML = '<p>No workout logs found. Start logging your workouts!</p>';
            return;
        }

        logsDiv.innerHTML = logs.map(log => `
            <div class="log-item">
                <div class="log-date">${log.date}</div>
                <p><strong>Workout:</strong> ${log.workout_type}</p>
                <p><strong>Exercise:</strong> ${log.exercise}</p>
                <p><strong>Sets/Reps:</strong> ${log.sets} x ${log.reps}</p>
                ${log.notes ? `<p><strong>Notes:</strong> ${log.notes}</p>` : ''}
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load workout logs:', error);
        logsDiv.innerHTML = '<p style="color: red;">Failed to load workout logs. Please try again.</p>';
    }
}

//Analytics functions
async function loadWeeklyAnalytics() {
    try {
        console.log('Loading weekly analytics...');
        const logs = await apiCall('/logs');

        if (logs.length === 0) {
            showMessage('No data available for analytics. Start logging activities!', 'error');
            clearAnalyticsCharts();
            return;
        }

        const last7Days = logs.slice(0, 7).reverse();
        createWeeklyStepsChart(last7Days);
        createWeeklyCaloriesChart(last7Days);
        createWeeklyMacroChart(last7Days);

    } catch (error) {
        console.error('Failed to load weekly analytics:', error);
        clearAnalyticsCharts();
    }
}

function clearAnalyticsCharts() {
    const charts = ['weekly-steps-chart', 'weekly-calories-chart', 'weekly-macro-chart'];
    charts.forEach(chartId => {
        const ctx = document.getElementById(chartId);
        if (ctx) {
            const context = ctx.getContext('2d');
            context.clearRect(0, 0, ctx.width, ctx.height);
            context.font = '16px Arial';
            context.fillStyle = '#666';
            context.textAlign = 'center';
            context.fillText('No data available', ctx.width/2, ctx.height/2);
        }
    });
}

function createWeeklyStepsChart(logs) {
    const ctx = document.getElementById('weekly-steps-chart');
    if (!ctx) return;

    if (weeklyStepsChart) {
        weeklyStepsChart.destroy();
    }

    if (logs.length === 0) {
        clearChart(ctx, 'No steps data available');
        return;
    }

    const dates = logs.map(log => new Date(log.date).toLocaleDateString());
    const steps = logs.map(log => log.steps);

    weeklyStepsChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: dates,
            datasets: [{
                label: 'Daily Steps',
                data: steps,
                borderColor: 'rgba(102, 126, 234, 1)',
                backgroundColor: 'rgba(102, 126, 234, 0.1)',
                tension: 0.4,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function createWeeklyCaloriesChart(logs) {
    const ctx = document.getElementById('weekly-calories-chart');
    if (!ctx) return;

    if (weeklyCaloriesChart) {
        weeklyCaloriesChart.destroy();
    }

    if (logs.length === 0) {
        clearChart(ctx, 'No calorie data available');
        return;
    }

    const dates = logs.map(log => new Date(log.date).toLocaleDateString());
    const calories = logs.map(log => log.calories);

    weeklyCaloriesChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: dates,
            datasets: [{
                label: 'Daily Calories',
                data: calories,
                backgroundColor: 'rgba(118, 75, 162, 0.8)',
                borderColor: 'rgba(118, 75, 162, 1)',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function createWeeklyMacroChart(logs) {
    const ctx = document.getElementById('weekly-macro-chart');
    if (!ctx) return;

    if (weeklyMacroChart) {
        weeklyMacroChart.destroy();
    }

    if (logs.length === 0) {
        clearChart(ctx, 'No macro data available');
        return;
    }

    const dates = logs.map(log => new Date(log.date).toLocaleDateString());
    const protein = logs.map(log => log.protein);
    const carbs = logs.map(log => log.carbohydrates);
    const fats = logs.map(log => log.fats);

    weeklyMacroChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: dates,
            datasets: [
                {
                    label: 'Protein',
                    data: protein,
                    borderColor: 'rgba(255, 99, 132, 1)',
                    backgroundColor: 'rgba(255, 99, 132, 0.1)',
                    tension: 0.4
                },
                {
                    label: 'Carbs',
                    data: carbs,
                    borderColor: 'rgba(54, 162, 235, 1)',
                    backgroundColor: 'rgba(54, 162, 235, 0.1)',
                    tension: 0.4
                },
                {
                    label: 'Fats',
                    data: fats,
                    borderColor: 'rgba(255, 205, 86, 1)',
                    backgroundColor: 'rgba(255, 205, 86, 0.1)',
                    tension: 0.4
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top'
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

async function loadWorkoutAnalytics() {
    try {
        console.log('Loading workout analytics...');
        const logs = await apiCall('/workout-logs');
        createWorkoutFrequencyChart(logs);
    } catch (error) {
        console.error('Failed to load workout analytics:', error);
        const ctx = document.getElementById('workout-frequency-chart');
        if (ctx) {
            clearChart(ctx, 'Failed to load workout data');
        }
    }
}

function createWorkoutFrequencyChart(logs) {
    const ctx = document.getElementById('workout-frequency-chart');
    if (!ctx) return;

    if (workoutFrequencyChart) {
        workoutFrequencyChart.destroy();
    }

    if (logs.length === 0) {
        clearChart(ctx, 'No workout data available');
        return;
    }

    //Count workout types
    const workoutCounts = {};
    logs.forEach(log => {
        if (log.workout_type) {
            workoutCounts[log.workout_type] = (workoutCounts[log.workout_type] || 0) + 1;
        }
    });

    const labels = Object.keys(workoutCounts);
    const data = Object.values(workoutCounts);

    workoutFrequencyChart = new Chart(ctx, {
        type: 'pie',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: [
                    'rgba(255, 99, 132, 0.8)',
                    'rgba(54, 162, 235, 0.8)',
                    'rgba(255, 205, 86, 0.8)',
                    'rgba(75, 192, 192, 0.8)',
                    'rgba(153, 102, 255, 0.8)',
                    'rgba(255, 159, 64, 0.8)'
                ],
                borderColor: [
                    'rgba(255, 99, 132, 1)',
                    'rgba(54, 162, 235, 1)',
                    'rgba(255, 205, 86, 1)',
                    'rgba(75, 192, 192, 1)',
                    'rgba(153, 102, 255, 1)',
                    'rgba(255, 159, 64, 1)'
                ],
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'right'
                }
            }
        }
    });
}

// Helper function to clear chart canvas
function clearChart(ctx, message) {
    const context = ctx.getContext('2d');
    context.clearRect(0, 0, ctx.width, ctx.height);
    context.font = '16px Arial';
    context.fillStyle = '#666';
    context.textAlign = 'center';
    context.fillText(message, ctx.width/2, ctx.height/2);
}

// Initialize app
document.addEventListener('DOMContentLoaded', async () => {
    console.log('App initializing...');

    // Try to check if user is already logged in
    try {
        // Use a silent progress check to see if we're authenticated
        await apiCall('/progress', { showError: false });

        // If successful, user is logged in
        console.log('User already authenticated');
        document.getElementById('auth-section').classList.add('hidden');
        document.getElementById('app-section').classList.remove('hidden');
        document.getElementById('current-user').textContent = 'Welcome back!';

        // Load progress normally now
        await loadProgress();

    } catch (error) {
        // If failed, user needs to login
        console.log('User not authenticated, showing login');
        document.getElementById('auth-section').classList.remove('hidden');
        document.getElementById('app-section').classList.add('hidden');
    }
});