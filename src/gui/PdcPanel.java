package gui;

import flight.Flight;
import gui.button.PilotButton;
import gui.button.ReturnButton;
import service.CpdlcService;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.util.prefs.Preferences;

public class PdcPanel extends JPanel {

    private final CpdlcService service;
    private final Runnable onBack;
    private final Preferences prefs = Preferences.userNodeForPackage(PdcPanel.class);

    private JTextField stationField;
    private JTextField atisField;
    private JTextField typeField;
    private JTextField depField;
    private JTextField destField;
    private JTextField standField;
    private JTextField pdcRemarkField;

    public PdcPanel(CpdlcService service, Runnable onBack) {
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

        // FORM
        JPanel formArea = createPDCFormArea();
        formArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(createScrollPane(formArea), BorderLayout.CENTER);

        // BUTTON
        PilotButton requestBtn = new PilotButton("REQUEST CLEARANCE");
        requestBtn.setPreferredSize(new Dimension(0, 40));
        requestBtn.setCustomColor(new Color(0, 100, 150), Color.WHITE);
        requestBtn.addActionListener(e -> sendPDCRequest());
        add(requestBtn, BorderLayout.SOUTH);
    }

    private JPanel createPDCFormArea() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        // Simbrief
        JPanel simbriefRow = new JPanel(new BorderLayout(5, 0));
        simbriefRow.setOpaque(false);
        JTextField simbriefField = createStyledTextField("");
        PilotButton fetchBtn = new PilotButton("FETCH");
        fetchBtn.setPreferredSize(new Dimension(80, 30));
        fetchBtn.addActionListener(e -> {
            prefs.put("lastSimbriefID", simbriefField.getText());
            service.fetchSimbriefData(simbriefField.getText(), new CpdlcService.SimbriefCallback() {
                @Override
                public void onSuccess(Flight flight) {
                    SwingUtilities.invokeLater(() -> {
                        depField.setText(flight.getOrigin());
                        destField.setText(flight.getDestination());
                        typeField.setText(flight.getAircraft());
                    });
                }
                @Override
                public void onFailure(Exception e) {
                    // Error handled by service notifying listeners, or we could handle locally
                }
            });
        });
        simbriefField.setText(prefs.get("lastSimbriefID", ""));
        simbriefRow.add(simbriefField, BorderLayout.CENTER);
        simbriefRow.add(fetchBtn, BorderLayout.EAST);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(createLabeledComponent("SIMBRIEF ID", simbriefRow), gbc);

        // Station & Atis
        gbc.gridwidth = 1; gbc.gridy = 1;
        panel.add(createLabeledComponent("STATION", stationField = createStyledTextField("")), gbc);
        gbc.gridx = 1;
        panel.add(createLabeledComponent("ATIS", atisField = createStyledTextField("")), gbc);

        // Departure & Destination
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(createLabeledComponent("DEPARTURE", depField = createStyledTextField("")), gbc);
        gbc.gridx = 1;
        panel.add(createLabeledComponent("DESTINATION", destField = createStyledTextField("")), gbc);

        // Type & Stand
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(createLabeledComponent("ACFT TYPE", typeField = createStyledTextField("")), gbc);
        gbc.gridx = 1;
        panel.add(createLabeledComponent("STAND / GATE", standField = createStyledTextField("")), gbc);

        // Remarks
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(createLabeledComponent("REMARKS", pdcRemarkField = createStyledTextField("")), gbc);

        return panel;
    }

    private void sendPDCRequest() {
        JTextField[] fields = {stationField, atisField, typeField, depField, destField, standField};
        for (JTextField field : fields) {
            if (field.getText().trim().isEmpty()) {
                field.setBorder(BorderFactory.createLineBorder(Color.RED));
                field.requestFocus();
                return;
            }
            field.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        }

        Flight flight = new Flight(service.getCallsign(), depField.getText().trim(), destField.getText().trim(), typeField.getText().trim());
        service.sendPdcRequest(stationField.getText().trim(), flight, standField.getText().trim(), atisField.getText().trim(), pdcRemarkField.getText().trim());
        onBack.run();
    }

    private JScrollPane createScrollPane(Component c) {
        JScrollPane scrollPane = new JScrollPane(c);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        return scrollPane;
    }

    private JPanel createLabeledComponent(String labelText, JComponent component) {
        JPanel container = new JPanel(new BorderLayout(0, 3));
        container.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font(DashboardPanel.UI_FONT, Font.BOLD, 12));
        label.setForeground(Color.LIGHT_GRAY);
        container.add(label, BorderLayout.NORTH);
        container.add(component, BorderLayout.CENTER);
        return container;
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font(DashboardPanel.UI_FONT, Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)
        ));
        field.setPreferredSize(new Dimension(0, 30));
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new UppercaseFilter());
        return field;
    }
}
