DROP DATABASE IF EXISTS intellitrip;
CREATE DATABASE intellitrip CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE intellitrip;

CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    google_id VARCHAR(255) UNIQUE,
    role ENUM('USER', 'ADMIN') DEFAULT 'USER' NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE trips (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    country VARCHAR(255),
    image_url TEXT,
    days INT NOT NULL,
    budget ENUM('LOW', 'MEDIUM', 'LUXURY') NOT NULL,
    budget_usd DECIMAL(12,2),
    travel_type ENUM('SOLO', 'FAMILY', 'FRIENDS', 'COUPLE') NOT NULL,
    interests JSON,
    start_date DATE,
    status ENUM('UPCOMING', 'DRAFT', 'COMPLETED') DEFAULT 'DRAFT' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE itinerary_days (
    id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    day_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    theme TEXT,
    accommodations JSON,
    transportation JSON,
    cost_breakdown JSON,
    tips TEXT,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

CREATE TABLE itinerary_items (
    id VARCHAR(36) PRIMARY KEY,
    itinerary_day_id VARCHAR(36) NOT NULL,
    time VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    cost DECIMAL(10,2),
    FOREIGN KEY (itinerary_day_id) REFERENCES itinerary_days(id) ON DELETE CASCADE
);

CREATE TABLE saved_places (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    rating DECIMAL(3,1),
    price VARCHAR(50),
    image_url TEXT,
    category VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    days INT NOT NULL,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE analytics (
    id VARCHAR(36) PRIMARY KEY,
    total_users BIGINT DEFAULT 0,
    total_trips BIGINT DEFAULT 0,
    total_generations BIGINT DEFAULT 0,
    avg_trip_days DOUBLE DEFAULT 0.0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_trips_user_id ON trips(user_id);
CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_itinerary_trip_id ON itinerary_days(trip_id);
CREATE INDEX idx_saved_places_user_id ON saved_places(user_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);

INSERT INTO analytics (id, total_users, total_trips, total_generations, avg_trip_days) VALUES
(UUID(), 12480, 38291, 84012, 6.4);
