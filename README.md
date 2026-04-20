# TravelReservationSystemProject
Java Code + SQL scripts for Princ. Info Project

## Project Structure

```
TravelReservationSystemProject/
├── src/
│   └── ProjectFrame.java   # Main Swing login UI
├── sql/
│   └── setup.sql           # Database & table creation script
└── README.md
```

## Prerequisites

| Tool | Version |
|------|---------|
| Java (JDK) | 11 or newer |
| MySQL Server | 8.0 or newer |
| MySQL Connector/J | 8.x (JDBC driver) |

Download the MySQL Connector/J JAR from
<https://dev.mysql.com/downloads/connector/j/> and place it anywhere
convenient (e.g. `lib/mysql-connector-j-8.x.xx.jar`).

## Database Setup

Before running the setup script, open `sql/setup.sql` and change the password
on line 11 to a strong value of your choice. Then run the script as a
privileged MySQL user:

```bash
mysql -u root -p < sql/setup.sql
```

This will:
1. Create the `testproject` database.
2. Create the `testuser` account with your chosen password.
3. Create the `users` table.

Then set the matching credentials via environment variables before launching
the application (see **Run** below).

## Compile

```bash
javac -cp lib/mysql-connector-j-8.x.xx.jar src/ProjectFrame.java -d out/
```

## Run

The application reads database credentials from environment variables,
falling back to development defaults if they are not set:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:mysql://localhost:3306/testproject` | JDBC connection URL |
| `DB_USER` | `testuser` | Database username |
| `DB_PASSWORD` | `abc123` | Database password |

```bash
DB_URL=jdbc:mysql://localhost:3306/testproject \
DB_USER=testuser \
DB_PASSWORD=<your_password> \
java -cp "out:lib/mysql-connector-j-8.x.xx.jar" ProjectFrame
```

> **Windows note:** replace `:` with `;` in the classpath and set environment
> variables with `set` or in System Properties before running:
> ```
> java -cp "out;lib\mysql-connector-j-8.x.xx.jar" ProjectFrame
> ```

## Usage

| Button | Action |
|--------|--------|
| **Add User** | Inserts the username/password into the `users` table |
| **Login** | Verifies credentials against the database |
| **Clear** | Clears the username and password fields |
