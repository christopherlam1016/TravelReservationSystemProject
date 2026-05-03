-- Active: 1776748666822@@127.0.0.1@3306@reservation_system
-- Safe to re-run: uses CREATE TABLE IF NOT EXISTS so existing data is never dropped.
-- To wipe all data and start fresh, run reset.sql first, then this file, then seed.sql.
CREATE DATABASE IF NOT EXISTS reservation_system;
USE reservation_system;

/* create initial user and grant privileges */
CREATE USER IF NOT EXISTS 'test'@'localhost' IDENTIFIED BY 'group16';
ALTER USER 'test'@'localhost' IDENTIFIED BY 'group16';
GRANT ALL PRIVILEGES ON reservation_system.* TO 'test'@'localhost';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS users (
    user VARCHAR(100) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role ENUM('admin', 'rep', 'customer') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/* Airline Reservation System Schema (can be changed in the future) */

-- 1. AirlineCompany Table
CREATE TABLE IF NOT EXISTS AirlineCompany (
    AirlineID CHAR(2) PRIMARY KEY,
    AirlineName VARCHAR(255) NOT NULL UNIQUE
);

-- 2. Airport Table
CREATE TABLE IF NOT EXISTS Airport (
    AirportCode CHAR(3) PRIMARY KEY,
    AirportName VARCHAR(100) NOT NULL,
    Location VARCHAR(255) NOT NULL
);

-- 3. Customer Table
CREATE TABLE IF NOT EXISTS Customer (
    CustomerID INT AUTO_INCREMENT PRIMARY KEY,
    FirstName VARCHAR(100) NOT NULL,
    LastName VARCHAR(100) NOT NULL,
    Email VARCHAR(255) UNIQUE,
    Phone VARCHAR(30)
);

-- 4. Aircraft Table
CREATE TABLE IF NOT EXISTS Aircraft (
    AircraftID VARCHAR(50) PRIMARY KEY,
    AirlineModel VARCHAR(100) NOT NULL,
    SeatCapacity INT NOT NULL,
    AirlineID CHAR(2) NOT NULL,
    CONSTRAINT fk_airline FOREIGN KEY (AirlineID) REFERENCES AirlineCompany(AirlineID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- 5. Flight Table
CREATE TABLE IF NOT EXISTS Flight (
    FlightID BIGINT AUTO_INCREMENT PRIMARY KEY,
    FlightNumber INT NOT NULL,
    AirlineID CHAR(2) NOT NULL,
    Airline_Name VARCHAR(255) NOT NULL,
    Travel_Type ENUM('domestic', 'international') NOT NULL,
    DepartureTime DATETIME NOT NULL,
    ArrivalTime DATETIME NOT NULL,
    DepartureAirport CHAR(3) NOT NULL,
    ArrivalAirport CHAR(3) NOT NULL,
    AircraftID VARCHAR(50) NOT NULL,
    BaseFare DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT uq_flight_number_per_airline UNIQUE (AirlineID, FlightNumber),
    CONSTRAINT fk_flight_airline FOREIGN KEY (AirlineID) REFERENCES AirlineCompany(AirlineID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_dep_airport FOREIGN KEY (DepartureAirport) REFERENCES Airport(AirportCode)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_arr_airport FOREIGN KEY (ArrivalAirport) REFERENCES Airport(AirportCode)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_aircraft FOREIGN KEY (AircraftID) REFERENCES Aircraft(AircraftID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- 5a. Operating days for recurring flights
CREATE TABLE IF NOT EXISTS FlightOperatingDay (
    FlightID BIGINT NOT NULL,
    DayOfWeek ENUM('Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun') NOT NULL,
    PRIMARY KEY (FlightID, DayOfWeek),
    CONSTRAINT fk_operatingday_flight FOREIGN KEY (FlightID) REFERENCES Flight(FlightID)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- 6. Account Table
CREATE TABLE IF NOT EXISTS Account (
    AccountID VARCHAR(50) PRIMARY KEY,
    CustomerID INT NOT NULL UNIQUE,
    CreatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_acc FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- 7. Ticket Table (one ticket per passenger reservation)
CREATE TABLE IF NOT EXISTS Ticket (
    TicketID VARCHAR(50) PRIMARY KEY,
    TicketNumber BIGINT NOT NULL UNIQUE,
    TicketType ENUM('one_way', 'round_trip') NOT NULL,
    FlightClass ENUM('economy', 'business', 'first') NOT NULL,
    BookingFee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    TotalFare DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    PurchaseDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Flexibility BOOLEAN NOT NULL DEFAULT FALSE,
    FromAirport CHAR(3) NOT NULL,
    ToAirport CHAR(3) NOT NULL,
    IsPaid BOOLEAN NOT NULL DEFAULT FALSE,
    Status ENUM('booked', 'changed', 'cancelled', 'completed') NOT NULL DEFAULT 'booked',
    CustomerID INT NOT NULL,
    AccountID VARCHAR(50) NOT NULL,
    CONSTRAINT fk_ticket_from_airport FOREIGN KEY (FromAirport) REFERENCES Airport(AirportCode)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_to_airport FOREIGN KEY (ToAirport) REFERENCES Airport(AirportCode)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_customer FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_account FOREIGN KEY (AccountID) REFERENCES Account(AccountID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- 7a. Ticket to Flight segments (supports direct and multi-leg itineraries)
CREATE TABLE IF NOT EXISTS TicketSegment (
    TicketID VARCHAR(50) NOT NULL,
    SegmentOrder INT NOT NULL,
    FlightID BIGINT NOT NULL,
    DepartureDate DATE NOT NULL,
    SeatNumber VARCHAR(10),
    SpecialMeal VARCHAR(100),
    SegmentFare DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (TicketID, SegmentOrder),
    CONSTRAINT fk_segment_ticket FOREIGN KEY (TicketID) REFERENCES Ticket(TicketID)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_segment_flight FOREIGN KEY (FlightID) REFERENCES Flight(FlightID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT uq_flight_seat_per_day UNIQUE (FlightID, DepartureDate, SeatNumber)
);

-- 8. FlightWaitlist Table
CREATE TABLE IF NOT EXISTS FlightWaitlist (
    WaitlistID BIGINT AUTO_INCREMENT PRIMARY KEY,
    FlightID BIGINT NOT NULL,
    CustomerID INT NOT NULL,
    RequestedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_waitlist_customer_per_flight UNIQUE (FlightID, CustomerID),
    CONSTRAINT fk_waitlist_flight FOREIGN KEY (FlightID) REFERENCES Flight(FlightID)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_waitlist_customer FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);