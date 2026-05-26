package gui;

import gui.button.PilotButton;
import hoppie.AcarsMessage;
import service.CpdlcListener;
import service.CpdlcService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardPanel extends JPanel implements CpdlcListener {
    
    public static final String UI_FONT = "Roboto Mono";

    private final CpdlcService service;
    private final Client client;

    private JLabel callsignLabel;
    private JLabel atsLabel;
    private final CardLayout cardLayout;
    private final JPanel cardContainer;

    private MessageListPanel listPanel;
    private MessageDetailPanel detailPanel;
    private CpdlcMenuPanel cpdlcMenuPanel;

    public DashboardPanel(CpdlcService service, Client client) {
        this.service = service;
        this.client = client;
        this.cardLayout = new CardLayout();
        this.cardContainer = new JPanel(cardLayout);

        setupUI();
        service.addListener(this);
    }

    private void setupUI() {
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(createTopButtonPanel(), BorderLayout.NORTH);

        // Sub-panels
        listPanel = new MessageListPanel(service, this::handleMessageSelection);
        detailPanel = new MessageDetailPanel(service, () -> showCard("LIST"));
        
        cardContainer.add(listPanel, "LIST");
        cardContainer.add(detailPanel, "DETAIL");
        cardContainer.add(new TelexPanel(service, () -> showCard("LIST")), "TELEX");
        
        cpdlcMenuPanel = new CpdlcMenuPanel(service, () -> showCard("LIST"), this::showCard);
        cardContainer.add(cpdlcMenuPanel, "CPDLC_MENU");
        
        cardContainer.add(new LogonATCPanel(service, () -> showCard("CPDLC_MENU")), "LOGON_FORM");
        cardContainer.add(new ClearanceMenuPanel(service, () -> showCard("CPDLC_MENU"), this::showCard), "CLEARANCE_MENU");
        cardContainer.add(new PdcPanel(service, () -> showCard("LIST")), "PDC_FORM");
        cardContainer.add(new RequestPanel(service, () -> showCard("CPDLC_MENU"), () -> showCard("LIST")), "REQUEST");
        cardContainer.add(new ReportPanel(service, () -> showCard("CPDLC_MENU"), () -> showCard("LIST")), "REPORT");

        add(cardContainer, BorderLayout.CENTER);
        cardLayout.show(cardContainer, "LIST");
    }

    private void showCard(String cardName) {
        if ("LIST".equals(cardName)) {
            listPanel.clearSelection();
        }
        cardLayout.show(cardContainer, cardName);
    }

    private JPanel createTopButtonPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setPreferredSize(new Dimension(300, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.fill = GridBagConstraints.VERTICAL;

        // Disconnect
        PilotButton disc = new PilotButton("DISCONNECT");
        disc.setPreferredSize(new Dimension(120, 60));
        disc.setCustomColor(Color.darkGray, Color.WHITE);
        disc.addActionListener(e -> handleDisconnect());
        gbc.gridx = 0;
        p.add(disc, gbc);

        // Telex
        PilotButton telex = new PilotButton("TELEX");
        telex.setPreferredSize(new Dimension(120, 60));
        telex.setCustomColor(Color.darkGray, Color.WHITE);
        telex.addActionListener(e -> showCard("TELEX"));
        gbc.gridx = 1;
        p.add(telex, gbc);

        // CPDLC
        PilotButton cpdlc = new PilotButton("CPDLC");
        cpdlc.setPreferredSize(new Dimension(120, 60));
        cpdlc.setCustomColor(Color.darkGray, Color.WHITE);
        cpdlc.addActionListener(e -> showCard("CPDLC_MENU"));
        gbc.gridx = 2;
        p.add(cpdlc, gbc);

        // ZULU CLOCK
        gbc.gridx = 3;
        JPanel clockBox = new JPanel(new BorderLayout());
        clockBox.setBackground(new Color(30, 30, 30));
        clockBox.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        clockBox.setPreferredSize(new Dimension(100, 60));

        JLabel zuluLabel = new JLabel("00:00Z", SwingConstants.CENTER);
        zuluLabel.setForeground(Color.WHITE);
        zuluLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));
        clockBox.add(zuluLabel, BorderLayout.CENTER);
        p.add(clockBox, gbc);

        Timer zuluTimer = new Timer(1000, e -> {
            zuluLabel.setText(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm'Z'")));
        });
        zuluTimer.start();

        // Info box
        gbc.gridx = 4; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.BOTH;
        JPanel infoBox = new JPanel(new GridLayout(2, 1));
        infoBox.setBackground(new Color(30, 30, 30));
        infoBox.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        callsignLabel = new JLabel(service.getCallsign(), SwingConstants.RIGHT);
        callsignLabel.setForeground(Color.GREEN);
        callsignLabel.setFont(new Font(UI_FONT, Font.BOLD, 16));

        atsLabel = new JLabel("ATS: ----", SwingConstants.RIGHT);
        atsLabel.setForeground(Color.CYAN);
        atsLabel.setFont(new Font(UI_FONT, Font.PLAIN, 12));

        infoBox.add(callsignLabel);
        infoBox.add(atsLabel);
        p.add(infoBox, gbc);

        return p;
    }

    private void handleMessageSelection(AcarsMessage selected) {
        if (selected == null) return;
        detailPanel.setMessage(selected);
        showCard("DETAIL");
    }

    private void handleDisconnect() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "You will disconnect from Hoppie and your received messages will be lost.",
                "Warning", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            client.showLogin();
        }
    }

    @Override
    public void onMessageReceived(AcarsMessage message) {
        // Don't play sound for outgoing messages
        if (message.getFrom().equalsIgnoreCase(service.getCallsign())) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (message.getType().equalsIgnoreCase("system")) {
                SoundManager.playWarning();
            } else {
                SoundManager.playNotification();
            }
            alertNewMessage();
        });
    }

    @Override
    public void onMessagesUpdated(List<AcarsMessage> messages) {
        // Handled by listPanel directly as it also listens to service
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        SwingUtilities.invokeLater(() -> {
            callsignLabel.setForeground(isConnected ? Color.GREEN : Color.RED);
        });
    }

    @Override
    public void onAtsUnitChanged(String atsUnit) {
        SwingUtilities.invokeLater(() -> {
            atsLabel.setText("ATS: " + (atsUnit == null ? "----" : atsUnit));
            cpdlcMenuPanel.updateMenuState();
        });
    }

    @Override
    public void onError(String message) {
        // Could show a popup or log
    }

    private void alertNewMessage() {

        JFrame frame = client.frame;
        if (frame != null && !frame.isActive()) {
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                try {
                    Class<?> appClass = Class.forName("com.apple.eawt.Application");
                    Object application = appClass.getMethod("getApplication").invoke(null);
                    appClass.getMethod("requestUserAttention", boolean.class).invoke(application, true);
                } catch (Exception e) {
                    frame.toFront();
                }
            } else {
                frame.toFront();
            }
        }
    }
}
