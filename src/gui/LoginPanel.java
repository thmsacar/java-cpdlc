package gui;

import gui.button.PilotButton;
import hoppie.HoppieAPI;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.io.IOException;
import java.util.prefs.Preferences;

public class LoginPanel extends JPanel {

    private final JTextField callsignField = new JTextField(15);
    private final JTextField hoppieField = new JTextField(15);
    private final PilotButton loginButton = new PilotButton("CONNECT");

    private final Preferences prefs = Preferences.userNodeForPackage(LoginPanel.class);

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
                HoppieAPI.HoppieResponse response = checkHoppieID(callsign, hoppieID);
                if (response.body().trim().equalsIgnoreCase("ok")) client.showDashboard(callsign, hoppieID);
                else {
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

    private HoppieAPI.HoppieResponse checkHoppieID(String callsign, String hoppieID) throws IOException {
        HoppieAPI api = new HoppieAPI(hoppieID);
        return api.sendPing(callsign);
    }

    // Save pref
    private void saveData() {
        prefs.put("lastCallsign", callsignField.getText());
        prefs.put("lastHoppieID", hoppieField.getText());
    }

    // Load pref
    private void loadSavedData() {
        callsignField.setText(prefs.get("lastCallsign", ""));
        hoppieField.setText(prefs.get("lastHoppieID", ""));
    }
}
