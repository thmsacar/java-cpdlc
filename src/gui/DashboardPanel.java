package gui;

import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import hoppie.HoppieAPI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DashboardPanel extends JPanel {

    private static final int LIST_CHARACTER_LIMIT = 70;
    private static final Color GREEN_RESPONSE = new Color(0, 150, 0);
    private static final Color RED_RESPONSE = new Color(200 , 0, 0);
    private static final Color YELLOW_RESPONSE = new Color(200, 150, 50);
    private static final Color BLUE_RESPONSE = new Color(0, 100, 150);
    private static final Color DISABLED_BUTTON_COLOR = Color.GRAY;
    private static final Color ENABLED_BUTTON_COLOR = Color.WHITE;


    //GUI main class / client
    private final Client client;

    //Backend utils
    private final String callsign;
    private final HoppieAPI hoppieAPI;
    private ScheduledExecutorService fetcherService;

    //Callsign area
    private JLabel callsignLabel;
    private JLabel atsLabel;

    //ATS connection status variables
    private String currentATS;
    private boolean isLoggedOn = false;
    private String pendingLogonStation = "";

    //Message list
    private DefaultListModel<AcarsMessage> messageModel;
    private JList<AcarsMessage> messageList;

    private final CardLayout cardLayout;
    private final JPanel cardContainer;

    //Detail screen
    private JTextArea detailTextArea;
    private JTextArea detailTextAreaNoRes;

    //Response Panel
    private JPanel responsePanel;
    private PilotButton wilcoBtn;
    private PilotButton unableBtn;
    private PilotButton affirmBtn;
    private PilotButton negativeBtn;
    private PilotButton rogerBtn;
    private PilotButton standbyBtn;

    //CPDLC Menu buttons
    private PilotButton btnClearance;
    private PilotButton btnLogonATC;
    private PilotButton btnRequest;
    private PilotButton btnReport;


    public DashboardPanel(String callsign, String hoppieID, Client client) {
        this.callsign = callsign;
        this.hoppieAPI = new HoppieAPI(hoppieID);
        this.client = client;

        this.cardLayout = new CardLayout();
        this.cardContainer = new JPanel(cardLayout);

        setupUI();
        startAutoFetch();

        this.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (!this.isShowing()) {
                    stopFetcher();
                }
            }
        });

    }

    private void setupUI() {
        // Layout
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Buttons
        add(createTopButtonPanel(), BorderLayout.NORTH);

        // Cards
        cardContainer.add(createListCard(), "LIST");
        cardContainer.add(createDetailCard(), "DETAIL");
        cardContainer.add(createDetailNoResponseCard(), "DETAIL_NO_RES");
        cardContainer.add(createTelexCard(), "TELEX");
        cardContainer.add(createCpdlcCard(), "CPDLC");

        add(cardContainer, BorderLayout.CENTER);

        cardLayout.show(cardContainer, "LIST");


        //TODO acars message ile çözülebilir mi??
        try {
            HoppieAPI.HoppieResponse response = hoppieAPI.sendPing(callsign);
            if (response.body().equalsIgnoreCase("ok")) {
                addMessage(new AcarsMessage("system", "Connected as " + callsign));
                setConnectionStatus(true);
            }else{
                addMessage(new AcarsMessage("system", "ERROR: " + response.body()));
                setConnectionStatus(false);
            }
        } catch (IOException e) {
            addMessage(new AcarsMessage("system", "ERROR: " + e));
            setConnectionStatus(false);
        }

        changeATSUnit("LTBB");

        addMessage(new CpdlcMessage("LTBB", "cpdlc", "THY1GF", "DIRECT ASMAP", 1, -1, "WU"));
        addMessage(new CpdlcMessage("LTBB", "cpdlc", "THY1GF", "CONTACT LGAA 132.150", 1, -1, "R"));
        addMessage(new AcarsMessage("THY2GF", "telex", "THY1GF", "HELLO"));

    }

    private JPanel createTopButtonPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setPreferredSize(new Dimension(300, 60));
        GridBagConstraints gbc = new GridBagConstraints();

        // Butonlar için ortak ayarlar
        gbc.insets = new Insets(0, 5, 0, 5); // Aralarda 5px boşluk
        gbc.fill = GridBagConstraints.VERTICAL; // Butonlar dikeyde tam boy olsun
        gbc.weightx = 0; // Butonlar esnemesin

        //Disconnect Button
        PilotButton disc = new PilotButton("DISCONNECT");
        disc.setPreferredSize(new Dimension(120, 60));
        disc.setCustomColor(Color.darkGray, Color.WHITE);
        disc.addActionListener(e -> handleDisconnect(client));
        gbc.gridx = 0;
        p.add(disc, gbc);

        //Telex Button
        PilotButton telex = new PilotButton("TELEX");
        telex.setPreferredSize(new Dimension(120, 60));
        telex.setCustomColor(Color.darkGray, Color.WHITE);
        telex.addActionListener(e -> handleTelex());
        gbc.gridx = 1;
        p.add(telex, gbc);

        //CPDLC Button
        PilotButton cpdlc = new PilotButton("CPDLC");
        cpdlc.setPreferredSize(new Dimension(120, 60));
        cpdlc.setCustomColor(Color.darkGray, Color.WHITE);
        cpdlc.addActionListener(e -> handleCpdlc());
        gbc.gridx = 2;
        p.add(cpdlc, gbc);

        //ZULU CLOCK
        gbc.gridx = 3;
        JPanel clockBox = new JPanel(new BorderLayout());
        clockBox.setBackground(new Color(30, 30, 30));
        clockBox.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        Dimension clockDim = new Dimension(100, 60);
        clockBox.setPreferredSize(clockDim);
        clockBox.setMinimumSize(clockDim);

        JLabel zuluLabel = new JLabel("00:00Z", SwingConstants.CENTER);
        zuluLabel.setForeground(Color.WHITE);
        zuluLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        clockBox.add(zuluLabel, BorderLayout.CENTER);
        p.add(clockBox, gbc);

        // --- ZULU TIMER ---
        Timer zuluTimer = new Timer(1000, e -> {
            ZonedDateTime nowUTC = ZonedDateTime.now(ZoneOffset.UTC);
            zuluLabel.setText(nowUTC.format(DateTimeFormatter.ofPattern("HH:mm'Z'")));
        });
        zuluTimer.start();

        //Callsign grid
        gbc.gridx = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;


        JPanel infoBox = new JPanel(new GridLayout(2, 1));
        infoBox.setBackground(new Color(30, 30, 30));
        infoBox.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        //Callsign
        callsignLabel = new JLabel(this.callsign, SwingConstants.RIGHT);
        callsignLabel.setForeground(Color.GREEN); // Uçak kokpiti ekranı rengi
        callsignLabel.setFont(new Font("Monospaced", Font.BOLD, 16));

        //Current ATS
        atsLabel = new JLabel("ATS: " + currentATS, SwingConstants.RIGHT);
        atsLabel.setForeground(Color.CYAN);
        atsLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));

        infoBox.add(callsignLabel);
        infoBox.add(atsLabel);

        p.add(infoBox, gbc);

        return p;
    }

    public void changeATSUnit(String ats){
        this.currentATS = ats;
        atsLabel.setText("ATS: " + (currentATS==null?"----":currentATS));

        isLoggedOn = (currentATS!=null);

        updateMenuState();
    }

    private PilotButton createReturnButton() {
        PilotButton button = new PilotButton("← RETURN");
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        button.addActionListener(e -> {
            messageList.clearSelection();
            cardLayout.show(cardContainer, "LIST");
        });
        return button;
    }

    private JPanel createResponsePanel() {
        JPanel panel = new JPanel(new GridLayout(1,3, 5, 0));
        panel.setPreferredSize(new Dimension(0, 40));


        wilcoBtn = new PilotButton("* WILCO");
        unableBtn = new PilotButton("* UNABLE");
        standbyBtn = new PilotButton("* STANDBY");
        affirmBtn = new PilotButton("* AFFIRM");
        negativeBtn = new PilotButton("* NEGATIVE");
        rogerBtn = new PilotButton("* ROGER");

        wilcoBtn.addActionListener(e -> {
            if (messageList.getSelectedValue() instanceof CpdlcMessage) {
                sendResponseAndReturn("WILCO", (CpdlcMessage) messageList.getSelectedValue());
            }
        });

        unableBtn.addActionListener(e -> {
            if (messageList.getSelectedValue() instanceof CpdlcMessage) {
                sendResponseAndReturn("UNABLE", (CpdlcMessage) messageList.getSelectedValue());
            }
        });

        standbyBtn.addActionListener(e -> {
            if (messageList.getSelectedValue() instanceof CpdlcMessage) {
                sendResponseAndReturn("STANDBY", (CpdlcMessage) messageList.getSelectedValue());
            }
        });

        affirmBtn.addActionListener(e -> {
            if (messageList.getSelectedValue() instanceof CpdlcMessage) {
                sendResponseAndReturn("AFFIRM", (CpdlcMessage) messageList.getSelectedValue());
            }
        });

        negativeBtn.addActionListener(e -> {
            if (messageList.getSelectedValue() instanceof CpdlcMessage) {
                sendResponseAndReturn("NEGATIVE", (CpdlcMessage) messageList.getSelectedValue());
            }
        });

        rogerBtn.addActionListener(e -> {
            if (messageList.getSelectedValue() instanceof CpdlcMessage) {
                sendResponseAndReturn("ROGER", (CpdlcMessage) messageList.getSelectedValue());
            }
        });

        wilcoBtn.setBackground(GREEN_RESPONSE);
        unableBtn.setBackground(RED_RESPONSE);
        standbyBtn.setBackground(YELLOW_RESPONSE);
        affirmBtn.setBackground(GREEN_RESPONSE);
        negativeBtn.setBackground(RED_RESPONSE);
        rogerBtn.setBackground(BLUE_RESPONSE);

        panel.add(wilcoBtn);
        panel.add(unableBtn);
        panel.add(standbyBtn);

        return panel;
    }

    private JPanel createListCard() {
        JPanel p = new JPanel(new BorderLayout());
        messageModel = new DefaultListModel<>();
        messageList = new JList<>(messageModel);
        messageList.setFont(new Font("Monospaced", Font.PLAIN, 14));

        //List renderer
        messageList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof AcarsMessage) {
                    AcarsMessage msg = (AcarsMessage) value;
                    String rawLine = msg.getListFormat(callsign);
                    label.setText(formatListLine(rawLine));
                }

                //Line border
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY), // Alt çizgi
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)               // Yazı etrafındaki boşluk
                ));

                //List when selected
                if (isSelected) {
                    label.setBackground(new Color(45, 45, 45));
                } else {
                    label.setBackground(list.getBackground());
                }

                return label;
            }
        });


        //List selection listener
        messageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleMessageSelection(messageList.getSelectedValue());
            }
        });

        //Scroll pane
        JScrollPane scrollPane = new JScrollPane(messageList);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

