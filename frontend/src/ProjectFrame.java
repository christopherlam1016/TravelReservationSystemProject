import java.sql.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    JComboBox<String> cbFrom, cbTo;
    JSpinner dateSpinner;
    JCheckBox chkAnyDate;
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

        JPanel dashboardTop = new JPanel(new BorderLayout(10, 10));
        dashboardTop.setOpaque(false);
        dashboardTop.add(dashboardHeader, BorderLayout.CENTER);
        dashboardTop.add(btnLogout, BorderLayout.EAST);

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

        SpinnerDateModel dateModel = new SpinnerDateModel();
        dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setFont(fieldFont);
        dateSpinner.setEnabled(false);

        chkAnyDate = new JCheckBox("Any Date");
        chkAnyDate.setSelected(true);
        chkAnyDate.setOpaque(false);
        chkAnyDate.setFont(fieldFont);
        chkAnyDate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dateSpinner.setEnabled(!chkAnyDate.isSelected());
            }
        });

        JPanel datePanel = new JPanel(new BorderLayout(4, 0));
        datePanel.setOpaque(false);
        datePanel.add(dateSpinner, BorderLayout.CENTER);
        datePanel.add(chkAnyDate, BorderLayout.EAST);

        JPanel searchPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createTitledBorder("Explore Flights"));
        JLabel lbFrom = new JLabel("From Airport");
        JLabel lbTo = new JLabel("To Airport");
        JLabel lbDate = new JLabel("Departure Date");
        JLabel lbBlank = new JLabel();
        JButton btnSearchFlights = new JButton("Search Flights");

        lbFrom.setFont(fieldFont);
        lbTo.setFont(fieldFont);
        lbDate.setFont(fieldFont);
        btnSearchFlights.setFont(new Font("Lucida Sans", Font.BOLD, 14));

        btnSearchFlights.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fromSel = cbFrom.getSelectedItem().toString();
                String toSel = cbTo.getSelectedItem().toString();
                String fromCode = fromSel.equals("Any") ? "" : fromSel.substring(0, 3);
                String toCode = toSel.equals("Any") ? "" : toSel.substring(0, 3);
                String dateStr = "";
                if (!chkAnyDate.isSelected()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    dateStr = sdf.format((Date) dateSpinner.getValue());
                }
                loadFlights(fromCode, toCode, dateStr);
            }
        });

        searchPanel.add(lbFrom);
        searchPanel.add(lbTo);
        searchPanel.add(lbDate);
        searchPanel.add(lbBlank);
        searchPanel.add(cbFrom);
        searchPanel.add(cbTo);
        searchPanel.add(datePanel);
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
            msg.setText("Unable to load flights: " + e.getMessage());
        }
    }

    private void showBookingForm(int rowIndex) {
        long flightId   = flightRowIds.get(rowIndex);
        int  flightNum  = (Integer) flightTableModel.getValueAt(rowIndex, 0);
        String airline  = (String)  flightTableModel.getValueAt(rowIndex, 1);
        String from     = (String)  flightTableModel.getValueAt(rowIndex, 2);
        String to       = (String)  flightTableModel.getValueAt(rowIndex, 3);
        String dep      = flightTableModel.getValueAt(rowIndex, 4).toString();

        JDialog dialog = new JDialog(this, "Book Flight", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(440, 300);
        dialog.setLocationRelativeTo(this);

        Font dlgFont = new Font("Lucida Sans", Font.PLAIN, 14);

        JLabel infoLabel = new JLabel("<html><b>Flight " + flightNum + "</b> · " + airline
                + "<br/>" + from + " → " + to + " · Dep: " + dep + "</html>");
        infoLabel.setFont(dlgFont);
        infoLabel.setBorder(BorderFactory.createTitledBorder("Selected Flight"));

        JComboBox<String> cbTicketType = new JComboBox<>(new String[]{"one_way", "round_trip"});
        JComboBox<String> cbClass      = new JComboBox<>(new String[]{"economy", "business", "first"});
        JTextField tfSeat = new JTextField();
        JTextField tfMeal = new JTextField();
        cbTicketType.setFont(dlgFont); cbClass.setFont(dlgFont);
        tfSeat.setFont(dlgFont);       tfMeal.setFont(dlgFont);

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 8, 6));
        formPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        for (String lbl : new String[]{"Ticket Type", "Class", "Seat Number", "Special Meal"}) {
            JLabel l = new JLabel(lbl); l.setFont(dlgFont); formPanel.add(l);
            if (lbl.equals("Ticket Type")) formPanel.add(cbTicketType);
            else if (lbl.equals("Class"))  formPanel.add(cbClass);
            else if (lbl.equals("Seat Number")) formPanel.add(tfSeat);
            else formPanel.add(tfMeal);
        }

        JButton btnConfirm = new JButton("Confirm Booking");
        JButton btnCancel  = new JButton("Cancel");
        btnConfirm.setFont(new Font("Lucida Sans", Font.BOLD, 14));
        btnCancel.setFont(dlgFont);
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        btnPanel.add(btnConfirm); btnPanel.add(btnCancel);

        btnCancel.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { dialog.dispose(); }
        });

        btnConfirm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String lookupSql = "SELECT AccountID, CustomerID FROM Account WHERE AccountID = ?";
                try (PreparedStatement ps = con.prepareStatement(lookupSql)) {
                    ps.setString(1, user);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(dialog,
                            "No customer profile found for '" + user + "'.\nContact an admin to set one up.",
                            "Profile Not Found", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String accountId  = rs.getString("AccountID");
                    int    customerId = rs.getInt("CustomerID");
                    String depDate    = dep.length() >= 10 ? dep.substring(0, 10) : dep;
                    String ticketId   = "TKT-" + System.currentTimeMillis();
                    long   ticketNum  = System.currentTimeMillis();
                    String seatNum    = tfSeat.getText().trim().isEmpty() ? null : tfSeat.getText().trim();
                    String meal       = tfMeal.getText().trim().isEmpty() ? null : tfMeal.getText().trim();

                    String insTicket = "INSERT INTO Ticket "
                        + "(TicketID, TicketNumber, TicketType, FlightClass, BookingFee, TotalFare, "
                        + "Flexibility, FromAirport, ToAirport, IsPaid, Status, CustomerID, AccountID) "
                        + "VALUES (?, ?, ?, ?, 0.00, 0.00, FALSE, ?, ?, FALSE, 'booked', ?, ?)";
                    try (PreparedStatement psT = con.prepareStatement(insTicket)) {
                        psT.setString(1, ticketId);
                        psT.setLong(2, ticketNum);
                        psT.setString(3, cbTicketType.getSelectedItem().toString());
                        psT.setString(4, cbClass.getSelectedItem().toString());
                        psT.setString(5, from);
                        psT.setString(6, to);
                        psT.setInt(7, customerId);
                        psT.setString(8, accountId);
                        psT.executeUpdate();
                    }

                    String insSeg = "INSERT INTO TicketSegment "
                        + "(TicketID, SegmentOrder, FlightID, DepartureDate, SeatNumber, SpecialMeal, SegmentFare) "
                        + "VALUES (?, 1, ?, ?, ?, ?, 0.00)";
                    try (PreparedStatement psS = con.prepareStatement(insSeg)) {
                        psS.setString(1, ticketId);
                        psS.setLong(2, flightId);
                        psS.setString(3, depDate);
                        psS.setString(4, seatNum);
                        psS.setString(5, meal);
                        psS.executeUpdate();
                    }

                    JOptionPane.showMessageDialog(dialog,
                        "Booking confirmed!\nTicket ID: " + ticketId,
                        "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog,
                        "Booking failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dialog.add(infoLabel,  BorderLayout.NORTH);
        dialog.add(formPanel,  BorderLayout.CENTER);
        dialog.add(btnPanel,   BorderLayout.SOUTH);
        dialog.setVisible(true);
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
            msg.setText("Unable to load bookings: " + e.getMessage());
        }
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
