USE reservation_system;

INSERT INTO users (user, password, role)
VALUES ('admin', 'admin', 'admin')
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role);

INSERT INTO users (user, password, role)
VALUES ('test', 'group16', 'customer')
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role);

INSERT INTO users (user, password, role)
VALUES ('rep1', 'rep1', 'rep')
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role);

-- Airlines
INSERT IGNORE INTO AirlineCompany (AirlineID, AirlineName) VALUES
    ('DL', 'Delta Air Lines'),
    ('AA', 'American Airlines'),
    ('UA', 'United Airlines'),
    ('WN', 'Southwest Airlines'),
    ('B6', 'JetBlue Airways');

-- Airports
INSERT IGNORE INTO Airport (AirportCode, AirportName, Location) VALUES
    ('ATL', 'Hartsfield-Jackson Atlanta International', 'Atlanta, GA'),
    ('LAX', 'Los Angeles International',               'Los Angeles, CA'),
    ('ORD', 'O\'Hare International',                   'Chicago, IL'),
    ('DFW', 'Dallas/Fort Worth International',         'Dallas, TX'),
    ('JFK', 'John F. Kennedy International',           'New York, NY'),
    ('BOS', 'Logan International',                     'Boston, MA'),
    ('SFO', 'San Francisco International',             'San Francisco, CA'),
    ('MIA', 'Miami International',                     'Miami, FL'),
    ('SEA', 'Seattle-Tacoma International',            'Seattle, WA'),
    ('DEN', 'Denver International',                    'Denver, CO');

-- Aircraft
INSERT IGNORE INTO Aircraft (AircraftID, AirlineModel, SeatCapacity, AirlineID) VALUES
    ('DL-001', 'Boeing 737',    150, 'DL'),
    ('AA-001', 'Boeing 757',    200, 'AA'),
    ('UA-001', 'Airbus A320',   180, 'UA'),
    ('WN-001', 'Boeing 737',    143, 'WN'),
    ('B6-001', 'Airbus A321',   190, 'B6');

-- Flights
INSERT IGNORE INTO Flight
    (FlightNumber, AirlineID, Airline_Name, Travel_Type, DepartureTime, ArrivalTime,
     DepartureAirport, ArrivalAirport, AircraftID, BaseFare)
VALUES
    (100, 'DL', 'Delta Air Lines',    'domestic', '2026-05-01 08:00:00', '2026-05-01 11:30:00', 'ATL', 'JFK', 'DL-001', 250.00),
    (101, 'DL', 'Delta Air Lines',    'domestic', '2026-05-01 13:00:00', '2026-05-01 16:30:00', 'JFK', 'ATL', 'DL-001', 250.00),
    (102, 'DL', 'Delta Air Lines',    'domestic', '2026-05-06 08:00:00', '2026-05-06 11:00:00', 'ATL', 'LAX', 'DL-001', 350.00),
    (200, 'AA', 'American Airlines',  'domestic', '2026-05-02 07:00:00', '2026-05-02 12:30:00', 'LAX', 'ORD', 'AA-001', 320.00),
    (201, 'AA', 'American Airlines',  'domestic', '2026-05-02 14:00:00', '2026-05-02 17:30:00', 'ORD', 'LAX', 'AA-001', 320.00),
    (202, 'AA', 'American Airlines',  'domestic', '2026-05-07 09:00:00', '2026-05-07 12:30:00', 'JFK', 'MIA', 'AA-001', 200.00),
    (300, 'UA', 'United Airlines',    'domestic', '2026-05-03 09:00:00', '2026-05-03 12:30:00', 'SFO', 'DEN', 'UA-001', 180.00),
    (301, 'UA', 'United Airlines',    'domestic', '2026-05-03 15:00:00', '2026-05-03 17:30:00', 'DEN', 'SFO', 'UA-001', 180.00),
    (400, 'WN', 'Southwest Airlines', 'domestic', '2026-05-04 10:00:00', '2026-05-04 13:30:00', 'DFW', 'MIA', 'WN-001', 150.00),
    (500, 'B6', 'JetBlue Airways',    'domestic', '2026-05-05 06:00:00', '2026-05-05 11:30:00', 'BOS', 'SEA', 'B6-001', 280.00);

