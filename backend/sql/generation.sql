/* code to populate database tables with sample data for testing purposes */

USE reservation_system;

-- 0) Login users (independent)
INSERT INTO users (user, password, role)
VALUES
	('admin', 'admin', 'admin'),
	('rep1', 'rep1', 'rep'),
	('test', 'group16', 'customer')
ON DUPLICATE KEY UPDATE
	password = VALUES(password),
	role = VALUES(role);

-- 1) Parent reference data
INSERT INTO AirlineCompany (AirlineID, AirlineName)
VALUES
	('AA', 'American Airlines'),
	('DL', 'Delta Air Lines')
ON DUPLICATE KEY UPDATE
	AirlineName = VALUES(AirlineName);

INSERT INTO Airport (AirportCode, AirportName, Location)
VALUES
	('JFK', 'John F. Kennedy International Airport', 'New York, NY, USA'),
	('LAX', 'Los Angeles International Airport', 'Los Angeles, CA, USA'),
	('SFO', 'San Francisco International Airport', 'San Francisco, CA, USA'),
	('LHR', 'Heathrow Airport', 'London, UK')
ON DUPLICATE KEY UPDATE
	AirportName = VALUES(AirportName),
	Location = VALUES(Location);

INSERT INTO Customer (CustomerID, FirstName, LastName, Email, Phone)
VALUES
	(1, 'Megan', 'Souza', 'megan.souza@rutgers.edu', '555-1001'),
	(2, 'Yash', 'Kode', 'yash.kode@rutgers.edu', '555-1002'),
	(3, 'Chris', 'Lam', 'chris.lam@rutgers.edu', '555-1003'),
    (4, 'Lucas', 'Ganjia', 'lucas.ganjia@rutgers.edu', '555-1004')
ON DUPLICATE KEY UPDATE
	FirstName = VALUES(FirstName),
	LastName = VALUES(LastName),
	Email = VALUES(Email),
	Phone = VALUES(Phone);

-- 2) Children of AirlineCompany / Customer
INSERT INTO Aircraft (AircraftID, AirlineModel, SeatCapacity, AirlineID)
VALUES
	('AA-B737-001', 'Boeing 737-800', 160, 'AA'),
	('DL-A330-001', 'Airbus A330-300', 280, 'DL')
ON DUPLICATE KEY UPDATE
	AirlineModel = VALUES(AirlineModel),
	SeatCapacity = VALUES(SeatCapacity),
	AirlineID = VALUES(AirlineID);

INSERT INTO Account (AccountID, CustomerID, CreatedAt)
VALUES
	('ACC001', 1, '2026-04-01 10:00:00'),
	('ACC002', 2, '2026-04-02 10:00:00'),
	('ACC003', 3, '2026-04-03 10:00:00')
ON DUPLICATE KEY UPDATE
	CustomerID = VALUES(CustomerID),
	CreatedAt = VALUES(CreatedAt);

-- 3) Flights (depends on AirlineCompany, Airport, Aircraft)
INSERT INTO Flight (
	FlightID,
	FlightNumber,
	AirlineID,
	Airline_Name,
	Travel_Type,
	DepartureTime,
	ArrivalTime,
	DepartureAirport,
	ArrivalAirport,
	AircraftID,
	BaseFare
)
VALUES
	(1001, 101, 'AA', 'American Airlines', 'domestic', '2026-05-01 08:00:00', '2026-05-01 11:15:00', 'JFK', 'LAX', 'AA-B737-001', 220.00),
	(1002, 102, 'AA', 'American Airlines', 'domestic', '2026-05-02 14:30:00', '2026-05-02 22:45:00', 'LAX', 'JFK', 'AA-B737-001', 210.00),
	(1003, 201, 'DL', 'Delta Air Lines', 'international', '2026-05-03 16:00:00', '2026-05-04 10:00:00', 'SFO', 'LHR', 'DL-A330-001', 620.00),
	(1004, 202, 'DL', 'Delta Air Lines', 'international', '2026-05-10 13:00:00', '2026-05-10 17:00:00', 'LHR', 'SFO', 'DL-A330-001', 610.00)
