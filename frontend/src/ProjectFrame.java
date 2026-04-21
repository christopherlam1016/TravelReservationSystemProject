import java.sql.*;
import java.awt.*;
import java.awt.event.*;
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
    JTextField tfFrom, tfTo, tfDate;
    DefaultTableModel flightTableModel;

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

        JPanel searchPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createTitledBorder("Explore Flights"));
        JLabel lbFrom = new JLabel("From Airport");
        JLabel lbTo = new JLabel("To Airport");
        JLabel lbDate = new JLabel("Date (YYYY-MM-DD)");
        JLabel lbBlank = new JLabel();
        tfFrom = new JTextField();
        tfTo = new JTextField();
        tfDate = new JTextField();
        JButton btnSearchFlights = new JButton("Search Flights");

        lbFrom.setFont(new Font("Lucida Sans", Font.PLAIN, 14));
        lbTo.setFont(new Font("Lucida Sans", Font.PLAIN, 14));
        lbDate.setFont(new Font("Lucida Sans", Font.PLAIN, 14));
        tfFrom.setFont(new Font("Lucida Sans", Font.PLAIN, 14));
        tfTo.setFont(new Font("Lucida Sans", Font.PLAIN, 14));
        tfDate.setFont(new Font("Lucida Sans", Font.PLAIN, 14));
        btnSearchFlights.setFont(new Font("Lucida Sans", Font.BOLD, 14));

        btnSearchFlights.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadFlights(tfFrom.getText(), tfTo.getText(), tfDate.getText());
            }
        });

        searchPanel.add(lbFrom);
        searchPanel.add(lbTo);
        searchPanel.add(lbDate);
        searchPanel.add(lbBlank);
        searchPanel.add(tfFrom);
        searchPanel.add(tfTo);
        searchPanel.add(tfDate);
        searchPanel.add(btnSearchFlights);

        flightTableModel = new DefaultTableModel(
                new String[] { "Flight #", "Airline", "From", "To", "Departure", "Arrival", "Type" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable flightTable = new JTable(flightTableModel);
        flightTable.setRowHeight(22);
        JScrollPane tableScrollPane = new JScrollPane(flightTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Matching Flights"));

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
        String sql = "SELECT FlightNumber, Airline_Name, DepartureAirport, ArrivalAirport, "
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
