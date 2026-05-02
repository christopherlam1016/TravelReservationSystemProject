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

-- Customer and Account for user 'yk564' (allows booking test)
INSERT IGNORE INTO Customer (CustomerID, FirstName, LastName, Email) VALUES
    (1, 'Yash', 'Kode', 'yashkode@gmail.com');

INSERT IGNORE INTO Account (AccountID, CustomerID) VALUES
    ('yk564', 1);
