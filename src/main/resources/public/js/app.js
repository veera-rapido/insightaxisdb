/**
 * InsightAxisDB Dashboard Application
 */
document.addEventListener('DOMContentLoaded', function() {
    // Initialize API client
    const api = new TesseractAPI();
    
    // Navigation
    const navLinks = document.querySelectorAll('.nav-link');
    const sections = document.querySelectorAll('.section-content');
    
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Update active link
            navLinks.forEach(l => l.classList.remove('active'));
            this.classList.add('active');
            
            // Show selected section
            const sectionId = this.getAttribute('data-section');
            sections.forEach(section => {
                section.classList.add('d-none');
            });
            document.getElementById(`${sectionId}-section`).classList.remove('d-none');
            
            // Load section data if needed
            if (sectionId === 'users') {
                loadUsers();
            } else if (sectionId === 'events') {
                loadEvents();
                loadEventCharts();
            } else if (sectionId === 'recommendations' || sectionId === 'predictions') {
                loadUserDropdowns();
            }
        });
    });
    
    // User Management
    
    function loadUsers() {
        const tableBody = document.querySelector('#users-table tbody');
        tableBody.innerHTML = '<tr><td colspan="6" class="text-center">Loading users...</td></tr>';
        
        api.getAllUsers()
            .then(users => {
                if (users.length === 0) {
                    tableBody.innerHTML = '<tr><td colspan="6" class="text-center">No users found</td></tr>';
                    return;
                }
                
                tableBody.innerHTML = '';
                users.forEach(user => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${user.userId}</td>
                        <td>${user.properties.name || '-'}</td>
                        <td>${user.properties.email || '-'}</td>
                        <td>${user.properties.country || '-'}</td>
                        <td>${user.eventCount}</td>
                        <td>
                            <button class="btn btn-sm btn-outline-primary btn-icon view-events" data-user-id="${user.userId}">
                                <i class="bi bi-list-ul"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-danger btn-icon delete-user" data-user-id="${user.userId}">
                                <i class="bi bi-trash"></i>
                            </button>
                        </td>
                    `;
                    tableBody.appendChild(row);
                });
                
                // Add event listeners
                document.querySelectorAll('.view-events').forEach(button => {
                    button.addEventListener('click', function() {
                        const userId = this.getAttribute('data-user-id');
                        showUserEvents(userId);
                    });
                });
                
                document.querySelectorAll('.delete-user').forEach(button => {
                    button.addEventListener('click', function() {
                        const userId = this.getAttribute('data-user-id');
                        if (confirm(`Are you sure you want to delete user ${userId}?`)) {
                            deleteUser(userId);
                        }
                    });
                });
            })
            .catch(error => {
                tableBody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">Error loading users: ${error.message}</td></tr>`;
            });
    }
    
    function showUserEvents(userId) {
        const tableBody = document.querySelector('#user-events-table tbody');
        tableBody.innerHTML = '<tr><td colspan="4" class="text-center">Loading events...</td></tr>';
        
        // Update modal title
        document.querySelector('#userEventsModal .modal-title').textContent = `Events for User: ${userId}`;
        
        // Show modal
        const modal = new bootstrap.Modal(document.getElementById('userEventsModal'));
        modal.show();
        
        // Load user events
        api.getUserEvents(userId)
            .then(events => {
                if (events.length === 0) {
                    tableBody.innerHTML = '<tr><td colspan="4" class="text-center">No events found</td></tr>';
                    return;
                }
                
                tableBody.innerHTML = '';
                events.forEach(event => {
                    const row = document.createElement('tr');
                    
                    // Format timestamp
                    const date = new Date(event.timestamp);
                    const formattedDate = date.toLocaleString();
                    
                    // Format properties
                    let propertiesHtml = '<table class="properties-table">';
                    for (const [key, value] of Object.entries(event.properties)) {
                        propertiesHtml += `<tr><td>${key}</td><td>${value}</td></tr>`;
                    }
                    propertiesHtml += '</table>';
                    
                    row.innerHTML = `
                        <td>${event.eventId}</td>
                        <td>${event.eventName}</td>
                        <td>${formattedDate}</td>
                        <td>${propertiesHtml}</td>
                    `;
                    tableBody.appendChild(row);
                });
            })
            .catch(error => {
                tableBody.innerHTML = `<tr><td colspan="4" class="text-center text-danger">Error loading events: ${error.message}</td></tr>`;
            });
    }
    
    function deleteUser(userId) {
        api.deleteUser(userId)
            .then(() => {
                alert(`User ${userId} deleted successfully`);
                loadUsers();
            })
            .catch(error => {
                alert(`Error deleting user: ${error.message}`);
            });
    }
    
    // Create User
    document.getElementById('save-user').addEventListener('click', function() {
        const userId = document.getElementById('user-id').value;
        const name = document.getElementById('user-name').value;
        const email = document.getElementById('user-email').value;
        const country = document.getElementById('user-country').value;
        const age = document.getElementById('user-age').value;
        
        if (!userId || !name || !email) {
            alert('Please fill in all required fields');
            return;
        }
        
        const userData = {
            userId: userId,
            properties: {
                name: name,
                email: email,
                country: country
            }
        };
        
        if (age) {
            userData.properties.age = parseInt(age);
        }
        
        api.createUser(userData)
            .then(() => {
                // Close modal
                bootstrap.Modal.getInstance(document.getElementById('createUserModal')).hide();
                
                // Reset form
                document.getElementById('create-user-form').reset();
                
                // Reload users
                loadUsers();
                
                alert(`User ${userId} created successfully`);
            })
            .catch(error => {
                alert(`Error creating user: ${error.message}`);
            });
    });
    
    // Event Management
    
    function loadEvents() {
        const tableBody = document.querySelector('#events-table tbody');
        tableBody.innerHTML = '<tr><td colspan="5" class="text-center">Loading events...</td></tr>';
        
        api.getAllEvents()
            .then(events => {
                if (events.length === 0) {
                    tableBody.innerHTML = '<tr><td colspan="5" class="text-center">No events found</td></tr>';
                    return;
                }
                
                tableBody.innerHTML = '';
                events.slice(0, 10).forEach(event => { // Show only the 10 most recent events
                    const row = document.createElement('tr');
                    
                    // Format timestamp
                    const date = new Date(event.timestamp);
                    const formattedDate = date.toLocaleString();
                    
                    // Format properties
                    let propertiesHtml = '<table class="properties-table">';
                    for (const [key, value] of Object.entries(event.properties)) {
                        propertiesHtml += `<tr><td>${key}</td><td>${value}</td></tr>`;
                    }
                    propertiesHtml += '</table>';
                    
                    row.innerHTML = `
                        <td>${event.eventId}</td>
                        <td>${event.eventName}</td>
                        <td>${event.userId}</td>
                        <td>${formattedDate}</td>
                        <td>${propertiesHtml}</td>
                    `;
                    tableBody.appendChild(row);
                });
            })
            .catch(error => {
                tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-danger">Error loading events: ${error.message}</td></tr>`;
            });
    }
    
    function loadEventCharts() {
        // Query for event distribution
        const distributionQuery = {
            where: [],
            aggregate: [
                {
                    field: "eventId",
                    type: "COUNT",
                    alias: "count"
                }
            ],
            select: ["eventName"]
        };
        
        api.queryEvents(distributionQuery)
            .then(result => {
                const eventNames = [];
                const eventCounts = [];
                
                result.rows.forEach(row => {
                    eventNames.push(row.eventName);
                    eventCounts.push(result.aggregations.count);
                });
                
                // Create chart
                const distributionCtx = document.getElementById('event-distribution-chart').getContext('2d');
                new Chart(distributionCtx, {
                    type: 'pie',
                    data: {
                        labels: eventNames,
                        datasets: [{
                            data: eventCounts,
                            backgroundColor: [
                                '#4e73df',
                                '#1cc88a',
                                '#36b9cc',
                                '#f6c23e',
                                '#e74a3b'
                            ]
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        legend: {
                            position: 'bottom'
                        }
                    }
                });
            })
            .catch(error => {
                console.error('Error loading event distribution chart:', error);
            });
        
        // Create timeline chart with sample data
        const timelineCtx = document.getElementById('event-timeline-chart').getContext('2d');
        
        // Generate dates for the last 7 days
        const dates = [];
        const counts = [0, 0, 0, 0, 0, 0, 0];
        
        for (let i = 6; i >= 0; i--) {
            const date = new Date();
            date.setDate(date.getDate() - i);
            dates.push(date.toLocaleDateString());
        }
        
        // Generate random counts
        for (let i = 0; i < 7; i++) {
            counts[i] = Math.floor(Math.random() * 20) + 5;
        }
        
        new Chart(timelineCtx, {
            type: 'line',
            data: {
                labels: dates,
                datasets: [{
                    label: 'Events',
                    data: counts,
                    borderColor: '#4e73df',
                    backgroundColor: 'rgba(78, 115, 223, 0.1)',
                    borderWidth: 2,
                    pointBackgroundColor: '#4e73df',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2,
                    pointRadius: 4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }
    
    // Load user dropdowns for event creation
    function loadUserDropdowns() {
        const dropdowns = [
            document.getElementById('event-user'),
            document.getElementById('recommendation-user'),
            document.getElementById('prediction-user'),
            document.getElementById('best-time-user')
        ];
        
        api.getAllUsers()
            .then(users => {
                dropdowns.forEach(dropdown => {
                    if (!dropdown) return;
                    
                    // Clear existing options except the first one
                    while (dropdown.options.length > 1) {
                        dropdown.remove(1);
                    }
                    
                    // Add user options
                    users.forEach(user => {
                        const option = document.createElement('option');
                        option.value = user.userId;
                        option.textContent = `${user.userId} (${user.properties.name || 'Unknown'})`;
                        dropdown.appendChild(option);
                    });
                });
            })
            .catch(error => {
                console.error('Error loading user dropdowns:', error);
            });
    }
    
    // Update event properties based on event type
    document.getElementById('event-name').addEventListener('change', function() {
        const eventType = this.value;
        const propertiesContainer = document.getElementById('event-properties');
        
        // Clear existing properties
        propertiesContainer.innerHTML = '';
        
        // Add common properties
        propertiesContainer.innerHTML += `
            <div class="mb-3">
                <label for="event-device" class="form-label">Device</label>
                <select class="form-select" id="event-device">
                    <option value="desktop">Desktop</option>
                    <option value="mobile">Mobile</option>
                    <option value="tablet">Tablet</option>
                </select>
            </div>
        `;
        
        // Add event-specific properties
        if (eventType === 'view_item' || eventType === 'add_to_cart' || eventType === 'purchase') {
            propertiesContainer.innerHTML += `
                <div class="mb-3">
                    <label for="event-item-id" class="form-label">Item ID</label>
                    <input type="text" class="form-control" id="event-item-id" value="item${Math.floor(Math.random() * 20) + 1}">
                </div>
                <div class="mb-3">
                    <label for="event-category" class="form-label">Category</label>
                    <select class="form-select" id="event-category">
                        <option value="electronics">Electronics</option>
                        <option value="clothing">Clothing</option>
                        <option value="books">Books</option>
                        <option value="home">Home</option>
                        <option value="sports">Sports</option>
                    </select>
                </div>
            `;
            
            if (eventType === 'view_item' || eventType === 'add_to_cart' || eventType === 'purchase') {
                propertiesContainer.innerHTML += `
                    <div class="mb-3">
                        <label for="event-price" class="form-label">Price</label>
                        <input type="number" class="form-control" id="event-price" value="${(Math.random() * 90 + 10).toFixed(2)}">
                    </div>
                `;
            }
            
            if (eventType === 'purchase') {
                propertiesContainer.innerHTML += `
                    <div class="mb-3">
                        <label for="event-quantity" class="form-label">Quantity</label>
                        <input type="number" class="form-control" id="event-quantity" value="1" min="1">
                    </div>
                `;
            }
        }
    });
    
    // Create Event
    document.getElementById('save-event').addEventListener('click', function() {
        const eventName = document.getElementById('event-name').value;
        const userId = document.getElementById('event-user').value;
        
        if (!eventName || !userId) {
            alert('Please select an event type and user');
            return;
        }
        
        // Collect properties
        const properties = {};
        
        // Add device
        const device = document.getElementById('event-device');
        if (device) {
            properties.device = device.value;
        }
        
        // Add item-related properties
        const itemId = document.getElementById('event-item-id');
        const category = document.getElementById('event-category');
        const price = document.getElementById('event-price');
        const quantity = document.getElementById('event-quantity');
        
        if (itemId) properties.item_id = itemId.value;
        if (category) properties.category = category.value;
        if (price) properties.price = parseFloat(price.value);
        if (quantity) properties.quantity = parseInt(quantity.value);
        
        const eventData = {
            eventName: eventName,
            userId: userId,
            properties: properties
        };
        
        api.createEvent(eventData)
            .then(() => {
                // Close modal
                bootstrap.Modal.getInstance(document.getElementById('createEventModal')).hide();
                
                // Reset form
                document.getElementById('create-event-form').reset();
                
                // Reload events if on events tab
                if (document.querySelector('.nav-link[data-section="events"]').classList.contains('active')) {
                    loadEvents();
                    loadEventCharts();
                }
                
                alert(`Event ${eventName} created successfully for user ${userId}`);
            })
            .catch(error => {
                alert(`Error creating event: ${error.message}`);
            });
    });
    
    // Segmentation
    
    // RFM Analysis
    document.getElementById('run-rfm-analysis').addEventListener('click', function() {
        const recencyDays = parseInt(document.getElementById('rfm-days').value);
        const numSegments = parseInt(document.getElementById('rfm-segments').value);
        
        if (isNaN(recencyDays) || isNaN(numSegments)) {
            alert('Please enter valid numbers for recency days and segments');
            return;
        }
        
        const resultsContainer = document.getElementById('rfm-results');
        resultsContainer.innerHTML = '<p class="text-center"><span class="spinner-border" role="status"></span> Running analysis...</p>';
        
        api.getRFMSegmentation(recencyDays, numSegments)
            .then(results => {
                let html = '<div class="table-responsive"><table class="table table-sm">';
                html += '<thead><tr><th>User ID</th><th>Recency</th><th>Frequency</th><th>Monetary</th><th>Segment</th></tr></thead>';
                html += '<tbody>';
                
                for (const [userId, score] of Object.entries(results)) {
                    const combined = score.recency * 100 + score.frequency * 10 + score.monetary;
                    let segmentClass = '';
                    
                    if (score.recency >= 4 && score.frequency >= 4 && score.monetary >= 4) {
                        segmentClass = 'segment-high';
                    } else if (score.recency <= 2 && score.frequency >= 4 && score.monetary >= 4) {
                        segmentClass = 'segment-low';
                    } else {
                        segmentClass = 'segment-medium';
                    }
                    
                    html += `<tr>
                        <td>${userId}</td>
                        <td>${score.recency}</td>
                        <td>${score.frequency}</td>
                        <td>${score.monetary}</td>
                        <td><span class="${segmentClass}">${score.recency}-${score.frequency}-${score.monetary}</span></td>
                    </tr>`;
                }
                
                html += '</tbody></table></div>';
                
                resultsContainer.innerHTML = html;
            })
            .catch(error => {
                resultsContainer.innerHTML = `<p class="text-danger">Error running RFM analysis: ${error.message}</p>`;
            });
    });
    
    // Cohort Analysis
    document.getElementById('run-cohort-analysis').addEventListener('click', function() {
        const timePeriod = document.getElementById('cohort-period').value;
        const numPeriods = parseInt(document.getElementById('cohort-periods').value);
        const targetEventName = document.getElementById('cohort-event').value;
        
        if (isNaN(numPeriods) || !targetEventName) {
            alert('Please enter valid values for all fields');
            return;
        }
        
        const resultsContainer = document.getElementById('cohort-results');
        resultsContainer.innerHTML = '<p class="text-center"><span class="spinner-border" role="status"></span> Running analysis...</p>';
        
        api.getCohortAnalysis(timePeriod, numPeriods, targetEventName)
            .then(results => {
                let html = '<div class="table-responsive"><table class="cohort-table">';
                html += '<thead><tr><th>Cohort</th>';
                
                for (let i = 0; i < results.numPeriods; i++) {
                    html += `<th>Period ${i}</th>`;
                }
                
                html += '</tr></thead><tbody>';
                
                for (let i = 0; i < results.numPeriods; i++) {
                    html += `<tr><td>Cohort ${i} (${results.cohortSizes[i.toString()] || 0} users)</td>`;
                    
                    for (let j = 0; j < results.numPeriods - i; j++) {
                        const percentage = results.retentionPercentages[i][j] * 100;
                        let cellClass = '';
                        
                        if (percentage >= 70) {
                            cellClass = 'cohort-cell-high';
                        } else if (percentage >= 30) {
                            cellClass = 'cohort-cell-medium';
                        } else {
                            cellClass = 'cohort-cell-low';
                        }
                        
                        html += `<td class="${cellClass}">${percentage.toFixed(1)}%</td>`;
                    }
                    
                    // Add empty cells for periods that don't apply to this cohort
                    for (let j = results.numPeriods - i; j < results.numPeriods; j++) {
                        html += '<td class="text-muted">-</td>';
                    }
                    
                    html += '</tr>';
                }
                
                html += '</tbody></table></div>';
                
                resultsContainer.innerHTML = html;
            })
            .catch(error => {
                resultsContainer.innerHTML = `<p class="text-danger">Error running cohort analysis: ${error.message}</p>`;
            });
    });
    
    // Recommendations
    
    document.getElementById('get-recommendations').addEventListener('click', function() {
        const userId = document.getElementById('recommendation-user').value;
        const count = parseInt(document.getElementById('recommendation-count').value);
        
        if (!userId || isNaN(count)) {
            alert('Please select a user and enter a valid count');
            return;
        }
        
        const resultsContainer = document.getElementById('recommendations-results');
        resultsContainer.innerHTML = '<p class="text-center"><span class="spinner-border" role="status"></span> Getting recommendations...</p>';
        
        api.getRecommendations(userId, count)
            .then(recommendations => {
                if (recommendations.length === 0) {
                    resultsContainer.innerHTML = '<p class="text-muted">No recommendations found for this user</p>';
                    return;
                }
                
                let html = '';
                recommendations.forEach(item => {
                    html += `<div class="recommendation-item">
                        <div>${item.itemId}</div>
                        <div class="recommendation-score">${item.score.toFixed(2)}</div>
                    </div>`;
                });
                
                resultsContainer.innerHTML = html;
            })
            .catch(error => {
                resultsContainer.innerHTML = `<p class="text-danger">Error getting recommendations: ${error.message}</p>`;
            });
    });
    
    document.getElementById('get-popular-items').addEventListener('click', function() {
        const count = parseInt(document.getElementById('popular-count').value);
        
        if (isNaN(count)) {
            alert('Please enter a valid count');
            return;
        }
        
        const resultsContainer = document.getElementById('popular-items-results');
        resultsContainer.innerHTML = '<p class="text-center"><span class="spinner-border" role="status"></span> Getting popular items...</p>';
        
        api.getPopularItems(count)
            .then(items => {
                if (items.length === 0) {
                    resultsContainer.innerHTML = '<p class="text-muted">No popular items found</p>';
                    return;
                }
                
                let html = '';
                items.forEach(item => {
                    html += `<div class="recommendation-item">
                        <div>${item.itemId}</div>
                        <div class="recommendation-score">${item.score.toFixed(2)}</div>
                    </div>`;
                });
                
                resultsContainer.innerHTML = html;
            })
            .catch(error => {
                resultsContainer.innerHTML = `<p class="text-danger">Error getting popular items: ${error.message}</p>`;
            });
    });
    
    // Predictions
    
    document.getElementById('get-prediction').addEventListener('click', function() {
        const userId = document.getElementById('prediction-user').value;
        const eventName = document.getElementById('prediction-event').value;
        
        if (!userId || !eventName) {
            alert('Please select a user and enter an event name');
            return;
        }
        
        const resultsContainer = document.getElementById('prediction-results');
        resultsContainer.innerHTML = '<p class="text-center"><span class="spinner-border" role="status"></span> Getting prediction...</p>';
        
        api.getPrediction(userId, eventName)
            .then(prediction => {
                const likelihood = prediction.likelihood * 100;
                let predictionClass = '';
                
                if (likelihood >= 70) {
                    predictionClass = 'prediction-high';
                } else if (likelihood >= 30) {
                    predictionClass = 'prediction-medium';
                } else {
                    predictionClass = 'prediction-low';
                }
                
                resultsContainer.innerHTML = `<div class="prediction-result ${predictionClass}">
                    ${likelihood.toFixed(1)}%
                </div>
                <p class="text-center mt-2">Likelihood of user ${userId} performing ${eventName}</p>`;
            })
            .catch(error => {
                resultsContainer.innerHTML = `<p class="text-danger">Error getting prediction: ${error.message}</p>`;
            });
    });
    
    document.getElementById('get-best-time').addEventListener('click', function() {
        const userId = document.getElementById('best-time-user').value;
        
        if (!userId) {
            alert('Please select a user');
            return;
        }
        
        const resultsContainer = document.getElementById('best-time-results');
        resultsContainer.innerHTML = '<p class="text-center"><span class="spinner-border" role="status"></span> Getting best time...</p>';
        
        api.getBestTimeToSend(userId)
            .then(result => {
                if (result.bestHour < 0) {
                    resultsContainer.innerHTML = '<p class="text-muted">Unable to determine best time for this user</p>';
                    return;
                }
                
                // Format hour
                const hour = result.bestHour;
                const formattedHour = hour === 0 ? '12 AM' : 
                                     hour < 12 ? `${hour} AM` : 
                                     hour === 12 ? '12 PM' : 
                                     `${hour - 12} PM`;
                
                resultsContainer.innerHTML = `<div class="best-time-result">
                    ${formattedHour}
                </div>
                <p class="text-center mt-2">Best time to send notifications to user ${userId}</p>`;
            })
            .catch(error => {
                resultsContainer.innerHTML = `<p class="text-danger">Error getting best time: ${error.message}</p>`;
            });
    });
    
    // Initialize
    loadUsers();
    
    // Trigger event type change to initialize properties
    document.getElementById('event-name').dispatchEvent(new Event('change'));
});
