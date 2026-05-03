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
    JPanel bookingsCardsPanel;
    JPanel waitlistCardsPanel;
    JPanel repSection;
    JPanel adminSection;
    JPanel lookupCardsPanel;
    JComboBox<String> cbAccountLookup;
    boolean suppressLookup = false;
    DefaultTableModel usersTableModel;
    DefaultTableModel allBookingsTableModel;
    int loggedInCustomerId = -1;

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
                        try (PreparedStatement psAcc = con.prepareStatement(
                                "SELECT CustomerID FROM Account WHERE AccountID = ?")) {
                            psAcc.setString(1, user);
                            ResultSet rsAcc = psAcc.executeQuery();
                            loggedInCustomerId = rsAcc.next() ? rsAcc.getInt("CustomerID") : -1;
                        } catch (SQLException ignored) { loggedInCustomerId = -1; }
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
                loggedInCustomerId = -1;
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
        rootPanel.add(buildMyBookingsPanel(), "myBookings");

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
                loggedInCustomerId = -1;
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

        String[] airports = airportList();

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
                new String[] { "Flight #", "Airline", "From", "To", "Departure", "Arrival", "Type", "Operates" }, 0) {
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

        JButton btnViewBookings = new JButton("View My Bookings");
        btnViewBookings.setFont(new Font("Lucida Sans", Font.BOLD, 14));
        btnViewBookings.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnViewBookings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadMyBookings();
                cardLayout.show(rootPanel, "myBookings");
            }
        });

        centerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        centerPanel.add(btnViewBookings);

        // ── Rep section: Customer Lookup ──────────────────────────────────────
        repSection = new JPanel();
        repSection.setOpaque(false);
        repSection.setLayout(new BoxLayout(repSection, BoxLayout.Y_AXIS));
        repSection.setBorder(BorderFactory.createTitledBorder("Customer Lookup  (Rep & Admin)"));
        repSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        repSection.setVisible(false);

        JPanel lookupRow = new JPanel(new BorderLayout(8, 0));
        lookupRow.setOpaque(false);
        lookupRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        lookupRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbLookup = new JLabel("Customer: ");
        lbLookup.setFont(fieldFont);
        cbAccountLookup = new JComboBox<>();
        cbAccountLookup.setFont(fieldFont);
        cbAccountLookup.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (suppressLookup) return;
                Object sel = cbAccountLookup.getSelectedItem();
                if (sel != null && !sel.toString().isEmpty()) {
                    String accountId = sel.toString().split(" — ")[0].trim();
                    loadCustomerLookup(accountId);
                }
            }
        });
        JButton btnRefreshDropdown = new JButton("↻ Refresh List");
        btnRefreshDropdown.setFont(fieldFont);
        btnRefreshDropdown.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                refreshAccountDropdown();
            }
        });
        lookupRow.add(lbLookup, BorderLayout.WEST);
        lookupRow.add(cbAccountLookup, BorderLayout.CENTER);
        lookupRow.add(btnRefreshDropdown, BorderLayout.EAST);

        lookupCardsPanel = new JPanel();
        lookupCardsPanel.setOpaque(false);
        lookupCardsPanel.setLayout(new BoxLayout(lookupCardsPanel, BoxLayout.Y_AXIS));

        JScrollPane lookupScroll = new JScrollPane(lookupCardsPanel);
        lookupScroll.setPreferredSize(new Dimension(800, 200));
        lookupScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        lookupScroll.setBorder(BorderFactory.createEmptyBorder());

        repSection.add(lookupRow);
        repSection.add(Box.createRigidArea(new Dimension(0, 6)));
        repSection.add(lookupScroll);

        // ── Admin section: User Management + All Bookings ─────────────────────
        adminSection = new JPanel();
        adminSection.setOpaque(false);
        adminSection.setLayout(new BoxLayout(adminSection, BoxLayout.Y_AXIS));
        adminSection.setBorder(BorderFactory.createTitledBorder("Administration  (Admin Only)"));
        adminSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        adminSection.setVisible(false);

        usersTableModel = new DefaultTableModel(
                new String[]{"Username", "Role", "Created At"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable usersTable = new JTable(usersTableModel);
        usersTable.setRowHeight(22);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane usersScroll = new JScrollPane(usersTable);
        usersScroll.setBorder(BorderFactory.createTitledBorder("Users"));
        usersScroll.setPreferredSize(new Dimension(800, 130));
        usersScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField tfNewUser = new JTextField();
        JPasswordField tfNewPass = new JPasswordField();
        JComboBox<String> cbNewRole = new JComboBox<>(new String[]{"customer", "rep", "admin"});
        JButton btnAddUserAdmin  = new JButton("Add User");
        JButton btnDelUser      = new JButton("Delete Selected");
        JButton btnEditRole     = new JButton("Edit Role");
        JButton btnRefreshUsers = new JButton("Refresh");
        tfNewUser.setFont(fieldFont);
        tfNewPass.setFont(fieldFont);
        cbNewRole.setFont(fieldFont);
        btnAddUserAdmin.setFont(fieldFont);
        btnDelUser.setFont(fieldFont);
        btnEditRole.setFont(fieldFont);
        btnRefreshUsers.setFont(fieldFont);

        JPanel userCtrlRow = new JPanel(new GridLayout(2, 4, 8, 4));
        userCtrlRow.setOpaque(false);
        userCtrlRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        userCtrlRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbNu = new JLabel("New Username"); lbNu.setFont(fieldFont);
        JLabel lbNp = new JLabel("Password");     lbNp.setFont(fieldFont);
        JLabel lbNr = new JLabel("Role");         lbNr.setFont(fieldFont);
        userCtrlRow.add(lbNu);  userCtrlRow.add(lbNp);  userCtrlRow.add(lbNr);
        userCtrlRow.add(btnRefreshUsers);
        userCtrlRow.add(tfNewUser); userCtrlRow.add(tfNewPass);
        userCtrlRow.add(cbNewRole); userCtrlRow.add(btnAddUserAdmin);

        btnAddUserAdmin.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String newU = tfNewUser.getText().trim();
                String newP = new String(tfNewPass.getPassword()).trim();
                String newR = cbNewRole.getSelectedItem().toString();
                if (newU.isEmpty() || newP.isEmpty()) {
                    JOptionPane.showMessageDialog(ProjectFrame.this, "Username and password required.");
                    return;
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO users (`user`, `password`, role) VALUES (?, ?, ?)")) {
                    ps.setString(1, newU); ps.setString(2, newP); ps.setString(3, newR);
                    ps.executeUpdate();
                    tfNewUser.setText(""); tfNewPass.setText("");
                    loadAllUsers();
                } catch (SQLException ex) {
                    if (ex.getErrorCode() == 1062) {
                        JOptionPane.showMessageDialog(ProjectFrame.this,
                                "Username '" + newU + "' already exists.");
                    } else {
                        JOptionPane.showMessageDialog(ProjectFrame.this, "Error: " + ex.getMessage());
                    }
                }
            }
        });

        btnDelUser.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                int row = usersTable.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(ProjectFrame.this, "Select a user row first.");
                    return;
                }
                String target = usersTableModel.getValueAt(row, 0).toString();
                if (target.equals(user)) {
                    JOptionPane.showMessageDialog(ProjectFrame.this,
                            "You cannot delete your own account.");
                    return;
                }
                int choice = JOptionPane.showConfirmDialog(ProjectFrame.this,
                        "Delete user '" + target + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "DELETE FROM users WHERE `user` = ?")) {
                        ps.setString(1, target);
                        ps.executeUpdate();
                        loadAllUsers();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(ProjectFrame.this,
                                "Delete failed: " + ex.getMessage());
                    }
                }
            }
        });

        btnRefreshUsers.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { loadAllUsers(); }
        });

        btnEditRole.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                int row = usersTable.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(ProjectFrame.this, "Select a user row first.");
                    return;
                }
                String targetUser  = usersTableModel.getValueAt(row, 0).toString();
                String currentRole = usersTableModel.getValueAt(row, 1).toString();

                JComboBox<String> roleBox = new JComboBox<>(new String[]{"customer", "rep", "admin"});
                roleBox.setSelectedItem(currentRole);
                roleBox.setFont(fieldFont);

                int result = JOptionPane.showConfirmDialog(ProjectFrame.this, roleBox,
                        "Change role for \"" + targetUser + "\"",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;

                String newRole = roleBox.getSelectedItem().toString();
                if (newRole.equals(currentRole)) return;

                // Block demoting the last admin
                if ("admin".equals(currentRole) && !"admin".equals(newRole)) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT COUNT(*) FROM users WHERE role = 'admin'")) {
                        ResultSet rs = ps.executeQuery();
                        rs.next();
                        if (rs.getInt(1) <= 1) {
                            JOptionPane.showMessageDialog(ProjectFrame.this,
                                    "Cannot demote the last admin account.\n"
                                    + "Promote another user to admin first.");
                            return;
                        }
                    } catch (SQLException ex) { ex.printStackTrace(); return; }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE users SET role = ? WHERE `user` = ?")) {
                    ps.setString(1, newRole);
                    ps.setString(2, targetUser);
                    ps.executeUpdate();
                    loadAllUsers();
                    if (targetUser.equals(user)) {
                        userRole = newRole;
                        updateDashboardInfo();
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(ProjectFrame.this,
                            "Update failed: " + ex.getMessage());
                }
            }
        });

        JPanel delBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        delBtnRow.setOpaque(false);
        delBtnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        delBtnRow.add(btnDelUser);
        delBtnRow.add(btnEditRole);

        allBookingsTableModel = new DefaultTableModel(
                new String[]{"Account", "Customer", "Route", "Flight #", "Class", "Status", "Dep. Date"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable allBookingsTable = new JTable(allBookingsTableModel);
        allBookingsTable.setRowHeight(22);
        JScrollPane allBookingsScroll = new JScrollPane(allBookingsTable);
        allBookingsScroll.setBorder(BorderFactory.createTitledBorder("All Bookings"));
        allBookingsScroll.setPreferredSize(new Dimension(800, 160));
        allBookingsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnLoadAllBookings = new JButton("Load All Bookings");
        btnLoadAllBookings.setFont(fieldFont);
        btnLoadAllBookings.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLoadAllBookings.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { loadAllBookings(); }
        });

        adminSection.add(usersScroll);
        adminSection.add(Box.createRigidArea(new Dimension(0, 4)));
        adminSection.add(userCtrlRow);
        adminSection.add(Box.createRigidArea(new Dimension(0, 2)));
        adminSection.add(delBtnRow);
        adminSection.add(Box.createRigidArea(new Dimension(0, 10)));
        adminSection.add(btnLoadAllBookings);
        adminSection.add(Box.createRigidArea(new Dimension(0, 4)));
        adminSection.add(allBookingsScroll);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        centerPanel.add(repSection);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        centerPanel.add(adminSection);

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
        customerInfoLabel.setText("<html><b>Username:</b> " + user
                + "<br/><b>Role:</b> " + userRole + "</html>");
        boolean isRep   = "rep".equals(userRole) || "admin".equals(userRole);
        boolean isAdmin = "admin".equals(userRole);
        if (repSection   != null) repSection.setVisible(isRep);
        if (adminSection != null) adminSection.setVisible(isAdmin);
        if (isRep)   refreshAccountDropdown();
        if (isAdmin) loadAllUsers();
    }

    private void loadFlights(String fromAirport, String toAirport, String flightDate) {
        if (flightTableModel == null) {
            return;
        }

        flightTableModel.setRowCount(0);
        flightRowIds.clear();
        String sql = "SELECT f.FlightID, f.FlightNumber, f.Airline_Name, f.DepartureAirport, "
                + "f.ArrivalAirport, f.DepartureTime, f.ArrivalTime, f.Travel_Type, "
                + "GROUP_CONCAT(fod.DayOfWeek ORDER BY "
                + "FIELD(fod.DayOfWeek,'Mon','Tue','Wed','Thu','Fri','Sat','Sun') "
                + "SEPARATOR '/') AS OperatingDays "
                + "FROM Flight f "
                + "LEFT JOIN FlightOperatingDay fod ON f.FlightID = fod.FlightID "
                + "WHERE 1=1";

        boolean filterFrom = fromAirport != null && !fromAirport.trim().isEmpty();
        boolean filterTo = toAirport != null && !toAirport.trim().isEmpty();
        boolean filterDate = flightDate != null && !flightDate.trim().isEmpty();

        if (filterFrom) {
            sql += " AND f.DepartureAirport = ?";
        }
        if (filterTo) {
            sql += " AND f.ArrivalAirport = ?";
        }
        if (filterDate) {
            sql += " AND DATE(f.DepartureTime) = ?";
        }
        sql += " GROUP BY f.FlightID, f.FlightNumber, f.Airline_Name, f.DepartureAirport, "
             + "f.ArrivalAirport, f.DepartureTime, f.ArrivalTime, f.Travel_Type "
             + "ORDER BY f.DepartureTime LIMIT 100";

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
                    String days = rs.getString("OperatingDays");
                    flightTableModel.addRow(new Object[] {
                            rs.getInt("FlightNumber"),
                            rs.getString("Airline_Name"),
                            rs.getString("DepartureAirport"),
                            rs.getString("ArrivalAirport"),
                            rs.getTimestamp("DepartureTime"),
                            rs.getTimestamp("ArrivalTime"),
                            rs.getString("Travel_Type"),
                            days != null ? days : "—"
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

        if (isFlightFull(bookingFlightId)) {
            int pos = getWaitlistPosition(bookingFlightId);
            if (pos > 0) {
                JOptionPane.showMessageDialog(this,
                        "Flight " + flightNum + " (" + bookingFrom + " → " + bookingTo + ") is full.\n"
                        + "You are already on the waitlist — position " + pos + ".\n"
                        + "You'll be automatically booked when a seat opens.",
                        "Already Waitlisted", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Flight " + flightNum + " (" + bookingFrom + " → " + bookingTo + ") is full.\n"
                        + "Would you like to join the waitlist?\n"
                        + "You'll be automatically booked when a seat opens.",
                        "Flight Full — Join Waitlist?", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    joinWaitlist(bookingFlightId);
                }
            }
            return;
        }

        cbBookingTicketType.setSelectedIndex(0);
        cbBookingClass.setSelectedIndex(0);
        cbBookingMeal.setSelectedIndex(0);
        loadSeatOptions(bookingFlightId);
        cardLayout.show(rootPanel, "booking");
    }

    private void loadMyBookings() {
        if (bookingsCardsPanel == null) return;
        bookingsCardsPanel.removeAll();

        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Ticket t "
                + "JOIN TicketSegment ts ON t.TicketID = ts.TicketID "
                + "SET t.Status = 'completed' "
                + "WHERE t.AccountID = ? "
                + "AND t.Status IN ('booked', 'changed') "
                + "AND ts.DepartureDate < CURDATE()")) {
            ps.setString(1, user);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sql = "SELECT t.TicketID, t.TicketType, t.FlightClass, t.Status, "
                   + "ts.SeatNumber, ts.DepartureDate, ts.SpecialMeal, ts.SegmentOrder, "
                   + "f.FlightNumber, f.Airline_Name, f.DepartureAirport, f.ArrivalAirport "
                   + "FROM Ticket t "
                   + "JOIN TicketSegment ts ON t.TicketID = ts.TicketID "
                   + "JOIN Flight f ON ts.FlightID = f.FlightID "
                   + "WHERE t.AccountID = ? "
                   + "ORDER BY ts.DepartureDate DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                boolean hasResults = false;
                while (rs.next()) {
                    hasResults = true;
                    String depDate = rs.getDate("DepartureDate") != null
                            ? rs.getDate("DepartureDate").toString() : "";
                    JPanel card = createBookingCard(
                            rs.getString("TicketID"),
                            rs.getString("TicketType"),
                            rs.getString("FlightClass"),
                            rs.getString("Status"),
                            rs.getString("SeatNumber"),
                            depDate,
                            rs.getString("SpecialMeal"),
                            rs.getInt("FlightNumber"),
                            rs.getString("Airline_Name"),
                            rs.getString("DepartureAirport"),
                            rs.getString("ArrivalAirport"),
                            rs.getInt("SegmentOrder"));
                    card.setAlignmentX(Component.LEFT_ALIGNMENT);
                    bookingsCardsPanel.add(card);
                    bookingsCardsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                }
                if (!hasResults) {
                    JLabel noBookings = new JLabel("No bookings found for account: " + user);
                    noBookings.setFont(new Font("Lucida Sans", Font.ITALIC, 15));
                    noBookings.setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 0));
                    bookingsCardsPanel.add(noBookings);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JLabel errLabel = new JLabel("Error loading bookings: " + e.getMessage());
            errLabel.setFont(new Font("Lucida Sans", Font.PLAIN, 13));
            errLabel.setForeground(Color.RED);
            bookingsCardsPanel.add(errLabel);
        }

        bookingsCardsPanel.revalidate();
        bookingsCardsPanel.repaint();
        loadWaitlistEntries();
    }

    private JPanel createBookingCard(String ticketId, String ticketType, String flightClass,
            String status, String seat, String depDate, String meal,
            int flightNum, String airline, String from, String to, int segmentOrder) {

        JPanel card = new JPanel(new BorderLayout(12, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getStatusColor(status), 4),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel statusBadge = new JPanel(new BorderLayout());
        statusBadge.setPreferredSize(new Dimension(90, 0));
        statusBadge.setBackground(getStatusColor(status));
        JLabel statusLabel = new JLabel(status != null ? status.toUpperCase() : "UNKNOWN",
                SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Lucida Sans", Font.BOLD, 11));
        statusBadge.add(statusLabel, BorderLayout.CENTER);

        JPanel centerInfo = new JPanel();
        centerInfo.setOpaque(false);
        centerInfo.setLayout(new BoxLayout(centerInfo, BoxLayout.Y_AXIS));

        JLabel route = new JLabel(from + "  →  " + to);
        route.setFont(new Font("Lucida Sans", Font.BOLD, 18));

        JLabel airlineInfo = new JLabel(airline + "  ·  Flight " + flightNum
                + "  ·  " + capitalize(flightClass));
        airlineInfo.setFont(new Font("Lucida Sans", Font.PLAIN, 14));
        airlineInfo.setForeground(new Color(60, 60, 60));

        String seatDisplay = (seat != null && !seat.isEmpty()) ? seat : "No Preference";
        String mealDisplay = (meal != null && !meal.isEmpty()) ? meal : "None";
        JLabel details = new JLabel(depDate + "  ·  Seat: " + seatDisplay
                + "  ·  Meal: " + mealDisplay);
        details.setFont(new Font("Lucida Sans", Font.PLAIN, 13));
        details.setForeground(new Color(80, 80, 80));

        JLabel tidLabel = new JLabel("Ticket: " + ticketId
                + "   Type: " + (ticketType != null ? ticketType.replace("_", " ") : ""));
        tidLabel.setFont(new Font("Lucida Sans", Font.ITALIC, 11));
        tidLabel.setForeground(new Color(140, 140, 140));

        centerInfo.add(route);
        centerInfo.add(Box.createRigidArea(new Dimension(0, 4)));
        centerInfo.add(airlineInfo);
        centerInfo.add(Box.createRigidArea(new Dimension(0, 3)));
        centerInfo.add(details);
        centerInfo.add(Box.createRigidArea(new Dimension(0, 3)));
        centerInfo.add(tidLabel);

        boolean isActive = "booked".equalsIgnoreCase(status) || "changed".equalsIgnoreCase(status);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setFont(new Font("Lucida Sans", Font.PLAIN, 12));
        btnCancel.setEnabled(isActive);
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int choice = JOptionPane.showConfirmDialog(ProjectFrame.this,
                        "Cancel booking " + ticketId + "?\nThis cannot be undone.",
                        "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    cancelBooking(ticketId);
                }
            }
        });

        JButton btnChange = new JButton("Change Flight");
        btnChange.setFont(new Font("Lucida Sans", Font.PLAIN, 12));
        btnChange.setEnabled(isActive);
        btnChange.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showChangeFlightDialog(ticketId, segmentOrder);
            }
        });

        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        btnCancel.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnChange.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionPanel.add(Box.createVerticalGlue());
        actionPanel.add(btnChange);
        actionPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        actionPanel.add(btnCancel);
        actionPanel.add(Box.createVerticalGlue());

        card.add(statusBadge, BorderLayout.WEST);
        card.add(centerInfo, BorderLayout.CENTER);
        card.add(actionPanel, BorderLayout.EAST);
        return card;
    }

    private Color getStatusColor(String status) {
        if (status == null) return Color.GRAY;
        switch (status.toLowerCase()) {
            case "booked":    return new Color(0, 120, 200);
            case "completed": return new Color(0, 160, 0);
            case "cancelled": return new Color(200, 0, 0);
            case "changed":   return new Color(200, 120, 0);
            default:          return Color.GRAY;
        }
    }

    private void cancelBooking(String ticketId) {
        try {
            long flightId = -1;
            String freedSeat = null;
            String depDate = null;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT ts.FlightID, ts.SeatNumber, ts.DepartureDate "
                    + "FROM TicketSegment ts WHERE ts.TicketID = ? ORDER BY ts.SegmentOrder ASC LIMIT 1")) {
                ps.setString(1, ticketId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    flightId  = rs.getLong("FlightID");
                    freedSeat = rs.getString("SeatNumber");
                    depDate   = rs.getDate("DepartureDate") != null
                            ? rs.getDate("DepartureDate").toString() : null;
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE Ticket SET Status = 'cancelled' WHERE TicketID = ?")) {
                ps.setString(1, ticketId);
                ps.executeUpdate();
            }

            if (flightId > 0 && depDate != null) {
                promoteFromWaitlist(flightId, freedSeat, depDate);
            }

            loadMyBookings();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Could not cancel booking: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAccountDropdown() {
        if (cbAccountLookup == null) return;
        suppressLookup = true;
        Object prev = cbAccountLookup.getSelectedItem();
        cbAccountLookup.removeAllItems();
        String sql = "SELECT a.AccountID, "
                   + "COALESCE(c.FirstName, '') AS FirstName, "
                   + "COALESCE(c.LastName, '')  AS LastName "
                   + "FROM Account a JOIN Customer c ON a.CustomerID = c.CustomerID "
                   + "ORDER BY a.AccountID";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String display = rs.getString("AccountID")
                        + " — " + rs.getString("FirstName")
                        + " " + rs.getString("LastName");
                cbAccountLookup.addItem(display.trim());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        suppressLookup = false;
        if (prev != null) cbAccountLookup.setSelectedItem(prev);
    }

    private void loadCustomerLookup(String accountId) {
        if (accountId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter an Account ID first.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        lookupCardsPanel.removeAll();

        String sql = "SELECT t.TicketID, t.TicketType, t.FlightClass, t.Status, "
                   + "ts.SeatNumber, ts.DepartureDate, ts.SpecialMeal, ts.SegmentOrder, "
                   + "f.FlightNumber, f.Airline_Name, f.DepartureAirport, f.ArrivalAirport "
                   + "FROM Ticket t "
                   + "JOIN TicketSegment ts ON t.TicketID = ts.TicketID "
                   + "JOIN Flight f ON ts.FlightID = f.FlightID "
                   + "WHERE t.AccountID = ? ORDER BY ts.DepartureDate DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    String depDate = rs.getDate("DepartureDate") != null
                            ? rs.getDate("DepartureDate").toString() : "";
                    JPanel card = createBookingCard(
                            rs.getString("TicketID"), rs.getString("TicketType"),
                            rs.getString("FlightClass"), rs.getString("Status"),
                            rs.getString("SeatNumber"), depDate,
                            rs.getString("SpecialMeal"), rs.getInt("FlightNumber"),
                            rs.getString("Airline_Name"), rs.getString("DepartureAirport"),
                            rs.getString("ArrivalAirport"), rs.getInt("SegmentOrder"));
                    card.setAlignmentX(Component.LEFT_ALIGNMENT);
                    lookupCardsPanel.add(card);
                    lookupCardsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                }
                if (!any) {
                    JLabel none = new JLabel("No bookings found for account: " + accountId);
                    none.setFont(new Font("Lucida Sans", Font.ITALIC, 14));
                    none.setBorder(BorderFactory.createEmptyBorder(10, 6, 0, 0));
                    lookupCardsPanel.add(none);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JLabel err = new JLabel("Error: " + e.getMessage());
            err.setForeground(Color.RED);
            lookupCardsPanel.add(err);
        }
        lookupCardsPanel.revalidate();
        lookupCardsPanel.repaint();
    }

    private void loadAllUsers() {
        if (usersTableModel == null) return;
        usersTableModel.setRowCount(0);
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT `user`, role, created_at FROM users ORDER BY created_at DESC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                usersTableModel.addRow(new Object[]{
                    rs.getString("user"),
                    rs.getString("role"),
                    rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAllBookings() {
        if (allBookingsTableModel == null) return;
        allBookingsTableModel.setRowCount(0);
        String sql = "SELECT t.AccountID, "
                   + "CONCAT(c.FirstName, ' ', c.LastName) AS CustomerName, "
                   + "CONCAT(f.DepartureAirport, '→', f.ArrivalAirport) AS Route, "
                   + "f.FlightNumber, t.FlightClass, t.Status, ts.DepartureDate "
                   + "FROM Ticket t "
                   + "JOIN TicketSegment ts ON t.TicketID = ts.TicketID "
                   + "JOIN Flight f ON ts.FlightID = f.FlightID "
                   + "JOIN Customer c ON t.CustomerID = c.CustomerID "
                   + "ORDER BY ts.DepartureDate DESC LIMIT 500";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                allBookingsTableModel.addRow(new Object[]{
                    rs.getString("AccountID"),
                    rs.getString("CustomerName"),
                    rs.getString("Route"),
                    rs.getInt("FlightNumber"),
                    rs.getString("FlightClass"),
                    rs.getString("Status"),
                    rs.getDate("DepartureDate")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showChangeFlightDialog(String ticketId, int segmentOrder) {
        JDialog dialog = new JDialog(this, "Change Flight — " + ticketId, true);
        dialog.setSize(900, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        Font fieldFont = new Font("Lucida Sans", Font.PLAIN, 13);
        String[] airports = airportList();

        JComboBox<String> cbFrom = new JComboBox<>(airports);
        JComboBox<String> cbTo   = new JComboBox<>(airports);
        JTextField tfDialogDate  = new JTextField(10);
        tfDialogDate.setFont(fieldFont);
        tfDialogDate.setToolTipText("yyyy-MM-dd or leave blank");
        cbFrom.setFont(fieldFont);
        cbTo.setFont(fieldFont);

        DefaultTableModel dialogModel = new DefaultTableModel(
                new String[]{"Flight #", "Airline", "From", "To", "Departure", "Arrival", "Type"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        List<Long> dialogFlightIds = new ArrayList<>();
        JTable dialogTable = new JTable(dialogModel);
        dialogTable.setRowHeight(22);
        dialogTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JComboBox<String> cbDialogSeat = new JComboBox<>();
        cbDialogSeat.addItem("No Preference");
        cbDialogSeat.setFont(fieldFont);

        dialogTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = dialogTable.getSelectedRow();
            if (row >= 0 && row < dialogFlightIds.size()) {
                long fid = dialogFlightIds.get(row);
                cbDialogSeat.removeAllItems();
                cbDialogSeat.addItem("No Preference");
                String sql = "SELECT ac.SeatCapacity FROM Aircraft ac "
                           + "JOIN Flight f ON ac.AircraftID = f.AircraftID WHERE f.FlightID = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setLong(1, fid);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        int capacity = rs.getInt("SeatCapacity");
                        int rows = (int) Math.ceil(capacity / 6.0);
                        String[] letters = {"A","B","C","D","E","F"};
                        int count = 0;
                        outer:
                        for (int r2 = 1; r2 <= rows; r2++) {
                            for (String l : letters) {
                                cbDialogSeat.addItem(r2 + l);
                                if (++count >= capacity) break outer;
                            }
                        }
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        JButton btnDialogSearch = new JButton("Search Flights");
        btnDialogSearch.setFont(fieldFont);
        btnDialogSearch.addActionListener(e -> {
            dialogModel.setRowCount(0);
            dialogFlightIds.clear();
            String fromSel = cbFrom.getSelectedItem().toString();
            String toSel   = cbTo.getSelectedItem().toString();
            String fromCode = fromSel.equals("Any") ? "" : fromSel.substring(0, 3);
            String toCode   = toSel.equals("Any")   ? "" : toSel.substring(0, 3);
            String dateVal  = tfDialogDate.getText().trim();

            String sql = "SELECT FlightID, FlightNumber, Airline_Name, DepartureAirport, "
                       + "ArrivalAirport, DepartureTime, ArrivalTime, Travel_Type "
                       + "FROM Flight WHERE 1=1";
            if (!fromCode.isEmpty()) sql += " AND DepartureAirport = ?";
            if (!toCode.isEmpty())   sql += " AND ArrivalAirport = ?";
            if (!dateVal.isEmpty())  sql += " AND DATE(DepartureTime) = ?";
            sql += " ORDER BY DepartureTime LIMIT 100";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int idx = 1;
                if (!fromCode.isEmpty()) ps.setString(idx++, fromCode);
                if (!toCode.isEmpty())   ps.setString(idx++, toCode);
                if (!dateVal.isEmpty())  ps.setString(idx++, dateVal);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        dialogFlightIds.add(rs.getLong("FlightID"));
                        dialogModel.addRow(new Object[]{
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
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error loading flights: " + ex.getMessage());
            }
        });

        JButton btnConfirmChange = new JButton("Confirm Change");
        btnConfirmChange.setFont(new Font("Lucida Sans", Font.BOLD, 14));
        btnConfirmChange.addActionListener(e -> {
            int row = dialogTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dialog, "Select a new flight from the table first.");
                return;
            }
            long newFlightId = dialogFlightIds.get(row);
            String newFrom   = dialogModel.getValueAt(row, 2).toString();
            String newTo     = dialogModel.getValueAt(row, 3).toString();
            String newDep    = dialogModel.getValueAt(row, 4).toString();
            String newDate   = newDep.length() >= 10 ? newDep.substring(0, 10) : newDep;
            String seatSel   = cbDialogSeat.getSelectedItem().toString();
            String seatNum   = seatSel.equals("No Preference") ? null : seatSel;

            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE TicketSegment SET FlightID=?, DepartureDate=?, SeatNumber=? "
                        + "WHERE TicketID=? AND SegmentOrder=?")) {
                    ps.setLong(1, newFlightId);
                    ps.setString(2, newDate);
                    ps.setString(3, seatNum);
                    ps.setString(4, ticketId);
                    ps.setInt(5, segmentOrder);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Ticket SET Status='changed', FromAirport=?, ToAirport=? "
                        + "WHERE TicketID=?")) {
                    ps.setString(1, newFrom);
                    ps.setString(2, newTo);
                    ps.setString(3, ticketId);
                    ps.executeUpdate();
                }
                dialog.dispose();
                loadMyBookings();
            } catch (SQLException ex) {
                ex.printStackTrace();
                if (ex.getErrorCode() == 1062) {
                    JOptionPane.showMessageDialog(dialog,
                            "Seat " + seatSel + " is already taken on that flight. Pick another.");
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "Change failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JPanel searchBar = new JPanel(new GridLayout(2, 4, 8, 6));
        searchBar.setBorder(BorderFactory.createTitledBorder("Filter Flights"));
        JLabel lbF = new JLabel("From"); lbF.setFont(fieldFont);
        JLabel lbT = new JLabel("To");   lbT.setFont(fieldFont);
        JLabel lbD = new JLabel("Date (yyyy-MM-dd)");
        lbD.setFont(new Font("Lucida Sans", Font.PLAIN, 11));
        searchBar.add(lbF); searchBar.add(lbT); searchBar.add(lbD); searchBar.add(new JLabel());
        searchBar.add(cbFrom); searchBar.add(cbTo); searchBar.add(tfDialogDate);
        searchBar.add(btnDialogSearch);

        JPanel seatRow = new JPanel(new GridLayout(1, 4, 8, 0));
        seatRow.setOpaque(false);
        seatRow.setBorder(BorderFactory.createTitledBorder("Seat for New Flight"));
        JLabel lbSeat = new JLabel("Seat Number"); lbSeat.setFont(fieldFont);
        seatRow.add(lbSeat);
        seatRow.add(cbDialogSeat);
        seatRow.add(new JLabel());
        seatRow.add(btnConfirmChange);

        JScrollPane tableScroll = new JScrollPane(dialogTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder(
                "Available Flights (select one, then pick a seat)"));

        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.add(seatRow, BorderLayout.CENTER);

        dialog.add(searchBar, BorderLayout.NORTH);
        dialog.add(tableScroll, BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String[] airportList() {
        return new String[] {
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
                cardLayout.show(rootPanel, "myBookings");
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

    private boolean isFlightFull(long flightId) {
        String sql = "SELECT ac.SeatCapacity, "
                   + "COUNT(ts.SeatNumber) AS booked "
                   + "FROM Aircraft ac "
                   + "JOIN Flight f ON f.AircraftID = ac.AircraftID "
                   + "LEFT JOIN TicketSegment ts ON ts.FlightID = f.FlightID "
                   + "  AND ts.SeatNumber IS NOT NULL "
                   + "LEFT JOIN Ticket t ON ts.TicketID = t.TicketID "
                   + "  AND t.Status NOT IN ('cancelled') "
                   + "WHERE f.FlightID = ? "
                   + "GROUP BY ac.SeatCapacity";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, flightId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("booked") >= rs.getInt("SeatCapacity");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private int getWaitlistPosition(long flightId) {
        if (loggedInCustomerId < 0) return -1;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT RequestedAt FROM FlightWaitlist WHERE FlightID = ? AND CustomerID = ?")) {
            ps.setLong(1, flightId);
            ps.setInt(2, loggedInCustomerId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return -1;
            java.sql.Timestamp myTime = rs.getTimestamp("RequestedAt");
            try (PreparedStatement ps2 = con.prepareStatement(
                    "SELECT COUNT(*) FROM FlightWaitlist WHERE FlightID = ? AND RequestedAt < ?")) {
                ps2.setLong(1, flightId);
                ps2.setTimestamp(2, myTime);
                ResultSet rs2 = ps2.executeQuery();
                rs2.next();
                return rs2.getInt(1) + 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void joinWaitlist(long flightId) {
        if (loggedInCustomerId < 0) {
            JOptionPane.showMessageDialog(this,
                    "No customer profile found for '" + user + "'. Contact an admin.",
                    "Profile Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO FlightWaitlist (FlightID, CustomerID) VALUES (?, ?)")) {
            ps.setLong(1, flightId);
            ps.setInt(2, loggedInCustomerId);
            ps.executeUpdate();
            int pos = getWaitlistPosition(flightId);
            JOptionPane.showMessageDialog(this,
                    "You've been added to the waitlist.\nYour position: " + pos
                    + "\nYou'll be automatically booked when a seat opens.",
                    "Waitlist Confirmed", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                int pos = getWaitlistPosition(flightId);
                JOptionPane.showMessageDialog(this,
                        "You're already on the waitlist for this flight.\nYour position: " + pos,
                        "Already Waitlisted", JOptionPane.INFORMATION_MESSAGE);
            } else {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Could not join waitlist: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void promoteFromWaitlist(long flightId, String freedSeat, String depDate) {
        try {
            long waitlistId = -1;
            int nextCustomerId = -1;
            String nextAccountId = null, nextFrom = null, nextTo = null;

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT wl.WaitlistID, wl.CustomerID, a.AccountID, "
                    + "f.DepartureAirport, f.ArrivalAirport "
                    + "FROM FlightWaitlist wl "
                    + "JOIN Account a ON a.CustomerID = wl.CustomerID "
                    + "JOIN Flight f ON f.FlightID = wl.FlightID "
                    + "WHERE wl.FlightID = ? ORDER BY wl.RequestedAt ASC LIMIT 1")) {
                ps.setLong(1, flightId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return;
                waitlistId     = rs.getLong("WaitlistID");
                nextCustomerId = rs.getInt("CustomerID");
                nextAccountId  = rs.getString("AccountID");
                nextFrom       = rs.getString("DepartureAirport");
                nextTo         = rs.getString("ArrivalAirport");
            }

            String newTicketId  = "TKT-WL-" + System.currentTimeMillis();
            long   newTicketNum = System.currentTimeMillis();

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Ticket (TicketID, TicketNumber, TicketType, FlightClass, "
                    + "BookingFee, TotalFare, Flexibility, FromAirport, ToAirport, "
                    + "IsPaid, Status, CustomerID, AccountID) "
                    + "VALUES (?, ?, 'one_way', 'economy', 0.00, 0.00, FALSE, ?, ?, FALSE, 'booked', ?, ?)")) {
                ps.setString(1, newTicketId);
                ps.setLong(2, newTicketNum);
                ps.setString(3, nextFrom);
                ps.setString(4, nextTo);
                ps.setInt(5, nextCustomerId);
                ps.setString(6, nextAccountId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO TicketSegment "
                    + "(TicketID, SegmentOrder, FlightID, DepartureDate, SeatNumber, SpecialMeal, SegmentFare) "
                    + "VALUES (?, 1, ?, ?, ?, NULL, 0.00)")) {
                ps.setString(1, newTicketId);
                ps.setLong(2, flightId);
                ps.setString(3, depDate);
                ps.setString(4, freedSeat);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM FlightWaitlist WHERE WaitlistID = ?")) {
                ps.setLong(1, waitlistId);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this,
                    "A seat was freed — the next customer on the waitlist has been automatically booked.\n"
                    + "New Ticket ID: " + newTicketId,
                    "Waitlist Promotion", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadWaitlistEntries() {
        if (waitlistCardsPanel == null) return;
        waitlistCardsPanel.removeAll();

        if (loggedInCustomerId < 0) {
            waitlistCardsPanel.revalidate();
            waitlistCardsPanel.repaint();
            return;
        }

        String sql = "SELECT wl.WaitlistID, wl.FlightID, wl.RequestedAt, "
                   + "f.FlightNumber, f.Airline_Name, f.DepartureAirport, f.ArrivalAirport, "
                   + "f.DepartureTime, "
                   + "(SELECT COUNT(*) + 1 FROM FlightWaitlist wl2 "
                   + " WHERE wl2.FlightID = wl.FlightID AND wl2.RequestedAt < wl.RequestedAt) AS QueuePosition "
                   + "FROM FlightWaitlist wl "
                   + "JOIN Flight f ON wl.FlightID = f.FlightID "
                   + "WHERE wl.CustomerID = ? ORDER BY wl.RequestedAt ASC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, loggedInCustomerId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    long   wlId      = rs.getLong("WaitlistID");
                    String from      = rs.getString("DepartureAirport");
                    String to        = rs.getString("ArrivalAirport");
                    String airline   = rs.getString("Airline_Name");
                    int    flightNum = rs.getInt("FlightNumber");
                    String depTime   = rs.getTimestamp("DepartureTime") != null
                            ? rs.getTimestamp("DepartureTime").toString().substring(0, 16) : "";
                    String requested = rs.getTimestamp("RequestedAt") != null
                            ? rs.getTimestamp("RequestedAt").toString().substring(0, 16) : "";
                    int    position  = rs.getInt("QueuePosition");

                    JPanel card = new JPanel(new BorderLayout(12, 6));
                    card.setBackground(Color.WHITE);
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(130, 80, 180), 4),
                            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
                    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

                    JPanel badge = new JPanel(new BorderLayout());
                    badge.setPreferredSize(new Dimension(90, 0));
                    badge.setBackground(new Color(130, 80, 180));
                    JLabel badgeLbl = new JLabel("WAITLIST", SwingConstants.CENTER);
                    badgeLbl.setForeground(Color.WHITE);
                    badgeLbl.setFont(new Font("Lucida Sans", Font.BOLD, 10));
                    badge.add(badgeLbl, BorderLayout.CENTER);

                    JPanel info = new JPanel();
                    info.setOpaque(false);
                    info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

                    JLabel route = new JLabel(from + "  →  " + to);
                    route.setFont(new Font("Lucida Sans", Font.BOLD, 17));

                    JLabel detail1 = new JLabel(airline + "  ·  Flight " + flightNum
                            + "  ·  Departs: " + depTime);
                    detail1.setFont(new Font("Lucida Sans", Font.PLAIN, 13));
                    detail1.setForeground(new Color(60, 60, 60));

                    JLabel detail2 = new JLabel("Queue position: " + position
                            + "  ·  Joined: " + requested);
                    detail2.setFont(new Font("Lucida Sans", Font.PLAIN, 12));
                    detail2.setForeground(new Color(100, 100, 100));

                    info.add(route);
                    info.add(Box.createRigidArea(new Dimension(0, 4)));
                    info.add(detail1);
                    info.add(Box.createRigidArea(new Dimension(0, 3)));
                    info.add(detail2);

                    JButton btnLeave = new JButton("Leave Waitlist");
                    btnLeave.setFont(new Font("Lucida Sans", Font.PLAIN, 12));
                    btnLeave.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            int choice = JOptionPane.showConfirmDialog(ProjectFrame.this,
                                    "Leave the waitlist for Flight " + flightNum
                                    + " (" + from + " → " + to + ")?",
                                    "Confirm", JOptionPane.YES_NO_OPTION);
                            if (choice == JOptionPane.YES_OPTION) {
                                try (PreparedStatement ps2 = con.prepareStatement(
                                        "DELETE FROM FlightWaitlist WHERE WaitlistID = ?")) {
                                    ps2.setLong(1, wlId);
                                    ps2.executeUpdate();
                                    loadWaitlistEntries();
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    });

                    JPanel actionPanel = new JPanel();
                    actionPanel.setOpaque(false);
                    actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
                    actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
                    btnLeave.setAlignmentX(Component.CENTER_ALIGNMENT);
                    actionPanel.add(Box.createVerticalGlue());
                    actionPanel.add(btnLeave);
                    actionPanel.add(Box.createVerticalGlue());

                    card.add(badge, BorderLayout.WEST);
                    card.add(info, BorderLayout.CENTER);
                    card.add(actionPanel, BorderLayout.EAST);
                    card.setAlignmentX(Component.LEFT_ALIGNMENT);
                    waitlistCardsPanel.add(card);
                    waitlistCardsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                }
                if (!any) {
                    JLabel none = new JLabel("Not on any waitlists.");
                    none.setFont(new Font("Lucida Sans", Font.ITALIC, 13));
                    none.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
                    waitlistCardsPanel.add(none);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JLabel err = new JLabel("Error loading waitlist: " + e.getMessage());
            err.setForeground(Color.RED);
            waitlistCardsPanel.add(err);
        }
        waitlistCardsPanel.revalidate();
        waitlistCardsPanel.repaint();
    }

    private JPanel buildMyBookingsPanel() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        page.setBackground(new Color(242, 240, 230));

        Font fieldFont = new Font("Lucida Sans", Font.PLAIN, 14);

        JButton btnBack = new JButton("← Back to Dashboard");
        btnBack.setFont(fieldFont);
        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(rootPanel, "dashboard");
            }
        });

        JLabel title = new JLabel("My Bookings");
        title.setFont(new Font("Lucida Sans", Font.BOLD, 20));

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);
        topPanel.add(btnBack, BorderLayout.WEST);
        topPanel.add(title, BorderLayout.CENTER);

        bookingsCardsPanel = new JPanel();
        bookingsCardsPanel.setLayout(new BoxLayout(bookingsCardsPanel, BoxLayout.Y_AXIS));
        bookingsCardsPanel.setBackground(new Color(242, 240, 230));
        bookingsCardsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JScrollPane bookingsScroll = new JScrollPane(bookingsCardsPanel);
        bookingsScroll.setBorder(BorderFactory.createTitledBorder("My Bookings"));
        bookingsScroll.getVerticalScrollBar().setUnitIncrement(16);
        bookingsScroll.setBackground(new Color(242, 240, 230));

        waitlistCardsPanel = new JPanel();
        waitlistCardsPanel.setLayout(new BoxLayout(waitlistCardsPanel, BoxLayout.Y_AXIS));
        waitlistCardsPanel.setBackground(new Color(242, 240, 230));
        waitlistCardsPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JScrollPane waitlistScroll = new JScrollPane(waitlistCardsPanel);
        waitlistScroll.setBorder(BorderFactory.createTitledBorder("My Waitlist"));
        waitlistScroll.getVerticalScrollBar().setUnitIncrement(16);
        waitlistScroll.setBackground(new Color(242, 240, 230));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, bookingsScroll, waitlistScroll);
        splitPane.setResizeWeight(0.72);
        splitPane.setDividerSize(6);

        page.add(topPanel, BorderLayout.NORTH);
        page.add(splitPane, BorderLayout.CENTER);
        return page;
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

        // First-run check: if no admin exists, prompt to create one before login
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'admin'")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                Font dlgFont = new Font("Lucida Sans", Font.PLAIN, 14);
                JTextField tfAdminUser    = new JTextField(18);
                JPasswordField tfAdminPass = new JPasswordField(18);
                JPasswordField tfAdminConf = new JPasswordField(18);
                tfAdminUser.setFont(dlgFont);
                tfAdminPass.setFont(dlgFont);
                tfAdminConf.setFont(dlgFont);

                JPanel setupPanel = new JPanel(new GridLayout(4, 2, 8, 8));
                setupPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                JLabel hdr = new JLabel("No admin account found. Create the first admin:");
                hdr.setFont(new Font("Lucida Sans", Font.BOLD, 13));
                setupPanel.add(hdr);           setupPanel.add(new JLabel());
                setupPanel.add(new JLabel("Username:")); setupPanel.add(tfAdminUser);
                setupPanel.add(new JLabel("Password:")); setupPanel.add(tfAdminPass);
                setupPanel.add(new JLabel("Confirm password:")); setupPanel.add(tfAdminConf);

                while (true) {
                    int choice = JOptionPane.showConfirmDialog(null, setupPanel,
                            "First-Time Setup — Create Admin Account",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (choice != JOptionPane.OK_OPTION) { System.exit(0); }

                    String adminUser = tfAdminUser.getText().trim();
                    String adminPass = new String(tfAdminPass.getPassword()).trim();
                    String adminConf = new String(tfAdminConf.getPassword()).trim();

                    if (adminUser.isEmpty() || adminPass.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Username and password cannot be empty.");
                        continue;
                    }
                    if (!adminPass.equals(adminConf)) {
                        JOptionPane.showMessageDialog(null, "Passwords do not match. Try again.");
                        continue;
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO users (`user`, `password`, role) VALUES (?, ?, 'admin')")) {
                        ps.setString(1, adminUser);
                        ps.setString(2, adminPass);
                        ps.executeUpdate();
                        JOptionPane.showMessageDialog(null,
                                "Admin account \"" + adminUser + "\" created.\n"
                                + "Log in with these credentials to manage the system.",
                                "Setup Complete", JOptionPane.INFORMATION_MESSAGE);
                        break;
                    } catch (SQLException ex) {
                        if (ex.getErrorCode() == 1062) {
                            JOptionPane.showMessageDialog(null,
                                    "Username \"" + adminUser + "\" is already taken. Choose another.");
                        } else {
                            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
                        }
                    }
                }
            }
        }

        ProjectFrame myFrame = new ProjectFrame();
        myFrame.initialize();
    }
}
