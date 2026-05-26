package gui;

import gui.button.PilotButton;
import gui.button.ReturnButton;
import service.CpdlcService;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

public class LogonATCPanel extends JPanel {

    private final CpdlcService service;
    private final Runnable onBack;

    public LogonATCPanel(CpdlcService service, Runnable onBack) {
        this.service = service;
        this.onBack = onBack;
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout(0, 10));

        // RETURN
        PilotButton returnBtn = new ReturnButton();
        returnBtn.addActionListener(e -> onBack.run());
        add(returnBtn, BorderLayout.NORTH);

        // FORM AREA
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);

        // STATION
        JPanel stationContainer = new JPanel(new BorderLayout(0, 5));
        JLabel stationLabel = new JLabel("STATION");
        stationLabel.setFont(new Font(DashboardPanel.UI_FONT, Font.BOLD, 14));
        JTextField stationField = new JTextField(15);
        stationField.setFont(new Font(DashboardPanel.UI_FONT, Font.PLAIN, 14));
        stationField.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        stationField.setPreferredSize(new Dimension(200, 30));
        ((AbstractDocument) stationField.getDocument()).setDocumentFilter(new UppercaseFilter());

        stationContainer.add(stationLabel, BorderLayout.NORTH);
        stationContainer.add(stationField, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        formPanel.add(stationContainer, gbc);

        // REMARKS
        gbc.fill = GridBagConstraints.BOTH;
        JPanel messageContainer = new JPanel(new BorderLayout(0, 5));
        JLabel textLabel = new JLabel("REMARKS");
        textLabel.setFont(new Font(DashboardPanel.UI_FONT, Font.BOLD, 14));
        JTextArea messageArea = new JTextArea();
        messageArea.setFont(new Font(DashboardPanel.UI_FONT, Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        ((AbstractDocument) messageArea.getDocument()).setDocumentFilter(new UppercaseFilter());

        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        messageContainer.add(textLabel, BorderLayout.NORTH);
        messageContainer.add(scrollPane, BorderLayout.CENTER);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 1.0;
        formPanel.add(messageContainer, gbc);

        add(formPanel, BorderLayout.CENTER);

        // SEND BUTTON
        PilotButton sendBtn = new PilotButton("SEND LOGON");
        sendBtn.setPreferredSize(new Dimension(0, 40));
        sendBtn.setCustomColor(new Color(140, 80, 160), Color.WHITE);
        sendBtn.addActionListener(e -> {
            String station = stationField.getText().trim();
            if(!station.isEmpty()) {
                service.sendLogon(station, messageArea.getText().trim());
                stationField.setText("");
                messageArea.setText("");
                onBack.run();
            }
        });
        add(sendBtn, BorderLayout.SOUTH);
    }
}
