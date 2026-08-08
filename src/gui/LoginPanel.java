package gui;

import gui.button.PilotButton;
import service.CpdlcService;
import service.UpdateChecker;
import service.UserPreferences;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.io.IOException;

public class LoginPanel extends JPanel {

    private final JTextField callsignField = new JTextField(15);
    private final JTextField hoppieField = new JTextField(15);
    private final PilotButton loginButton = new PilotButton("CONNECT");

    public LoginPanel(Client client) {

        // Init GridBagLayout
        this.setLayout(new GridBagLayout());
        this.setBackground(new Color(45, 45, 45)); // Match Dashboard background (control)
        GridBagConstraints gbc = new GridBagConstraints();

        // Init gbc
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // --- Callsign Label ---
        gbc.gridy = 0;
        JLabel callsignLabel = new JLabel("Callsign:");
        callsignLabel.setForeground(Color.WHITE);
        callsignLabel.setFont(new Font("Roboto Mono", Font.BOLD, 14));
        add(callsignLabel, gbc);

        // --- Callsign Field ---
        gbc.gridy = 1;
        Dimension fieldSize = new Dimension(300, 35);
        callsignField.setMinimumSize(fieldSize);
        callsignField.setPreferredSize(fieldSize);
        callsignField.setBackground(new Color(30, 30, 30)); // Match Dashboard light background
        callsignField.setForeground(Color.WHITE);
        callsignField.setCaretColor(Color.WHITE);
        callsignField.setFont(new Font("Roboto Mono", Font.PLAIN, 14));
        callsignField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        add(callsignField, gbc);

        // Blank space
        gbc.gridy = 2;
        add(Box.createVerticalStrut(10), gbc);

        // --- Hoppie ID Label ---
        gbc.gridy = 3;
        JLabel hoppieLabel = new JLabel("Hoppie ID:");
        hoppieLabel.setForeground(Color.WHITE);
        hoppieLabel.setFont(new Font("Roboto Mono", Font.BOLD, 14));
        add(hoppieLabel, gbc);

        // --- Hoppie ID Field ---
        gbc.gridy = 4;
        hoppieField.setMinimumSize(fieldSize);
        hoppieField.setPreferredSize(fieldSize);
        hoppieField.setBackground(new Color(30, 30, 30));
        hoppieField.setForeground(Color.WHITE);
        hoppieField.setCaretColor(Color.WHITE);
        hoppieField.setFont(new Font("Roboto Mono", Font.PLAIN, 14));
        hoppieField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        add(hoppieField, gbc);

        // Login Button
        gbc.gridy = 5;
        gbc.insets = new Insets(30, 5, 5, 5);
        loginButton.setPreferredSize(new Dimension(300, 45));
        loginButton.setCustomColor(Color.darkGray, Color.WHITE); // Match Dashboard buttons
        loginButton.setFont(new Font("Roboto Mono", Font.BOLD, 15));
        loginButton.addColorChangerOnPress();
        add(loginButton, gbc);

        // Version Label (low contrast)
        gbc.gridy = 6;
        gbc.insets = new Insets(15, 5, 0, 5);
        JLabel versionLabel = new JLabel("v" + UpdateChecker.CURRENT_VERSION, SwingConstants.CENTER);
        versionLabel.setForeground(new Color(110, 110, 110));
        versionLabel.setFont(FontManager.REGULAR != null ? FontManager.REGULAR.deriveFont(11f) : new Font("Roboto Mono", Font.PLAIN, 11));
        add(versionLabel, gbc);

        hoppieField.getInputMap().put(KeyStroke.getKeyStroke("control V"), "paste");
        hoppieField.getInputMap().put(KeyStroke.getKeyStroke("meta V"), "paste");

        // Document Filter for CAPS
        ((AbstractDocument) callsignField.getDocument())
                .setDocumentFilter(new UppercaseFilter());

        // Action Listener
        loginButton.addActionListener(e -> {
            saveData();
            String callsign = callsignField.getText().trim();
            String hoppieID = hoppieField.getText().trim();


            //Check hoppie connection
            try {

                if (CpdlcService.validateCredentials(callsign, hoppieID)) {
                    client.showDashboard(callsign, hoppieID);
                } else {
                    JOptionPane.showMessageDialog(
                            null,
                            "INVALID LOGON\n\nPlease check your Callsign and Hoppie ID.",
                            "CPDLC Connection Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Network Error: " + ex.getMessage());
            }

        });

        loadSavedData();

    }

    // Save pref
    private void saveData() {
        UserPreferences.setLastCallsign(callsignField.getText());
        UserPreferences.setLastHoppieID(hoppieField.getText());
    }

    // Load pref
    private void loadSavedData() {
        callsignField.setText(UserPreferences.getLastCallsign());
        hoppieField.setText(UserPreferences.getLastHoppieID());
    }
}
