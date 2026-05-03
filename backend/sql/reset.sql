-- reset.sql — DESTRUCTIVE: drops all tables and all data.
-- Run this only when you want a completely clean slate.
-- Sequence: reset.sql → schema.sql → seed.sql
USE reservation_system;

DROP TRIGGER IF EXISTS trg_flight_distinct_airports_bi;
DROP TRIGGER IF EXISTS trg_flight_distinct_airports_bu;
DROP TRIGGER IF EXISTS trg_ticket_distinct_airports_bi;
DROP TRIGGER IF EXISTS trg_ticket_distinct_airports_bu;

DROP TABLE IF EXISTS FlightWaitlist;
DROP TABLE IF EXISTS TicketSegment;
DROP TABLE IF EXISTS Ticket;
DROP TABLE IF EXISTS FlightOperatingDay;
DROP TABLE IF EXISTS Account;
DROP TABLE IF EXISTS Flight;
DROP TABLE IF EXISTS Aircraft;
DROP TABLE IF EXISTS Airport;
DROP TABLE IF EXISTS Customer;
DROP TABLE IF EXISTS AirlineCompany;
DROP TABLE IF EXISTS users;