//        scrollPane.setBorder(BorderFactory.createEmptyBorder());
//        scrollPane.setBackground(new Color(30, 30, 30));


        p.add(scrollPane, BorderLayout.CENTER);
        return p;
    }

    private JPanel createDetailCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));

        // Return
        PilotButton returnBtn = createReturnButton();
        p.add(returnBtn, BorderLayout.NORTH);

        // Detail text
        detailTextArea = new JTextArea();
        detailTextArea.setEditable(false);
        detailTextArea.setFont(new Font("Monospaced", Font.BOLD, 14));
        p.add(new JScrollPane(detailTextArea), BorderLayout.CENTER);

        // Response
        responsePanel = createResponsePanel();

        p.add(responsePanel, BorderLayout.SOUTH);

        return p;
    }

    private JPanel createDetailNoResponseCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));

        // Return button
        PilotButton returnBtn = createReturnButton();
        p.add(returnBtn, BorderLayout.NORTH);

        detailTextAreaNoRes = new JTextArea();
        detailTextAreaNoRes.setEditable(false);
        detailTextAreaNoRes.setFont(new Font("Monospaced", Font.BOLD, 14));

        p.add(new JScrollPane(detailTextAreaNoRes), BorderLayout.CENTER);

        return p;
    }

    private JPanel createTelexCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));

