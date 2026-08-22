CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NULL,
    country VARCHAR(255) NULL,
    country_code VARCHAR(10) NULL
);

CREATE TABLE IF NOT EXISTS trips (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    country VARCHAR(255) NULL,
    image VARCHAR(255) NULL,
    days INT NOT NULL,
    budget VARCHAR(255) NULL,
    budget_usd DOUBLE NULL,
    travel_type VARCHAR(255) NULL,
    interests VARCHAR(255) NULL,
    accommodation_preference VARCHAR(255) NULL,
    destination_currency_code VARCHAR(10) NULL,
    itinerary_json TEXT NOT NULL,
    trip_status VARCHAR(255) DEFAULT 'draft',
    is_saved BOOLEAN DEFAULT FALSE,
    start_date VARCHAR(255) NULL,
    created_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS saved_places (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NULL,
    rating DOUBLE NULL,
    price VARCHAR(255) NULL,
    image VARCHAR(255) NULL,
    category VARCHAR(255) NULL,
    description TEXT NULL,
    created_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
