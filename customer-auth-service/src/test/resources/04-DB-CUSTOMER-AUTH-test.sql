-- Drop tables if they exist to apply the clean new schema (Warning: Clears data)
DROP TABLE IF EXISTS users;

-- Table structure for storing customer users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255),
    full_name VARCHAR(255),
    github_id VARCHAR(100) NOT NULL UNIQUE,
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME
);