//       | RETURN
//       | FORM
//       | BUTTON

        // RETURN
        PilotButton returnBtn = createReturnButton();
        p.add(returnBtn, BorderLayout.NORTH);

        // FORM AREA
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);

        // --- Left side (station) ---
        JPanel stationContainer = new JPanel(new BorderLayout(0, 5));
        JLabel stationLabel = new JLabel("STATION");
        stationLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        JTextField stationField = new JTextField(15); //field for 15 chars
        stationField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        stationField.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        stationField.setPreferredSize(new Dimension(200, 30));
        stationField.setMinimumSize(stationField.getPreferredSize());


        ((AbstractDocument) stationField.getDocument())
                .setDocumentFilter(new UppercaseFilter());

        stationContainer.add(stationLabel, BorderLayout.NORTH);
        stationContainer.add(stationField, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        formPanel.add(stationContainer, gbc);

        // --- Right side (message) ---
        gbc.fill = GridBagConstraints.BOTH;

        JPanel messageContainer = new JPanel(new BorderLayout(0, 5));
        JLabel textLabel = new JLabel("MESSAGE");
        textLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        JTextArea messageArea = new JTextArea();
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        messageArea.setLineWrap(true);

        ((AbstractDocument) messageArea.getDocument())
                .setDocumentFilter(new UppercaseFilter());

        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        messageContainer.add(textLabel, BorderLayout.NORTH);
        messageContainer.add(scrollPane, BorderLayout.CENTER);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(messageContainer, gbc);

        p.add(formPanel, BorderLayout.CENTER);

        // BUTTON
        PilotButton sendBtn = new PilotButton("SEND TELEX");
        sendBtn.setPreferredSize(new Dimension(0, 40));
        sendBtn.setCustomColor(new Color(0, 100, 150), Color.WHITE);
        p.add(sendBtn, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> {
            if(!stationField.getText().trim().isEmpty() && !messageArea.getText().trim().isEmpty()) {
                sendTelex(stationField.getText(), messageArea.getText());
                stationField.setText("");
                messageArea.setText("");
                cardLayout.show(cardContainer, "LIST");
            }
        });

        return p;
    }

    private JPanel createCpdlcCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));

