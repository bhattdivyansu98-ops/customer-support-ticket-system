# Customer Support Ticket System

Desktop help-desk application built with **Java Swing, MySQL and JDBC**.

## Features
- Create tickets (customer is saved automatically, matched by email)
- Priority levels: Low, Medium, High, Critical
- Status tracking: Open -> In Progress -> Resolved
- Assign tickets to an agent, change priority, resolve/close tickets
- Search tickets by Ticket ID, Status and Priority
- Pending Tickets report (sorted by priority, then oldest first, with age in days)

## Run
1. Run `schema.sql` in MySQL (creates `support_db` with sample data).
2. Set DB password if it is not `root`:
   - Windows: `set DB_PASS=yourpassword`
   - Linux/Mac: `export DB_PASS=yourpassword`
   (optional: `DB_USER`, `DB_URL`)
3. `mvn compile exec:java`

Or in an IDE: add `mysql-connector-j` jar to the project and run `com.support.TicketApp`.

## Tech
Java 11+, Swing, JDBC (PreparedStatement), MySQL 8