ON DUPLICATE KEY UPDATE
	FlightNumber = VALUES(FlightNumber),
	AirlineID = VALUES(AirlineID),
	Airline_Name = VALUES(Airline_Name),
	Travel_Type = VALUES(Travel_Type),
	DepartureTime = VALUES(DepartureTime),
	ArrivalTime = VALUES(ArrivalTime),
	DepartureAirport = VALUES(DepartureAirport),
	ArrivalAirport = VALUES(ArrivalAirport),
	AircraftID = VALUES(AircraftID),
	BaseFare = VALUES(BaseFare);

-- 4) Flight operating days (depends on Flight)
INSERT INTO FlightOperatingDay (FlightID, DayOfWeek)
VALUES
	(1001, 'Mon'),
	(1001, 'Wed'),
	(1001, 'Fri'),
	(1002, 'Tue'),
	(1002, 'Thu'),
	(1002, 'Sat'),
	(1003, 'Mon'),
	(1003, 'Thu'),
	(1004, 'Tue'),
	(1004, 'Sun')
ON DUPLICATE KEY UPDATE
	DayOfWeek = VALUES(DayOfWeek);

-- 5) Tickets (depends on Airport, Customer, Account)
INSERT INTO Ticket (
	TicketID,
	TicketNumber,
	TicketType,
	FlightClass,
	BookingFee,
	TotalFare,
	PurchaseDate,
	Flexibility,
	FromAirport,
	ToAirport,
	IsPaid,
	Status,
	CustomerID,
	AccountID
)
VALUES
	('TICK001', 900000001, 'one_way', 'economy', 20.00, 240.00, '2026-04-10 09:15:00', FALSE, 'JFK', 'LAX', TRUE, 'booked', 1, 'ACC001'),
	('TICK002', 900000002, 'round_trip', 'business', 30.00, 1260.00, '2026-04-11 14:00:00', TRUE, 'SFO', 'LHR', TRUE, 'booked', 2, 'ACC002'),
	('TICK003', 900000003, 'one_way', 'economy', 20.00, 230.00, '2026-04-12 16:20:00', FALSE, 'LAX', 'JFK', FALSE, 'booked', 3, 'ACC003')
ON DUPLICATE KEY UPDATE
	TicketNumber = VALUES(TicketNumber),
	TicketType = VALUES(TicketType),
	FlightClass = VALUES(FlightClass),
	BookingFee = VALUES(BookingFee),
	TotalFare = VALUES(TotalFare),
	PurchaseDate = VALUES(PurchaseDate),
	Flexibility = VALUES(Flexibility),
	FromAirport = VALUES(FromAirport),
	ToAirport = VALUES(ToAirport),
	IsPaid = VALUES(IsPaid),
	Status = VALUES(Status),
	CustomerID = VALUES(CustomerID),
	AccountID = VALUES(AccountID);

-- 6) Ticket segments (depends on Ticket and Flight)
INSERT INTO TicketSegment (
	TicketID,
	SegmentOrder,
	FlightID,
	DepartureDate,
	SeatNumber,
	SpecialMeal,
	SegmentFare
)
VALUES
	('TICK001', 1, 1001, '2026-05-15', '12A', 'Vegetarian', 220.00),
	('TICK002', 1, 1003, '2026-06-01', '14C', 'Standard', 620.00),
	('TICK002', 2, 1004, '2026-06-10', '15C', 'Standard', 610.00),
	('TICK003', 1, 1002, '2026-05-20', '18B', 'Kosher', 210.00)
ON DUPLICATE KEY UPDATE
	FlightID = VALUES(FlightID),
	DepartureDate = VALUES(DepartureDate),
	SeatNumber = VALUES(SeatNumber),
	SpecialMeal = VALUES(SpecialMeal),
	SegmentFare = VALUES(SegmentFare);

-- 7) Waitlist (depends on Flight and Customer)
INSERT INTO FlightWaitlist (FlightID, CustomerID, RequestedAt)
VALUES
	(1001, 2, '2026-04-20 11:00:00'),
	(1003, 1, '2026-04-21 12:30:00')
ON DUPLICATE KEY UPDATE
	RequestedAt = VALUES(RequestedAt);

-- Keep AUTO_INCREMENT ahead of explicit IDs used above
ALTER TABLE Customer AUTO_INCREMENT = 4;
ALTER TABLE Flight AUTO_INCREMENT = 1005;