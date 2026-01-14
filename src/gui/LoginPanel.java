package gui;

import hoppie.HoppieAPI;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.io.IOException;
import java.util.prefs.Preferences;

public class LoginPanel extends JPanel {

    private JTextField callsignField = new JTextField(15);
    private JTextField hoppieField = new JTextField(15);
    private JButton saveButton = new JButton("SAVE");

    private Preferences prefs = Preferences.userNodeForPackage(LoginPanel.class);

    public LoginPanel(Client client) {

        // Init GridBagLayout
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Init gbc
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // --- Callsign Label ---
        gbc.gridy = 0; // 0. satır
        JLabel callsignLabel = new JLabel("Callsign:");
        add(callsignLabel, gbc);

        // --- Callsign Field ---
        gbc.gridy = 1; // 1. satır
        Dimension fieldSize = new Dimension(300, 28);
        callsignField.setMinimumSize(fieldSize);
        callsignField.setPreferredSize(fieldSize);
        add(callsignField, gbc);

        // Blank space
        gbc.gridy = 2;
        add(Box.createVerticalStrut(10), gbc);

        // --- Hoppie ID Label ---
        gbc.gridy = 3;
        add(new JLabel("Hoppie ID:"), gbc);

        // --- Hoppie ID Field ---
        gbc.gridy = 4;
        hoppieField.setMinimumSize(fieldSize);
        hoppieField.setPreferredSize(fieldSize);
        add(hoppieField, gbc);

        // Save Button
        gbc.gridy = 5;
        gbc.insets = new Insets(20, 5, 5, 5);
        add(saveButton, gbc);

        hoppieField.getInputMap().put(KeyStroke.getKeyStroke("control V"), "paste");
        hoppieField.getInputMap().put(KeyStroke.getKeyStroke("meta V"), "paste");

        // Document Filter for CAPS
        ((AbstractDocument) callsignField.getDocument())
                .setDocumentFilter(new UppercaseFilter());

        // Action Listener
        saveButton.addActionListener(e -> {
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
                JOptionPane.showMessageDialog(null, "Network Error: " + ex.getMessage());            }

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
