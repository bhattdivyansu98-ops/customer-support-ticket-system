CREATE DATABASE IF NOT EXISTS support_db;
USE support_db;

CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS tickets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT,
    priority ENUM('Low','Medium','High','Critical') NOT NULL DEFAULT 'Medium',
    status ENUM('Open','In Progress','Resolved') NOT NULL DEFAULT 'Open',
    assigned_to VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_ticket_status ON tickets(status);
CREATE INDEX idx_ticket_priority ON tickets(priority);

INSERT IGNORE INTO customers (name, email, phone) VALUES
('Rahul Sharma','rahul@example.com','9800000001'),
('Priya Verma','priya@example.com','9800000002'),
('Amit Patel','amit@example.com','9800000003');

INSERT INTO tickets (customer_id, subject, description, priority, status, assigned_to) VALUES
(1,'Wi-Fi not connecting','Laptop shows connected but no internet.','High','Open',NULL),
(2,'Password reset request','Forgot email account password.','Medium','In Progress','Divyanshu'),
(3,'Printer offline','Office printer shows offline status.','Low','Resolved','Divyanshu'),
(1,'MS Office installation failed','Error while installing Office.','Critical','Open',NULL);