-- FlightOperatingDay — recurring schedule for each flight
-- Uses subquery on FlightNumber+AirlineID so IDs don't need to be hardcoded
INSERT IGNORE INTO FlightOperatingDay (FlightID, DayOfWeek)
-- DL 100 (ATL→JFK): Mon / Wed / Fri
SELECT f.FlightID, d.day FROM Flight f
    JOIN (SELECT 'Mon' AS day UNION ALL SELECT 'Wed' UNION ALL SELECT 'Fri') d
    WHERE f.FlightNumber = 100 AND f.AirlineID = 'DL'
UNION ALL
-- DL 101 (JFK→ATL): Mon / Wed / Fri  (return leg mirrors outbound)
SELECT f.FlightID, d.day FROM Flight f
    JOIN (SELECT 'Mon' AS day UNION ALL SELECT 'Wed' UNION ALL SELECT 'Fri') d
    WHERE f.FlightNumber = 101 AND f.AirlineID = 'DL'
UNION ALL
-- DL 102 (ATL→LAX): Tue / Thu / Sat
SELECT f.FlightID, d.day FROM Flight f
    JOIN (SELECT 'Tue' AS day UNION ALL SELECT 'Thu' UNION ALL SELECT 'Sat') d
    WHERE f.FlightNumber = 102 AND f.AirlineID = 'DL'
UNION ALL
-- AA 200 (LAX→ORD): Mon–Fri (weekdays)
SELECT f.FlightID, d.day FROM Flight f
    JOIN (SELECT 'Mon' AS day UNION ALL SELECT 'Tue' UNION ALL SELECT 'Wed'
          UNION ALL SELECT 'Thu' UNION ALL SELECT 'Fri') d
    WHERE f.FlightNumber = 200 AND f.AirlineID = 'AA'
UNION ALL
-- AA 201 (ORD→LAX): Mon–Fri (return)
SELECT f.FlightID, d.day FROM Flight f
    JOIN (SELECT 'Mon' AS day UNION ALL SELECT 'Tue' UNION ALL SELECT 'Wed'
          UNION ALL SELECT 'Thu' UNION ALL SELECT 'Fri') d
    WHERE f.FlightNumber = 201 AND f.AirlineID = 'AA'
UNION ALL
-- AA 202 (JFK→MIA): Wed / Sat / Sun
SELECT f.FlightID, d.day FROM Flight f
    JOIN (SELECT 'Wed' AS day UNION ALL SELECT 'Sat' UNION ALL SELECT 'Sun') d
    WHERE f.FlightNumber = 202 AND f.AirlineID = 'AA'
UNION ALL
-- UA 300 (SFO→DEN): Mon / Fri / Sun
SELECT f.FlightID, d.day FROM Flight f
    JOIN (SELECT 'Mon' AS day UNION ALL SELECT 'Fri' UNION ALL SELECT 'Sun') d
    WHERE f.FlightNumber = 300 AND f.AirlineID = 'UA'
UNION ALL
-- UA 301 (DEN→SFO): Mon / Fri / Sun  (return)
SELECT f.FlightID, d.day FROM Flight f
    JOIN (SELECT 'Mon' AS day UNION ALL SELECT 'Fri' UNION ALL SELECT 'Sun') d
    WHERE f.FlightNumber = 301 AND f.AirlineID = 'UA'
UNION ALL
-- WN 400 (DFW→MIA): daily
SELECT f.FlightID, d.day FROM Flight f
    JOIN (SELECT 'Mon' AS day UNION ALL SELECT 'Tue' UNION ALL SELECT 'Wed' UNION ALL SELECT 'Thu'
          UNION ALL SELECT 'Fri' UNION ALL SELECT 'Sat' UNION ALL SELECT 'Sun') d
    WHERE f.FlightNumber = 400 AND f.AirlineID = 'WN'
UNION ALL
-- B6 500 (BOS→SEA): Tue / Thu / Sat
SELECT f.FlightID, d.day FROM Flight f
    JOIN (SELECT 'Tue' AS day UNION ALL SELECT 'Thu' UNION ALL SELECT 'Sat') d
    WHERE f.FlightNumber = 500 AND f.AirlineID = 'B6';

-- Customer and Account for user 'yk564' (allows booking test)
INSERT IGNORE INTO Customer (CustomerID, FirstName, LastName, Email) VALUES
    (1, 'Yash', 'Kode', 'yashkode@gmail.com');

INSERT IGNORE INTO Account (AccountID, CustomerID) VALUES
    ('yk564', 1);
