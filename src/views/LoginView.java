package views;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import entity.Group;
import interface_adapter.Login.LoginController;
import interface_adapter.Login.LoginState;
import interface_adapter.Login.LoginViewModel;

/**
 * The View for the Login Use Case.
 */
public class LoginView extends JPanel implements ActionListener, PropertyChangeListener {
    private final String viewName; // Login
    private final JTextField usernameInputField = new JTextField(20);
    private final JPasswordField passwordInputField = new JPasswordField(20);

    private final JButton loginButton;
    private final ViewManager viewManager;
    private LoginController loginController;
    private final LoginViewModel loginViewModel;

    public LoginView(LoginViewModel loginViewModel, ViewManager viewManager) {
        this.loginViewModel = loginViewModel;
        this.viewManager = viewManager;
        this.viewName = loginViewModel.getViewName(); // Assume getViewName() returns "loginView"
        loginViewModel.addPropertyChangeListener(this);

        // Set the preferred, minimum, and maximum size of the panel
        this.setPreferredSize(new Dimension(1280, 720));
        this.setMinimumSize(new Dimension(1280, 720));
        this.setMaximumSize(new Dimension(1280, 720));
        this.setLayout(new GridBagLayout());
        this.setBorder(new EmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Title label for "LinkUp" with custom font style
        JLabel title = new JLabel("LinkUp");
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        this.add(title, gbc);

        // Username field
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        this.add(usernameLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        usernameInputField.setToolTipText("Enter your username");
        this.add(usernameInputField, gbc);

        // Password field
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        this.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        passwordInputField.setToolTipText("Enter your password");
        this.add(passwordInputField, gbc);

        // "No Existing Account Yet?" label
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel noAccountLabel = new JLabel("No Existing Account Yet?");
        noAccountLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        noAccountLabel.setForeground(new Color(30, 144, 255)); // Set text color to blue
        noAccountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Change cursor to hand

        // Add mouse listener to label to handle click event
        noAccountLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                viewManager.switchToView("accountCreationView"); // Redirect to sign-up view
            }
        });

        this.add(noAccountLabel, gbc);

        // Buttons panel for Log In button only
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false); // Make buttons panel transparent
        GridBagConstraints btnGbc = new GridBagConstraints();
        btnGbc.insets = new Insets(10, 10, 10, 10);

        // Customizing Login Button
        loginButton = new JButton("Log In") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fill background with rounded corners
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                // Set text color and paint text
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() + fm.getAscent()) / 2 - fm.getDescent();
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getForeground());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }

            @Override
            public void setContentAreaFilled(boolean b) {
                // Ignore to prevent default behavior
            }
        };

        // Styling Login Button
        Color buttonBlue = new Color(30, 144, 255); // Blue color
        loginButton.setBackground(buttonBlue);
        loginButton.setForeground(Color.WHITE); // White text
        loginButton.setOpaque(false);
        loginButton.setFocusPainted(false); // Remove focus border
        loginButton.setPreferredSize(new Dimension(100, 40)); // Set constant size

        // Add login button to the panel
        btnGbc.gridx = 0;
        btnGbc.gridy = 0;
        buttons.add(loginButton, btnGbc);

        // Add buttons panel to the main panel
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        this.add(buttons, gbc);

        // Action listener for login button only
        loginButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == loginButton) {
            loginController.execute(usernameInputField.getText(), new String(passwordInputField.getPassword()));
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("LoginSuccess")) {
            LoginState loginState = (LoginState) evt.getNewValue();
            GroupChatView groupChatView = (GroupChatView) viewManager.getView("groupChatView");
            groupChatView.refresh();
            groupChatView.refreshGroups();
            viewManager.switchToView("groupChatView");
        } else if (evt.getPropertyName().equals("LoginError")) {
            LoginState loginState = (LoginState) evt.getNewValue();
            JOptionPane.showMessageDialog(this, loginState.getLoginError(), "Login Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getViewName() {
        return this.viewName;
    }

    public void setLoginController(LoginController controller) {
        this.loginController = controller;
    }
}
