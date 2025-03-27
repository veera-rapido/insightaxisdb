# InsightAxisDB Dashboard Guide

The InsightAxisDB Dashboard provides a user-friendly interface for interacting with the database. This guide explains how to use each section of the dashboard.

## Accessing the Dashboard

The dashboard is available at `http://localhost:8080` when the InsightAxisDB server is running.

## Navigation

The dashboard is divided into several sections, accessible from the navigation bar at the top:

- **Users**: Manage user profiles
- **Events**: Track and analyze user events
- **Segmentation**: Segment users based on behavior
- **Recommendations**: Generate personalized recommendations
- **Predictions**: Predict user behavior

## Users Section

The Users section allows you to view and manage user profiles.

### Features:

- **View Users**: See a list of all users with their basic information
- **Create User**: Add a new user with custom properties
- **View User Events**: See all events for a specific user
- **Delete User**: Remove a user from the database

### Creating a User:

1. Click the "Create User" button
2. Fill in the required fields (User ID, Name, Email)
3. Add optional properties (Country, Age)
4. Click "Create User" to save

## Events Section

The Events section allows you to track and analyze user events.

### Features:

- **View Events**: See a list of recent events across all users
- **Create Event**: Add a new event for a user
- **Event Distribution**: View a pie chart showing the distribution of event types
- **Event Timeline**: View a line chart showing event frequency over time

### Creating an Event:

1. Click the "Create Event" button
2. Select an event type (login, view_item, add_to_cart, purchase, logout)
3. Select a user
4. Fill in the event properties (varies by event type)
5. Click "Create Event" to save

## Segmentation Section

The Segmentation section allows you to segment users based on their behavior.

### Features:

- **RFM Analysis**: Segment users based on Recency, Frequency, and Monetary value
- **Cohort Analysis**: Track user retention over time

### Running RFM Analysis:

1. Set the recency days (e.g., 30)
2. Set the number of segments (e.g., 5)
3. Click "Run Analysis"
4. View the results in the table below

### Running Cohort Analysis:

1. Select the time period (Day, Week, Month)
2. Set the number of periods (e.g., 4)
3. Set the target event (e.g., login)
4. Click "Run Analysis"
5. View the results in the table below

## Recommendations Section

The Recommendations section allows you to generate personalized recommendations for users.

### Features:

- **Item Recommendations**: Generate personalized recommendations for a specific user
- **Popular Items**: View the most popular items across all users

### Getting Recommendations:

1. Select a user
2. Set the number of recommendations (e.g., 5)
3. Click "Get Recommendations"
4. View the recommendations in the list below

### Getting Popular Items:

1. Set the number of items (e.g., 5)
2. Click "Get Popular Items"
3. View the popular items in the list below

## Predictions Section

The Predictions section allows you to predict user behavior.

### Features:

- **Event Likelihood**: Predict the likelihood of a user performing a specific event
- **Best Time to Send**: Determine the optimal time to send notifications to a user

### Getting Event Likelihood:

1. Select a user
2. Set the event name (e.g., purchase)
3. Click "Get Prediction"
4. View the prediction result below

### Getting Best Time to Send:

1. Select a user
2. Click "Get Best Time"
3. View the best time result below
