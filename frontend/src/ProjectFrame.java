import java.sql.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ProjectFrame extends JFrame {
    // GUI related-variables:
    final private Font mainFont = new Font("Lucida Sans", Font.BOLD, 18);
    JTextField tfuser, tfpasswd;
    JLabel msg;
    JLabel loginStatus;
    JLabel dashboardHeader;
    JLabel customerInfoLabel;
    JLabel dashboardStatus;
    JLabel bookingFlightInfo;
    JComboBox<String> cbBookingTicketType;
    JComboBox<String> cbBookingClass;
    JComboBox<String> cbBookingSeat;
    JComboBox<String> cbBookingMeal;
    long bookingFlightId;
    String bookingFrom, bookingTo, bookingDepDate;
    JLabel confirmTicketId;
    JLabel confirmDetails;
    JComboBox<String> cbFrom, cbTo;
    JTextField tfDate;
    JTable flightTable;
    List<Long> flightRowIds = new ArrayList<>();
    DefaultTableModel flightTableModel;
    DefaultTableModel bookingsTableModel;

    CardLayout cardLayout;
    JPanel rootPanel;

    // Database related variables:
    public static Connection con = null;
    public static Statement stmt = null;
    public static boolean userLoggedin = false;
    public static String user = "";
    public static String userRole = "guest";

    public void initialize() throws Exception {
        loginStatus = new JLabel("Not logged in (Current Role: guest)");
        loginStatus.setFont(mainFont);
        dashboardHeader = new JLabel("Welcome", SwingConstants.LEFT);
        dashboardHeader.setFont(mainFont);
        customerInfoLabel = new JLabel();
        customerInfoLabel.setFont(new Font("Lucida Sans", Font.PLAIN, 15));

        // inputPanel: ------------------------------------
        // -- inputPanel components
        JLabel lbuser = new JLabel("Username");
        lbuser.setFont(mainFont);
        tfuser = new JTextField();
        tfuser.setFont(mainFont);

        JLabel lbpasswd = new JLabel("Password");
        lbpasswd.setFont(mainFont);
        tfpasswd = new JTextField();
        tfpasswd.setFont(mainFont);

        //-- create inputPanel and add its components
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 2, 5, 5));
        inputPanel.setOpaque(false); // so that form color is seen as background
        inputPanel.add(lbuser);
        inputPanel.add(tfuser);
        inputPanel.add(lbpasswd);
        inputPanel.add(tfpasswd);

        // msg : ------------------------------------
        msg = new JLabel(); // text will be added later (it is global variable)
        msg.setFont(mainFont);

        // buttonPanel: ------------------------------------
        // -- buttonPanel components
        JButton btnAdd = new JButton("Add User");
        btnAdd.setFont(mainFont);

        // add a listener
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // read the text of the text fields and store new user in the database
                String newUser = tfuser.getText().trim();
                String passwd = tfpasswd.getText().trim();

                if (newUser.isEmpty() || passwd.isEmpty()) {
                    msg.setText("Username and password are required.");
                    return;
                }

                // execute sql insert query for new user information
                String instruction = "INSERT INTO users (`user`, `password`, role) VALUES (?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(instruction)) {
                    ps.setString(1, newUser);
                    ps.setString(2, passwd);
                    ps.setString(3, "customer");
                    int result = ps.executeUpdate();
                    String s = "User " + newUser + " has been added (" + result + ")";
                    msg.setText(s);
                } catch (SQLException e1) {
                    msg.setText("Unable to add new user: " + e1.getMessage());
                }
            }
        });

        JButton btnLogin = new JButton("Login");
        btnLogin.setFont(mainFont);

        // add a listener
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // read the text of the text fields and check that they are in the database
                String loginUser = tfuser.getText().trim();
                String passwd = tfpasswd.getText().trim();

                if (loginUser.isEmpty() || passwd.isEmpty()) {
                    msg.setText("Enter both username and password.");
                    return;
                }

                String query = "SELECT `user`, role FROM users WHERE `user` = ? AND `password` = ?";
                try (PreparedStatement ps = con.prepareStatement(query)) {
                    ps.setString(1, loginUser);
                    ps.setString(2, passwd);
                    ResultSet rset = ps.executeQuery();
                    if (rset.next()) {
                        user = rset.getString("user");
                        String resolvedRole = "customer";
                        String fetchedRole = rset.getString("role");
                        if (fetchedRole != null && !fetchedRole.trim().isEmpty()) {
                            resolvedRole = fetchedRole.trim().toLowerCase();
                        }
                        userRole = resolvedRole;
                        String s = "Welcome " + user;
                        userLoggedin = true;
                        msg.setText(s);
                        loginStatus.setText("Logged in as: " + user + " (Role: " + userRole + ")");
                        updateDashboardInfo();
                        loadFlights("", "", "");
                        cardLayout.show(rootPanel, "dashboard");
                    } else {
                        String s = "Unknown user: invalid username or password";
                        msg.setText(s);
                        userLoggedin = false;
                        userRole = "guest";
                        loginStatus.setText("Not logged in (Current Role: guest)");
                    }
                } catch (SQLException e1) {
                    msg.setText("Login failed: " + e1.getMessage());
                }
            }
        });

        JButton btnClear = new JButton("Clear");
        btnClear.setFont(mainFont);

        // add a listener
        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // clear the text fields
                tfuser.setText("");
                tfpasswd.setText("");
                msg.setText("");
                userLoggedin = false;
                user = "";
                userRole = "guest";
                loginStatus.setText("Not logged in (Current Role: guest)");
            }
        });

        //-- create buttonPanel and add its components
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 3, 5, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnClear);

        // mainPanel: ------------------------------------
        //-------------- create main panel
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BorderLayout());
        loginPanel.setBackground(new Color(230, 140, 140));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // -- add mainPanel's components
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setOpaque(false);
        topPanel.add(loginStatus, BorderLayout.NORTH);
        topPanel.add(inputPanel, BorderLayout.CENTER);

        loginPanel.add(topPanel, BorderLayout.NORTH);
        loginPanel.add(msg, BorderLayout.CENTER);
        loginPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel dashboardPanel = buildDashboardPanel();

        cardLayout = new CardLayout();
        rootPanel = new JPanel(cardLayout);
        rootPanel.add(loginPanel, "login");
        rootPanel.add(dashboardPanel, "dashboard");
        rootPanel.add(buildBookingPanel(), "booking");
        rootPanel.add(buildConfirmationPanel(), "confirmation");

        // -- Add the mainPanel to our JForm and set up basic attributes
        this.add(rootPanel);
        this.setTitle("Login Page");
        this.setSize(950, 600);
        this.setMinimumSize(new Dimension(300, 200));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private JPanel buildDashboardPanel() {
        JPanel dashboardPanel = new JPanel(new BorderLayout(10, 10));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        dashboardPanel.setBackground(new Color(242, 240, 230));

        JButton btnLogout = new JButton("Logout");
        btnLogout.setFont(mainFont);
        btnLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userLoggedin = false;
                user = "";
                userRole = "guest";
                tfuser.setText("");
                tfpasswd.setText("");
                msg.setText("");
                loginStatus.setText("Not logged in (Current Role: guest)");
                cardLayout.show(rootPanel, "login");
            }
        });

        dashboardStatus = new JLabel(" ");
        dashboardStatus.setFont(new Font("Lucida Sans", Font.ITALIC, 13));
        dashboardStatus.setForeground(Color.RED);

        JPanel dashboardTop = new JPanel(new BorderLayout(10, 10));
        dashboardTop.setOpaque(false);
        dashboardTop.add(dashboardHeader, BorderLayout.CENTER);
        dashboardTop.add(btnLogout, BorderLayout.EAST);
        dashboardTop.add(dashboardStatus, BorderLayout.SOUTH);

        JPanel customerPanel = new JPanel(new BorderLayout());
        customerPanel.setOpaque(false);
        customerPanel.setBorder(BorderFactory.createTitledBorder("Customer Information"));
        customerPanel.add(customerInfoLabel, BorderLayout.CENTER);

        String[] airports = {
            "Any",
            "ATL - Hartsfield-Jackson Atlanta",
            "LAX - Los Angeles International",
            "ORD - O'Hare International (Chicago)",
            "DFW - Dallas/Fort Worth International",
            "DEN - Denver International",
            "JFK - John F. Kennedy International (New York)",
            "SFO - San Francisco International",
            "SEA - Seattle-Tacoma International",
            "LAS - Harry Reid International (Las Vegas)",
            "MCO - Orlando International",
            "EWR - Newark Liberty International",
            "MIA - Miami International",
            "PHX - Phoenix Sky Harbor International",
            "IAH - George Bush Intercontinental (Houston)",
            "BOS - Logan International (Boston)",
            "MSP - Minneapolis-Saint Paul International",
            "DTW - Detroit Metropolitan Wayne County",
            "PHL - Philadelphia International",
            "LGA - LaGuardia (New York)",
            "CLT - Charlotte Douglas International",
            "SLC - Salt Lake City International",
            "BWI - Baltimore/Washington International",
            "SAN - San Diego International",
            "MDW - Chicago Midway International",
            "TPA - Tampa International",
            "HNL - Daniel K. Inouye International (Honolulu)",
            "PDX - Portland International",
            "STL - St. Louis Lambert International",
            "BNA - Nashville International",
            "AUS - Austin-Bergstrom International",
            "MCI - Kansas City International",
            "RDU - Raleigh-Durham International",
            "FLL - Fort Lauderdale-Hollywood International",
            "OAK - Oakland International",
            "SMF - Sacramento International",
            "SJC - Norman Y. Mineta San Jose International",
            "ABQ - Albuquerque International Sunport",
            "MSY - Louis Armstrong New Orleans International",
            "JAX - Jacksonville International",
            "IND - Indianapolis International",
            "PIT - Pittsburgh International",
            "CMH - John Glenn Columbus International",
            "CLE - Cleveland Hopkins International",
            "MKE - General Mitchell International (Milwaukee)"
        };

        Font fieldFont = new Font("Lucida Sans", Font.PLAIN, 14);

        cbFrom = new JComboBox<>(airports);
        cbTo = new JComboBox<>(airports);
        cbFrom.setFont(fieldFont);
        cbTo.setFont(fieldFont);

        tfDate = new JTextField();
        tfDate.setFont(fieldFont);
        tfDate.setToolTipText("Enter date as yyyy-MM-dd, or leave blank for all dates");

        JPanel searchPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createTitledBorder("Explore Flights"));
        JLabel lbFrom = new JLabel("From Airport");
        JLabel lbTo = new JLabel("To Airport");
        JLabel lbDate = new JLabel("Date (yyyy-MM-dd or blank)");
        JLabel lbBlank = new JLabel();
        JButton btnSearchFlights = new JButton("Search Flights");

        lbFrom.setFont(fieldFont);
        lbTo.setFont(fieldFont);
        lbDate.setFont(new Font("Lucida Sans", Font.PLAIN, 12));
        btnSearchFlights.setFont(new Font("Lucida Sans", Font.BOLD, 14));

        btnSearchFlights.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dashboardStatus.setText(" ");
                String fromSel = cbFrom.getSelectedItem().toString();
                String toSel = cbTo.getSelectedItem().toString();
                String fromCode = fromSel.equals("Any") ? "" : fromSel.substring(0, 3);
                String toCode = toSel.equals("Any") ? "" : toSel.substring(0, 3);
                loadFlights(fromCode, toCode, tfDate.getText().trim());
            }
        });

        searchPanel.add(lbFrom);
        searchPanel.add(lbTo);
        searchPanel.add(lbDate);
        searchPanel.add(lbBlank);
        searchPanel.add(cbFrom);
        searchPanel.add(cbTo);
        searchPanel.add(tfDate);
        searchPanel.add(btnSearchFlights);

        flightTableModel = new DefaultTableModel(
                new String[] { "Flight #", "Airline", "From", "To", "Departure", "Arrival", "Type" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        flightTable = new JTable(flightTableModel);
        flightTable.setRowHeight(22);
        flightTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = flightTable.getSelectedRow();
                    if (row >= 0 && row < flightRowIds.size()) {
                        showBookingForm(row);
                    }
                }
            }
        });
        JScrollPane tableScrollPane = new JScrollPane(flightTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Matching Flights (double-click to book)"));

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        customerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        customerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        searchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        tableScrollPane.setPreferredSize(new Dimension(800, 260));
        tableScrollPane.setMinimumSize(new Dimension(350, 180));
        centerPanel.add(customerPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        centerPanel.add(searchPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        centerPanel.add(tableScrollPane);

        bookingsTableModel = new DefaultTableModel(
                new String[]{ "Ticket ID", "Type", "Class", "Status", "Seat",
                              "Dep. Date", "Flight #", "Airline", "From", "To" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable bookingsTable = new JTable(bookingsTableModel);
        bookingsTable.setRowHeight(22);
        JScrollPane bookingsScrollPane = new JScrollPane(bookingsTable);
        bookingsScrollPane.setBorder(BorderFactory.createTitledBorder("My Bookings"));
        bookingsScrollPane.setPreferredSize(new Dimension(800, 180));
        bookingsScrollPane.setMinimumSize(new Dimension(350, 120));

        JButton btnLoadBookings = new JButton("Load My Bookings");
        btnLoadBookings.setFont(new Font("Lucida Sans", Font.BOLD, 14));
        btnLoadBookings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadMyBookings();
            }
        });

        JPanel bookingsPanel = new JPanel();
        bookingsPanel.setOpaque(false);
        bookingsPanel.setLayout(new BoxLayout(bookingsPanel, BoxLayout.Y_AXIS));
        bookingsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLoadBookings.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookingsScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookingsPanel.add(btnLoadBookings);
        bookingsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        bookingsPanel.add(bookingsScrollPane);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        centerPanel.add(bookingsPanel);

        JScrollPane dashboardScrollPane = new JScrollPane(centerPanel);
        dashboardScrollPane.setBorder(BorderFactory.createEmptyBorder());
        dashboardScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        dashboardScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        dashboardPanel.add(dashboardTop, BorderLayout.NORTH);
        dashboardPanel.add(dashboardScrollPane, BorderLayout.CENTER);
        return dashboardPanel;
    }

    private void updateDashboardInfo() {
        dashboardHeader.setText("Logged in as " + user + " (Role: " + userRole + ")");
        customerInfoLabel.setText(
                "<html><b>Username:</b> " + user + "<br/><b>Role:</b> " + userRole
                        + "<br/><b>Reservation portfolio:</b> Coming next (past/upcoming trips).</html>");
    }

    private void loadFlights(String fromAirport, String toAirport, String flightDate) {
        if (flightTableModel == null) {
            return;
        }

        flightTableModel.setRowCount(0);
        flightRowIds.clear();
        String sql = "SELECT FlightID, FlightNumber, Airline_Name, DepartureAirport, ArrivalAirport, "
                + "DepartureTime, ArrivalTime, Travel_Type FROM Flight WHERE 1=1";

        boolean filterFrom = fromAirport != null && !fromAirport.trim().isEmpty();
        boolean filterTo = toAirport != null && !toAirport.trim().isEmpty();
        boolean filterDate = flightDate != null && !flightDate.trim().isEmpty();

        if (filterFrom) {
            sql += " AND DepartureAirport = ?";
        }
        if (filterTo) {
            sql += " AND ArrivalAirport = ?";
        }
        if (filterDate) {
            sql += " AND DATE(DepartureTime) = ?";
        }
        sql += " ORDER BY DepartureTime LIMIT 100";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            if (filterFrom) {
                ps.setString(idx++, fromAirport.trim());
            }
            if (filterTo) {
                ps.setString(idx++, toAirport.trim());
            }
            if (filterDate) {
                ps.setString(idx++, flightDate.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    flightRowIds.add(rs.getLong("FlightID"));
                    flightTableModel.addRow(new Object[] {
                            rs.getInt("FlightNumber"),
                            rs.getString("Airline_Name"),
                            rs.getString("DepartureAirport"),
                            rs.getString("ArrivalAirport"),
                            rs.getTimestamp("DepartureTime"),
                            rs.getTimestamp("ArrivalTime"),
                            rs.getString("Travel_Type")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            dashboardStatus.setText("Error loading flights: " + e.getMessage());
        }
    }

    private void showBookingForm(int rowIndex) {
        bookingFlightId = flightRowIds.get(rowIndex);
        int    flightNum = (Integer) flightTableModel.getValueAt(rowIndex, 0);
        String airline   = (String)  flightTableModel.getValueAt(rowIndex, 1);
        bookingFrom      = (String)  flightTableModel.getValueAt(rowIndex, 2);
        bookingTo        = (String)  flightTableModel.getValueAt(rowIndex, 3);
        String dep       = flightTableModel.getValueAt(rowIndex, 4).toString();
        bookingDepDate   = dep.length() >= 10 ? dep.substring(0, 10) : dep;

        bookingFlightInfo.setText("<html><b>Flight " + flightNum + "</b> &nbsp;&middot;&nbsp; "
                + airline + "<br/>" + bookingFrom + " &rarr; " + bookingTo
                + " &nbsp;&middot;&nbsp; Departure: " + dep + "</html>");

        cbBookingTicketType.setSelectedIndex(0);
        cbBookingClass.setSelectedIndex(0);
        cbBookingMeal.setSelectedIndex(0);
        loadSeatOptions(bookingFlightId);
        cardLayout.show(rootPanel, "booking");
    }

    private void loadMyBookings() {
        if (bookingsTableModel == null) return;
        bookingsTableModel.setRowCount(0);

        String sql = "SELECT t.TicketID, t.TicketType, t.FlightClass, t.Status, "
                   + "ts.SeatNumber, ts.DepartureDate, "
                   + "f.FlightNumber, f.Airline_Name, f.DepartureAirport, f.ArrivalAirport "
                   + "FROM Ticket t "
                   + "JOIN TicketSegment ts ON t.TicketID = ts.TicketID "
                   + "JOIN Flight f ON ts.FlightID = f.FlightID "
                   + "WHERE t.AccountID = ? "
                   + "ORDER BY ts.DepartureDate DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookingsTableModel.addRow(new Object[]{
                        rs.getString("TicketID"),
                        rs.getString("TicketType"),
                        rs.getString("FlightClass"),
                        rs.getString("Status"),
                        rs.getString("SeatNumber"),
                        rs.getDate("DepartureDate"),
                        rs.getInt("FlightNumber"),
                        rs.getString("Airline_Name"),
                        rs.getString("DepartureAirport"),
                        rs.getString("ArrivalAirport")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            dashboardStatus.setText("Error loading bookings: " + e.getMessage());
        }
    }

    private JPanel buildBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(242, 240, 230));

        Font fieldFont = new Font("Lucida Sans", Font.PLAIN, 14);

        JButton btnBack = new JButton("← Back to Flights");
        btnBack.setFont(fieldFont);
        btnBack.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                cardLayout.show(rootPanel, "dashboard");
            }
        });

        JLabel title = new JLabel("Book a Flight");
        title.setFont(new Font("Lucida Sans", Font.BOLD, 20));

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);
        topPanel.add(btnBack, BorderLayout.WEST);
        topPanel.add(title, BorderLayout.CENTER);

        bookingFlightInfo = new JLabel(" ");
        bookingFlightInfo.setFont(fieldFont);
        bookingFlightInfo.setBorder(BorderFactory.createTitledBorder("Selected Flight"));

        cbBookingTicketType = new JComboBox<>(new String[]{"one_way", "round_trip"});
        cbBookingClass      = new JComboBox<>(new String[]{"economy", "business", "first"});
        cbBookingSeat       = new JComboBox<>();
        cbBookingMeal       = new JComboBox<>(new String[]{
            "None", "Vegetarian", "Vegan", "Kosher",
            "Halal", "Gluten-Free", "Low-Sodium", "Child Meal"
        });
        cbBookingTicketType.setFont(fieldFont);
        cbBookingClass.setFont(fieldFont);
        cbBookingSeat.setFont(fieldFont);
        cbBookingMeal.setFont(fieldFont);

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 12));
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder("Booking Details"));
        String[] labels = {"Ticket Type", "Class", "Seat Number", "Special Meal"};
        JComponent[] fields = {cbBookingTicketType, cbBookingClass, cbBookingSeat, cbBookingMeal};
        for (int i = 0; i < labels.length; i++) {
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(fieldFont);
            formPanel.add(lbl);
            formPanel.add(fields[i]);
        }

        JButton btnConfirm = new JButton("Confirm Booking");
        btnConfirm.setFont(new Font("Lucida Sans", Font.BOLD, 16));
        btnConfirm.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { confirmBooking(); }
        });

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        bookingFlightInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnConfirm.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(bookingFlightInfo);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        centerPanel.add(formPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 16)));
        centerPanel.add(btnConfirm);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private void loadSeatOptions(long flightId) {
        cbBookingSeat.removeAllItems();
        cbBookingSeat.addItem("No Preference");
        String sql = "SELECT ac.SeatCapacity FROM Aircraft ac "
                   + "JOIN Flight f ON ac.AircraftID = f.AircraftID WHERE f.FlightID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, flightId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int capacity = rs.getInt("SeatCapacity");
                int rows = (int) Math.ceil(capacity / 6.0);
                String[] letters = {"A", "B", "C", "D", "E", "F"};
                int count = 0;
                outer:
                for (int r = 1; r <= rows; r++) {
                    for (String l : letters) {
                        cbBookingSeat.addItem(r + l);
                        if (++count >= capacity) break outer;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            dashboardStatus.setText("Could not load seat options: " + e.getMessage());
        }
    }

    private void confirmBooking() {
        String lookupSql = "SELECT AccountID, CustomerID FROM Account WHERE AccountID = ?";
        try (PreparedStatement ps = con.prepareStatement(lookupSql)) {
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this,
                    "No customer profile found for '" + user + "'.\nContact an admin.",
                    "Profile Not Found", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String accountId  = rs.getString("AccountID");
            int    customerId = rs.getInt("CustomerID");
            String ticketId   = "TKT-" + System.currentTimeMillis();
            long   ticketNum  = System.currentTimeMillis();
            String seatSel    = cbBookingSeat.getSelectedItem().toString();
            String seatNum    = seatSel.equals("No Preference") ? null : seatSel;
            String mealSel    = cbBookingMeal.getSelectedItem().toString();
            String meal       = mealSel.equals("None") ? null : mealSel;

            String insTicket = "INSERT INTO Ticket "
                + "(TicketID, TicketNumber, TicketType, FlightClass, BookingFee, TotalFare, "
                + "Flexibility, FromAirport, ToAirport, IsPaid, Status, CustomerID, AccountID) "
                + "VALUES (?, ?, ?, ?, 0.00, 0.00, FALSE, ?, ?, FALSE, 'booked', ?, ?)";
            try (PreparedStatement psT = con.prepareStatement(insTicket)) {
                psT.setString(1, ticketId);
                psT.setLong(2, ticketNum);
                psT.setString(3, cbBookingTicketType.getSelectedItem().toString());
                psT.setString(4, cbBookingClass.getSelectedItem().toString());
                psT.setString(5, bookingFrom);
                psT.setString(6, bookingTo);
                psT.setInt(7, customerId);
                psT.setString(8, accountId);
                psT.executeUpdate();
            }

            String insSeg = "INSERT INTO TicketSegment "
                + "(TicketID, SegmentOrder, FlightID, DepartureDate, SeatNumber, SpecialMeal, SegmentFare) "
                + "VALUES (?, 1, ?, ?, ?, ?, 0.00)";
            try (PreparedStatement psS = con.prepareStatement(insSeg)) {
                psS.setString(1, ticketId);
                psS.setLong(2, bookingFlightId);
                psS.setString(3, bookingDepDate);
                psS.setString(4, seatNum);
                psS.setString(5, meal);
                psS.executeUpdate();
            }

            confirmTicketId.setText("Ticket ID: " + ticketId);
            String seatDisplay = (seatNum != null) ? seatNum : "No Preference";
            String mealDisplay = (meal  != null) ? meal  : "None";
            confirmDetails.setText("<html>"
                + "<b>Flight:</b> " + bookingFrom + " &rarr; " + bookingTo + " &nbsp;|&nbsp; " + bookingDepDate + "<br/>"
                + "<b>Class:</b> " + cbBookingClass.getSelectedItem() + "<br/>"
                + "<b>Seat:</b> " + seatDisplay + "<br/>"
                + "<b>Special Meal:</b> " + mealDisplay
                + "</html>");
            cardLayout.show(rootPanel, "confirmation");
        } catch (SQLException ex) {
            ex.printStackTrace();
            if (ex.getErrorCode() == 1062) {
                JOptionPane.showMessageDialog(this,
                    "Seat " + cbBookingSeat.getSelectedItem() + " on this flight is already taken.\nPlease choose a different seat.",
                    "Seat Unavailable", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Booking failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel buildConfirmationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        panel.setBackground(new Color(242, 240, 230));

        Font fieldFont = new Font("Lucida Sans", Font.PLAIN, 15);

        JLabel title = new JLabel("Booking Confirmed!");
        title.setFont(new Font("Lucida Sans", Font.BOLD, 24));
        title.setForeground(new Color(0, 140, 0));

        confirmTicketId = new JLabel(" ");
        confirmTicketId.setFont(new Font("Lucida Sans", Font.BOLD, 16));
        confirmTicketId.setBorder(BorderFactory.createTitledBorder("Ticket ID"));

        confirmDetails = new JLabel(" ");
        confirmDetails.setFont(fieldFont);
        confirmDetails.setBorder(BorderFactory.createTitledBorder("Booking Details"));

        JButton btnFlights = new JButton("Search More Flights");
        JButton btnBookings = new JButton("View My Bookings");
        btnFlights.setFont(new Font("Lucida Sans", Font.BOLD, 14));
        btnBookings.setFont(new Font("Lucida Sans", Font.PLAIN, 14));

        btnFlights.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                cardLayout.show(rootPanel, "dashboard");
            }
        });
        btnBookings.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                loadMyBookings();
                cardLayout.show(rootPanel, "dashboard");
            }
        });

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnFlights);
        btnRow.add(btnBookings);

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmTicketId.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmDetails.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(title);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        centerPanel.add(confirmTicketId);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        centerPanel.add(confirmDetails);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 24)));
        centerPanel.add(btnRow);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    public static void main(String[] args) throws Exception {
        // Initialize the connection to the database
        String url = "jdbc:mysql://localhost:3306/reservation_system";
        String user = "test";
        String password = "group16";

        try {
            con = DriverManager.getConnection(url, user, password);
            stmt = con.createStatement();
        } catch (SQLException e) {
            System.out.println("Unable to create a connection to the database");
            e.printStackTrace();
            System.exit(0);
        }

        ProjectFrame myFrame = new ProjectFrame();
        myFrame.initialize();
    }
}
