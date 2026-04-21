# TravelReservationSystemProject

## Authors
- Project Group 16
    - Megan D Souza md1817
    - Yash Kode yk564
    - Christopher Lam cl1515
    - Lucas Janjia ljj54

Project Source Files for Principles of Information & Data Mgmt.

## Repository Structure

frontend/
- src/
	- ProjectFrame.java
	- (compiled .class files can be ignored)
- pom.xml

backend/
- sql/
	- schema.sql
	- seed.sql

## Frontend (Java Swing)

Main UI file:
- frontend/src/ProjectFrame.java
- code provided from Files/Project_Files/ProjectFrame.java on canvas

This file contains:
- User add/login form
- JDBC database connection setup
- Basic user registration and login actions

## Backend (SQL)

SQL setup files:
- backend/sql/schema.sql
- backend/sql/seed.sql

`schema.sql` creates:
- Database: `reservation_system`
- Table: `users(user, password)`

`seed.sql` inserts/updates:
- Default user: `test` / `group16`

## how to run

1. Start local MySQL server. (open up mysql workbench then run schema.sql and seed.sql)
2. Run the SQL scripts in order:
	- backend/sql/schema.sql
	- backend/sql/seed.sql
3. Ensure a JDK is installed
4. Ensure Maven is installed.
5. From the repository root, move to the frontend folder:

   cd frontend

6. Compile with Maven (this automatically downloads the JDBC driver):

   mvn compile

7. Run the app with Maven:

   mvn exec:java

## Notes
