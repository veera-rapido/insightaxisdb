/**
 * API client for InsightAxisDB
 */
class TesseractAPI {
    constructor(baseUrl = '') {
        this.baseUrl = baseUrl;
    }

    /**
     * Make an API request
     */
    async request(endpoint, method = 'GET', data = null) {
        const url = `${this.baseUrl}/api/${endpoint}`;
        
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json'
            }
        };
        
        if (data) {
            options.body = JSON.stringify(data);
        }
        
        try {
            const response = await fetch(url, options);
            
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`API error (${response.status}): ${errorText}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('API request failed:', error);
            throw error;
        }
    }

    // User endpoints
    
    async getAllUsers() {
        return this.request('users');
    }
    
    async getUser(userId) {
        return this.request(`users/${userId}`);
    }
    
    async createUser(userData) {
        return this.request('users', 'POST', userData);
    }
    
    async updateUser(userId, userData) {
        return this.request(`users/${userId}`, 'PUT', userData);
    }
    
    async deleteUser(userId) {
        return this.request(`users/${userId}`, 'DELETE');
    }
    
    // Event endpoints
    
    async getAllEvents() {
        return this.request('events');
    }
    
    async getEvent(eventId) {
        return this.request(`events/${eventId}`);
    }
    
    async createEvent(eventData) {
        return this.request('events', 'POST', eventData);
    }
    
    async getUserEvents(userId) {
        return this.request(`users/${userId}/events`);
    }
    
    // Query endpoints
    
    async queryUsers(queryData) {
        return this.request('query/users', 'POST', queryData);
    }
    
    async queryEvents(queryData) {
        return this.request('query/events', 'POST', queryData);
    }
    
    async queryUserEvents(userId, queryData) {
        return this.request(`query/users/${userId}/events`, 'POST', queryData);
    }
    
    // Segmentation endpoints
    
    async getRFMSegmentation(recencyDays, numSegments) {
        return this.request(`segmentation/rfm?recencyDays=${recencyDays}&numSegments=${numSegments}`);
    }
    
    async getCohortAnalysis(timePeriod, numPeriods, targetEventName) {
        return this.request(`segmentation/cohorts?timePeriod=${timePeriod}&numPeriods=${numPeriods}&targetEventName=${targetEventName}`);
    }
    
    // ML endpoints
    
    async getRecommendations(userId, maxRecommendations) {
        return this.request(`ml/recommendations/${userId}?max=${maxRecommendations}`);
    }
    
    async getPopularItems(maxItems) {
        return this.request(`ml/recommendations/popular?max=${maxItems}`);
    }
    
    async getPrediction(userId, eventName) {
        return this.request(`ml/predictions/${userId}/${eventName}`);
    }
    
    async getBestTimeToSend(userId) {
        return this.request(`ml/best-time/${userId}`);
    }
    
    // System endpoints
    
    async getConfig() {
        return this.request('system/config');
    }
    
    async saveData() {
        return this.request('system/save', 'POST');
    }
    
    async loadData() {
        return this.request('system/load', 'POST');
    }
}
