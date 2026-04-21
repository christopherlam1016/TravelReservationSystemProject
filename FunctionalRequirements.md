# Programming Project
(this is just the assignment doc)

## Introduction
In this project, you will design and implement a relational database system to support
the operations of an online travel reservation system. You will use Java Swing for the
user interface, MySQL for the database server, and Java, JDBC for connectivity between the
user interface and the database server.


## Project Specification
As you probably know, there are a multitude of online travel reservation systems on the web.
Some popular ones are expedia, skyscanner and kayak. I suggest that you visit these web sites to
get an understanding of the look-and-feel of a travel reservation system and how it is
supposed to function.

The basic idea behind your travel reservation system is that it will allow customers to use a
standalone application to browse/search the contents of your database (at least that part you want
the customer to see) and to make flight reservations. Your application should allow users to
make both domestic and international reservations. It should also allow users to
query the database for available flights (direct or indirect) between a pair of cities for a
given date and "approximate" time.

Actual travel sites allow you to do a lot more than simply make flight reservations. For example,
you can book a rental car or a hotel room. Due to time limitations, we will stick to
flight reservations.

Your database system must be based on the specifications and requirements that follow.

## 1 System Users
The users of your system will be the customers (passengers) that use your system to make a flight
reservation, customer representatives who provide customer-related services, and the site's admin.
You should assume that the computer knowledge of the users is limited, and thus your system
must be easy to operate.

The data items required for the travel reservation database can be classified into six categories:
airlines, aircrafts, airports, flights, tickets, customers and employees.
The above classification does not imply any particular table arrangement. You are responsible
for arranging the data items into tables, determining the relationships among tables and identifying
the key attributes. Finally, you should specify and enforce integrity constraints on the data,
including referential integrity constraints.

You will first create an E-R diagram of your online travel reservation system before developing
your relational model according to the functional requirements described below.

## 2 Functional Requirements
Every airline company owns a number of aircrafts and it is associated with a number of airports
from where it operates. Each airline has a two-letter ID from which it is being identified uniquely.
For example, the ID for American Airlines is AA, and the ID for United Airlines is UA. Similarly,
each airport has a three-letter ID. For example, EWR, LGA, and JFK are well known local airport
codes.

A flight is operated by an airline and a specific aircraft, and operates on a given set of days of the
week (e.g. every Monday, Wednesday). Flights can either be domestic or international. For every
flight, it must be recorded its flight number (unique only within that airline), the departure and
destination airports, as well as the departure and arrival time.

Customers should be able to make reservations, buy tickets and if there are not available seats for
a specific flight, they should be able to get into the flight’s waiting list. Customers should first be
able to search for specific flights by providing information about the departure and arrival airport
as well as the date they wish to fly. The flight ticket can either be one-way, round-trip and they
should be able to set if they are flexible about flight dates (+- 3 days).

A flight ticket has a unique number and is for just a single passenger. Each ticket is associated with
a sequence of flights. For example a ticket might be associated with just one flight if it is one-way
and direct or with 2 flights if it is direct and round-trip or more than 2 if it has stops (either for
one-way or round-trip). Each ticket must include all the associated flights and include: from-
airport, to-airport, flight numbers (along with its airline), departure date and time, special meal
ordered, seat number, and class (economy/business/first). It also has the following attributes: total
fare, and date and time when ticket was purchased. In case the class of the ticket is economy, the
customer should not be able to change/cancel their ticket unless a fee is paid. For business/first
class, customers should be able to change their ticket with no fee. Finally, a flight ticket also has
an associated booking fee, which is how your company makes money.

A customer may partake in any number of flight transactions and s/he is associated with one
account which includes a reservation portfolio, indicating all the flight history held in this account
(past flights and upcoming).

Your online reservation system should have the following functionality:
### Admin-Level Functionality
The admin should be able to:
- Add, Edit and Delete information for a customer representative or customer
- Obtain a sales report for a particular month
- Produce a list of reservations by flight number or by customer name
- Produce a summary listing of revenue generated by a particular flight, airline or customer
- Determine which customer generated most total revenue
- Produce a list of most active flights (most tickets sold)
- Produce a list of all flights for a given airport

### Customer-Representative-Level Functionality

Customer Representatives should be thought of as reservation agents and should be able to:
- Make flight reservations on behalf of users
- Edit flight reservations for a customer
- Add, Edit, Delete information for aircrafts, airports and flights
- Retrieve a list of all the passengers who are on the waiting list of a particular flight

### Customer-Level Functionality
Customers should be thought of as airline ticket buyers and should be able to easily browse your
travel reservation system and buy flight tickets. In particular, they should be able to search and
make the following types of reservations:
- One-Way
- Round-Trip
- Flexible Date/time
A customer should also be able to:
- sort flights by different criteria (price, take-off time, landing time)
- filter the list of flights by various criteria (price, number of stops, airline)
- cancel their flight reservations (if it is business or first class)
- enter the waiting list if the flight is full
- view all the past reservations with their details
- view all the upcoming reservations with their details

## 3 User Access Control
Your database system should provide controlled access to the data by distinguishing between the
different types of users: admin, customer representatives, and customers.
- Customer Representatives should not be able to perform manager-level transactions;
- A customer should not be allowed access to other customers' account information, or to
any employee information.

## 4 User Interface
Java Swing provides facilities for creating pop-up and pull-down menus, value lists, input/output
forms, labels and customized reports. You should make use of all of these capabilities, and in the
process come up with a system that caters to users with only limited computer knowledge.
We don’t care about having a beautiful UI. It only has to work!

What to turn in:
1. Relational database schema
2. Java program

IMPORTANT DATES
April 5: Group formation and ER diagram and Relational schema due date
May 5: Final project due date