//       | RETURN
//       | MENU

        // RETURN
        PilotButton returnBtn = createReturnButton();
        p.add(returnBtn, BorderLayout.NORTH);

        // FORM AREA
        JPanel menuPanel = createCpdlcMenuPanel();
        p.add(menuPanel, BorderLayout.CENTER);

        return p;
    }

    private JPanel createCpdlcMenuPanel(){
        JPanel cpdlcMenu = new JPanel(new GridLayout(2, 2, 20, 20)); // 2x2 Izgara
        cpdlcMenu.setBackground(new Color(30, 30, 30));
        cpdlcMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(20  , 20, 20, 20)
        ));

        btnClearance = createCpdlcMenuButton("PDC / CLX");
        btnLogonATC = createCpdlcMenuButton("ATC LOGON");
        btnRequest = createCpdlcMenuButton("REQUEST");
        btnReport = createCpdlcMenuButton("REPORT");

        updateMenuState();

        cpdlcMenu.add(btnClearance);
        cpdlcMenu.add(btnLogonATC);
        cpdlcMenu.add(btnRequest);
        cpdlcMenu.add(btnReport);

        return cpdlcMenu;
    }

    private PilotButton createCpdlcMenuButton(String text) {
        PilotButton btn = new PilotButton(text);
        btn.setFont(new Font("Monospaced", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        return btn;
    }

    //Updates to the relevant menu state according to ATC LOGON status (isLoggedOn)
    private void updateMenuState() {
        if (isLoggedOn) { //We are connected to an ATC
            //Change to LOGOFF button
            btnLogonATC.setText("LOGOFF " + currentATS);
            btnLogonATC.setForeground(Color.RED);

            //These button only works if logged on to ATC
            enableButton(btnRequest);
            enableButton(btnReport);
        } else { //We are not connected to an ATC
            //Change to LOGON button
            btnLogonATC.setText("ATC LOGON");
            btnLogonATC.setForeground(Color.MAGENTA);

            //These button only works if logged on to ATC
            disableButton(btnRequest);
            disableButton(btnReport);
        }
    }

    //TODO incelenip silinebilir
//    private void setCPDLCMenuButtons(){
//        if (!isLoggedOn){
//            btnLogonATC.setText("ATC LOGON");
//            disableButton(btnRequest);
//            disableButton(btnReport);
//        }else {
//            btnLogonATC.setText("LOGOFF");
//            enableButton(btnRequest);
//            enableButton(btnReport);
//        }
//    }

    //Disables button interaction and changes color
    private void disableButton(JButton button){
        button.setForeground(DISABLED_BUTTON_COLOR);
        button.setEnabled(false);
    }

    //Enables button interaction and changes color
    private void enableButton(JButton button){
        button.setForeground(ENABLED_BUTTON_COLOR);
        button.setEnabled(true);
    }

    private void handleDisconnect(Client client) {

        //Warning window
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "You will disconnect from Hoppie and your received messages will be lost.",
                "Warning",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            messageModel.clear(); // Clear data
            stopFetcher(); //Stop fetching data
            client.showLogin();    // Return login page
        }
    }

    private void handleTelex() {
        // Telex button
        cardLayout.show(cardContainer, "TELEX");
//        System.out.println("Loading telex menu...");
    }

    private void handleCpdlc() {
        // CPDLC button
        System.out.println("Loading CPDLC menu...");
        cardLayout.show(cardContainer, "CPDLC");
    }

    //TODO Hoppie response mantığı değişmeli
    private void sendTelex(String station, String message) {
        //Thread for networking
        new Thread(()->{
            AcarsMessage msg = null;
            try {
//                System.out.println("Sending msg: " +station+ "|" +message);
                HoppieAPI.HoppieResponse response = hoppieAPI.sendTelex(station, callsign, message);
                if (response.body().equalsIgnoreCase("ok")){
                    msg = new AcarsMessage(callsign, "telex", station, message);
//                    System.out.println("SENT" + msg);
                    setConnectionStatus(true);
                }else {
                    msg = new AcarsMessage("system", "ERROR: " + response.body());
//                    System.out.println("SENT" + msg);
                    setConnectionStatus(false);

                }
            } catch (IOException e) {
                msg = new AcarsMessage("system", "ERROR: " + e.getMessage());
//                System.out.println("SENT" + msg);
                setConnectionStatus(false);
            }finally {
//                System.out.println("Adding to list");
                addMessage(msg);
            }
        }).start();
    }

    private void handleMessageSelection(AcarsMessage selected) {
        if (selected == null) return;

        boolean isOurMessage = selected.getFrom().equalsIgnoreCase(this.callsign);
        boolean isCpdlc = "cpdlc".equalsIgnoreCase(selected.getType());

        boolean showResponseButtons = isCpdlc && !((CpdlcMessage)selected).getResponseType().equalsIgnoreCase("NE") && !isOurMessage;

        if (showResponseButtons) {
            CpdlcMessage cpdlc = (CpdlcMessage) selected;
            String resType = cpdlc.getResponseType(); // WU, AN, R vs.

            responsePanel.removeAll();

            if ("WU".equalsIgnoreCase(resType)) {
                responsePanel.add(wilcoBtn);
                responsePanel.add(unableBtn);
                responsePanel.add(standbyBtn);
            }else if ("R".equalsIgnoreCase(resType)) {
                responsePanel.add(rogerBtn);
                responsePanel.add(standbyBtn);
//            } else if ("AN".equalsIgnoreCase(resType)) {
            }else {
                responsePanel.add(affirmBtn);
                responsePanel.add(negativeBtn);
                responsePanel.add(standbyBtn);
            }

            detailTextArea.setText(selected.getDetailFormat(this.callsign));
            cardLayout.show(cardContainer, "DETAIL");
        } else {
            detailTextAreaNoRes.setText(selected.getDetailFormat(this.callsign));
            cardLayout.show(cardContainer, "DETAIL_NO_RES");
        }
    }

    //TODO Hoppie response mantığı değişmeli
    private void sendResponseAndReturn(String response, CpdlcMessage originalMsg) {
        //Thread for networking
        new Thread(() -> {
            AcarsMessage msg;
            try {
//                System.out.println("Sending msg: " + originalMsg.getFrom()+ "|" +response);
                int cpdlcNumber = hoppieAPI.getCpdlcCounter();
                HoppieAPI.HoppieResponse httpResponse = null;
                switch (response) {
                    case "WILCO": httpResponse = hoppieAPI.wilco(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                    case "UNABLE": httpResponse = hoppieAPI.unable(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                    case "ROGER": httpResponse = hoppieAPI.roger(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                    case "STANDBY": httpResponse = hoppieAPI.standby(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                    case "AFFIRM": httpResponse = hoppieAPI.affirm(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                    case "NEGATIVE": httpResponse = hoppieAPI.negative(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                }

                //TODO hoppie response
                if (httpResponse.body()!=null && httpResponse.body().trim().equalsIgnoreCase("ok")){
                    msg = new CpdlcMessage(this.callsign, "cpdlc", originalMsg.getFrom(), response, cpdlcNumber, originalMsg.getMsgNumber(), "N");
                    setConnectionStatus(true);
                }else {
                    msg = new AcarsMessage("system", "ERROR: " + httpResponse.body());
                    setConnectionStatus(false);
                }
                AcarsMessage finalMsg1 = msg;
                SwingUtilities.invokeLater(() -> {
                    messageList.clearSelection();
                    addMessage(finalMsg1);
                    cardLayout.show(cardContainer, "LIST");
                });

            } catch (Exception ex) {
                //TODO hoppie response
                msg = new AcarsMessage("system", "ERROR: " + ex.getMessage());
                setConnectionStatus(false);
                AcarsMessage finalMsg = msg;
                SwingUtilities.invokeLater(() -> {
                    messageList.clearSelection();
                    addMessage(finalMsg);
                    cardLayout.show(cardContainer, "LIST");
                });
            }
        }).start();
    }

//    TODO HoppieResponse burada incelemek yerine direkt hata mesajı veya normal mesajla AcarsMessage dönse???
//    private void sendLogonAction(String targetStation, String remarks) {
//        this.pendingLogonStation = targetStation;
//
//        try {
//            HoppieAPI.HoppieResponse response = hoppieAPI.sendLogonATC(targetStation, callsign, remarks);
//            if (response.body().trim().equalsIgnoreCase("ok")){
//                addMessage(new AcarsMessage(this.callsign, "cpdlc", ));
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        System.out.println("Logon request sent to: " + targetStation + ". Waiting for acceptance...");
//    }

    private void startAutoFetch() {
        fetcherService = Executors.newSingleThreadScheduledExecutor();

        fetcherService.scheduleAtFixedRate(() -> {
            try {
//                System.out.println("Fetching data...");
                List<AcarsMessage> newMessages = hoppieAPI.fetchMessages(this.callsign);
//                System.out.println("Fetched " + newMessages.size() + " messages");
                if (!newMessages.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        for (AcarsMessage msg : newMessages) {
                            System.out.println("Adding msg"+msg);
                            //LOGON ACCEPTED
                            //  TODO Burada ATS unit degisme olayı olacak ama once CPDLC mesajlarının hoppieresponse dönmesi olayını çözmek lazım her yerde kullandık bunu
//                            if(msg.getMessage().contains("LOGON ACCEPTED")){
//                                //Change ATS unit
//                                changeATSUnit(msg.);
//
//                            }
                            addMessage(msg);
                        }
                        messageList.setSelectedIndex(0);
//                        handleMessageSelection(messageList.getSelectedValue());
                        SoundManager.playNotification();
                        alertNewMessage();
//                        System.out.println(newMessages.size() + " new messages received");
                    });
                }
                callsignLabel.setForeground(Color.GREEN);
            } catch (Exception e) {
                addMessage(new AcarsMessage("system", "ERROR: " + e.getMessage()));
                callsignLabel.setForeground(Color.RED);
            }
        }, 0, 40, TimeUnit.SECONDS);
    }

    private void stopFetcher() {
        if (fetcherService != null && !fetcherService.isShutdown()) {
            System.out.println("Stopping fetcher...");
            fetcherService.shutdownNow();
        }
    }

    private void alertNewMessage() {
        JFrame frame = client.frame;
        if (frame != null) {
            if (!frame.isActive()) {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("mac")) {
                    try {
                        Class<?> appClass = Class.forName("com.apple.eawt.Application");
                        Object application = appClass.getMethod("getApplication").invoke(null);
                        //TODO requestUserAttention method ile ilgili bi problem var??
                        appClass.getMethod("requestUserAttention", int.class).invoke(application, 1);
                        appClass.getMethod("setDockIconBadge", String.class).invoke(application, "!");

                    } catch (Exception e) {
                        frame.toFront();
                    }
                } else {
                    frame.toFront();
                    frame.requestFocus();
                }
            }
        }
    }

    private void setConnectionStatus(boolean isConnected){
        if (isConnected) {callsignLabel.setForeground(Color.GREEN);}
        else {callsignLabel.setForeground(Color.RED);}
    }

    //Avoids list line overflow
    private String formatListLine(String text) {
        if (text == null) return "";
        int limit = LIST_CHARACTER_LIMIT;
        if (text.length() > limit) {
            return text.substring(0, limit) + "...";
        }
        return text;
    }

    public void addMessage(AcarsMessage message) {
        SwingUtilities.invokeLater(() -> messageModel.add(0, message));

    }
}