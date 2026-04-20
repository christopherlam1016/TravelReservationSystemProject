import java.sql.*;
import java.util.Arrays;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ProjectFrame extends JFrame {

    // GUI related variables
    final private Font mainFont = new Font("Lucida Sans", Font.BOLD, 18);
    JTextField tfuser;
    JPasswordField tfpasswd;
    JLabel msg;

    // Database related variables
    // NOTE: In production, move credentials to a config file or environment
    // variables and do not commit them to source control.
    private static Connection con = null;
    public static boolean userLoggedin = false;
    public static String user = "";

    public void initialize() throws Exception {

        // inputPanel -------------------------------------------------
        JLabel lbuser = new JLabel("Username");
        lbuser.setFont(mainFont);
        tfuser = new JTextField();
        tfuser.setFont(mainFont);

        JLabel lbpasswd = new JLabel("Password");
        lbpasswd.setFont(mainFont);
        tfpasswd = new JPasswordField();
        tfpasswd.setFont(mainFont);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 2, 5, 5));
        inputPanel.setOpaque(false);
        inputPanel.add(lbuser);
        inputPanel.add(tfuser);
        inputPanel.add(lbpasswd);
        inputPanel.add(tfpasswd);

        // msg --------------------------------------------------------
        msg = new JLabel();
        msg.setFont(mainFont);

        // buttonPanel ------------------------------------------------
        JButton btnAdd = new JButton("Add User");
        btnAdd.setFont(mainFont);
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newUser = tfuser.getText();
                char[] passwdChars = tfpasswd.getPassword();
                try {
                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO users (username, password) VALUES (?, ?)");
                    ps.setString(1, newUser);
                    ps.setString(2, new String(passwdChars));
                    int result = ps.executeUpdate();
                    ps.close();
                    msg.setText("User " + newUser + " has been added (" + result + ")");
                } catch (SQLException e1) {
                    msg.setText("Unable to add new user");
                } finally {
                    Arrays.fill(passwdChars, '\0');
                }
            }
        });

        JButton btnLogin = new JButton("Login");
        btnLogin.setFont(mainFont);
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                user = tfuser.getText();
                char[] passwdChars = tfpasswd.getPassword();
                ResultSet rset = null;
                PreparedStatement ps = null;
                try {
                    ps = con.prepareStatement(
                            "SELECT * FROM users WHERE username=? AND password=?");
                    ps.setString(1, user);
                    ps.setString(2, new String(passwdChars));
                    rset = ps.executeQuery();
                    if (rset.next()) {
                        userLoggedin = true;
                        msg.setText("Welcome " + user);
                    } else {
                        msg.setText("Unknown user " + user + " – not logged in");
                    }
                } catch (SQLException e1) {
                    msg.setText("Login error – please try again");
                } finally {
                    Arrays.fill(passwdChars, '\0');
                    try {
                        if (rset != null) rset.close();
                        if (ps   != null) ps.close();
                    } catch (SQLException ignored) { }
                }
            }
        });

        JButton btnClear = new JButton("Clear");
        btnClear.setFont(mainFont);
        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tfuser.setText("");
                tfpasswd.setText("");
                msg.setText("");
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 3, 5, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnClear);

        // mainPanel --------------------------------------------------
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(230, 140, 140));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(msg, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.add(mainPanel);
        this.setTitle("Login Page");
        this.setSize(500, 300);
        this.setMinimumSize(new Dimension(300, 200));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        // Read database credentials from environment variables when available,
        // falling back to development defaults.
        String url      = System.getenv().getOrDefault("DB_URL",      "jdbc:mysql://localhost:3306/testproject");
        String dbUser   = System.getenv().getOrDefault("DB_USER",     "testuser");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "abc123");
        try {
            con = DriverManager.getConnection(url, dbUser, password);
        } catch (SQLException e) {
            System.out.println("Unable to create a connection to the database");
            e.printStackTrace();
            System.exit(1);
        }

        ProjectFrame myFrame = new ProjectFrame();
        myFrame.initialize();
    }
}
