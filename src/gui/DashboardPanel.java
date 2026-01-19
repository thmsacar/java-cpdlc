package gui;

import flight.Flight;
import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import hoppie.HoppieAPI;
import simbrief.SimbriefAPI;

import javax.swing.*;
import javax.swing.border.Border;
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
import java.util.prefs.Preferences;

public class DashboardPanel extends JPanel {
    
    private static final String UI_FONT = "Roboto Mono";

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

    //PDC Form text fields
    private JTextField stationField;
    private JTextField atisField;
    private JTextField typeField;
    private JTextField depField;
    private JTextField destField;
    private JTextField standField;

    private Preferences prefs = Preferences.userNodeForPackage(DashboardPanel.class);

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
        cardContainer.add(createLogonCard(), "LOGON_FORM");
        cardContainer.add(createClearenceCard(), "CLEARANCE");
        cardContainer.add(createPDCCard(), "PDC");

        add(cardContainer, BorderLayout.CENTER);

        cardLayout.show(cardContainer, "LIST");

        AcarsMessage connectionMsg = hoppieAPI.checkConnection(callsign);
        setConnectionStatus(connectionMsg.getMessage().startsWith("Connected"));
        addMessage(connectionMsg);

        changeATSUnit(null);

//CLD 1614 260119 LTAC PDC 001 @THY1GF@ CLRD TO @LTFM@ OFF @03C@ VIA @YAVRU1T@ SQUAWK @6445@ NEXT FREQ @129.425@ ATIS @B@, @QNH 1023@ DEP FREQ @129.425@ CLIMB VIA SID TO ALTITUDE @FL140@ IF YOU REQ. RWY CHG. CALL @129.425@ BEFORE ACCEPTING VIA DCL.
        addMessage(new CpdlcMessage("LTXX", "cpdlc", "RYR2GF", "CLD 1614 260119 LTAC PDC 001 @THY1GF@ CLRD TO @LTFM@ OFF @03C@ VIA @YAVRU1T@ SQUAWK @6445@ NEXT FREQ @129.425@ ATIS @B@, @QNH 1023@ DEP FREQ @129.425@ CLIMB VIA SID TO ALTITUDE @FL140@ IF YOU REQ. RWY CHG. CALL @129.425@ BEFORE ACCEPTING VIA DCL.", 1, -1, "WU"));
        addMessage(new CpdlcMessage("CMRM", "cpdlc", "RYR2GF", "MAINTAIN @FL370", 1, -1, "WU"));
        addMessage(new CpdlcMessage("CMRM", "cpdlc", "RYR2GF", "CURRENT ATC UNIT@_@CMRM@_@MADRID CTL@CURRENT ATC UNIT@_@CMRM@_@MADRID CTL", 1, -1, "NE"));
        addMessage(new AcarsMessage("THY2GF", "telex", "RYR2GF", "HELLO"));

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
        zuluLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));
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
        callsignLabel.setFont(new Font(UI_FONT, Font.BOLD, 16));

        //Current ATS
        atsLabel = new JLabel("ATS: " + currentATS, SwingConstants.RIGHT);
        atsLabel.setForeground(Color.CYAN);
        atsLabel.setFont(new Font(UI_FONT, Font.PLAIN, 12));

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

    private PilotButton createReturnButton(String returnTo) {
        PilotButton button = new PilotButton("← RETURN");
        button.addColorChangerOnPress();
        button.setFont(new Font("Monospaced", Font.BOLD, 12));
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        );
        button.setBorder(border);

        button.addActionListener(e -> {
            messageList.clearSelection();
            cardLayout.show(cardContainer, returnTo);
        });
        return button;
    }

    private JScrollPane createScrollPane(Component c) {
        JScrollPane scrollPane = new JScrollPane(c);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        return scrollPane;
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

    //---CARDS---

    private JPanel createListCard() {
        JPanel p = new JPanel(new BorderLayout());
        messageModel = new DefaultListModel<>();
        messageList = new JList<>(messageModel);

        //List renderer
        messageList.setCellRenderer(new DefaultListCellRenderer() {

            private final JPanel rendererPanel = new JPanel(new BorderLayout(10, 0));
            private final JLabel arrowLabel = new JLabel();
            private final JLabel textLabel = new JLabel();

            {
                rendererPanel.add(arrowLabel, BorderLayout.WEST);
                rendererPanel.add(textLabel, BorderLayout.CENTER);

                arrowLabel.setFont(new Font("Monospaced", Font.BOLD, 20));
                textLabel.setFont(FontManager.REGULAR.deriveFont(14f));

                rendererPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
            }

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {

                if (value instanceof AcarsMessage) {
                    AcarsMessage msg = (AcarsMessage) value;
                    String symbol = msg.getListFormat(callsign).get("symbol");
                    String entry = msg.getListFormat(callsign).get("entry");
                    arrowLabel.setText(symbol);
                    textLabel.setText(entry);
                }


                if (isSelected) {
                    rendererPanel.setOpaque(true);
                    rendererPanel.setBackground(new Color(45 , 45, 45));
                } else {
                    rendererPanel.setOpaque(false);
                    rendererPanel.setBackground(list.getBackground());

                }

                return rendererPanel;
            }

//            @Override
//            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
//                                                          boolean isSelected, boolean cellHasFocus) {
//
//                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
//
//                if (value instanceof AcarsMessage) {
//                    AcarsMessage msg = (AcarsMessage) value;
//                    String symbol = msg.getListFormat(callsign).get("symbol");
//                    String entry = msg.getListFormat(callsign).get("entry");
//                    label.setText(formatListLine(entry));
//                }
//
//                //Line border
//                label.setBorder(BorderFactory.createCompoundBorder(
//                        BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY), // Alt çizgi
//                        BorderFactory.createEmptyBorder(10, 10, 10, 10)               // Yazı etrafındaki boşluk
//                ));
//
//                //List when selected
//                if (isSelected) {
//                    label.setBackground(new Color(45, 45, 45));
//                } else {
//                    label.setBackground(list.getBackground());
//                }
//
//                return label;
//            }
        });


        //List selection listener
        messageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleMessageSelection(messageList.getSelectedValue());
            }
        });

        //Scroll pane
//        JScrollPane scrollPane = new JScrollPane(messageList);
//        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
//        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
//        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        JScrollPane scrollPane = createScrollPane(messageList);


//        scrollPane.setBorder(BorderFactory.createEmptyBorder());
//        scrollPane.setBackground(new Color(30, 30, 30));


        p.add(scrollPane, BorderLayout.CENTER);
        return p;
    }

    private JPanel createDetailCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));

        // Return
        PilotButton returnBtn = createReturnButton("LIST");
        p.add(returnBtn, BorderLayout.NORTH);

        // Detail text
        detailTextArea = new JTextArea();
        detailTextArea.setEditable(false);
        detailTextArea.setFocusable(false);
        detailTextArea.setFont(new Font(UI_FONT, Font.BOLD, 14));

        JScrollPane scrollPane = createScrollPane(detailTextArea);
        p.add(scrollPane, BorderLayout.CENTER);


//        p.add(new JScrollPane(detailTextArea), BorderLayout.CENTER);

        //TODO scroll pane olayina scroll bar ekle

        // Response
        responsePanel = createResponsePanel();

        p.add(responsePanel, BorderLayout.SOUTH);

        return p;
    }

    private JPanel createDetailNoResponseCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));

        // Return button
        PilotButton returnBtn = createReturnButton("LIST");
        p.add(returnBtn, BorderLayout.NORTH);

        detailTextAreaNoRes = new JTextArea();
        detailTextAreaNoRes.setEditable(false);
        detailTextAreaNoRes.setFocusable(false);

        detailTextAreaNoRes.setFont(new Font(UI_FONT, Font.BOLD, 14));

        JScrollPane scrollPane = createScrollPane(detailTextAreaNoRes);
        p.add(scrollPane, BorderLayout.CENTER);

//        p.add(new JScrollPane(detailTextAreaNoRes), BorderLayout.CENTER);

        return p;
    }

    private JPanel createTelexCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));

//       | RETURN
//       | FORM
//       | BUTTON

        // RETURN
        PilotButton returnBtn = createReturnButton("LIST");
        p.add(returnBtn, BorderLayout.NORTH);

        // FORM AREA
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);

        // --- Left side (station) ---
        JPanel stationContainer = new JPanel(new BorderLayout(0, 5));
        JLabel stationLabel = new JLabel("STATION");
        stationLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));
        JTextField stationField = new JTextField(15); //field for 15 chars
        stationField.setFont(new Font(UI_FONT, Font.PLAIN, 14));
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
        textLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));
        JTextArea messageArea = new JTextArea();

        messageArea.setFont(new Font(UI_FONT, Font.PLAIN, 14));
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
                sendTelex(stationField.getText().trim(), messageArea.getText().trim());
                stationField.setText("");
                messageArea.setText("");
                cardLayout.show(cardContainer, "LIST");
            }
        });

        return p;
    }

    private JPanel createLogonCard(){
        JPanel p = new JPanel(new BorderLayout(0, 10));

//       | RETURN
//       | FORM
//       | BUTTON

        // RETURN
        PilotButton returnBtn = createReturnButton("CPDLC");
        p.add(returnBtn, BorderLayout.NORTH);

        // FORM AREA
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);

        // --- Left side (station) ---
        JPanel stationContainer = new JPanel(new BorderLayout(0, 5));
        JLabel stationLabel = new JLabel("STATION");
        stationLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));
        JTextField stationField = new JTextField(15); //field for 15 chars
        stationField.setFont(new Font(UI_FONT, Font.PLAIN, 14));
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
        JLabel textLabel = new JLabel("REMARKS");
        textLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));
        JTextArea messageArea = new JTextArea();
        messageArea.setFont(new Font(UI_FONT, Font.PLAIN, 14));
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
        PilotButton sendBtn = new PilotButton("SEND LOGON");
        sendBtn.setPreferredSize(new Dimension(0, 40));
        sendBtn.setCustomColor(new Color(140, 80, 160), Color.WHITE);
        p.add(sendBtn, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> {
            if(!stationField.getText().trim().isEmpty()) {
                sendLogonAction(stationField.getText().trim(), messageArea.getText().trim());
                stationField.setText("");
                messageArea.setText("");
                cardLayout.show(cardContainer, "CPDLC");
            }
        });

        return p;
    }

    private JPanel createCpdlcCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));

//       | RETURN
//       | MENU

        // RETURN
        PilotButton returnBtn = createReturnButton("LIST");
        p.add(returnBtn, BorderLayout.NORTH);

        // FORM AREA
        JPanel menuPanel = createCpdlcMenuPanel();
        p.add(menuPanel, BorderLayout.CENTER);

        return p;
    }

    private JPanel createClearenceCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));

        PilotButton returnBtn = createReturnButton("CPDLC");
        p.add(returnBtn, BorderLayout.NORTH);

        JPanel clearenceMenu = new JPanel(new GridLayout(1, 2, 20, 20));
        clearenceMenu.setBackground(new Color(30, 30, 30));
        clearenceMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(50, 20, 50, 20)
        ));

        PilotButton btnDepartureClx = createCpdlcMenuButton("DEPARTURE CLX");
        PilotButton btnOceanicClx = createCpdlcMenuButton("OCEANIC CLX");

        btnDepartureClx.addActionListener(e -> handleDepartureForm());

        //Disable oceanic button to add as a future feature
        disableButton(btnOceanicClx);

        clearenceMenu.add(btnDepartureClx);
        clearenceMenu.add(btnOceanicClx);


        p.add(clearenceMenu, BorderLayout.CENTER);

        return p;
    }

    private JPanel createPDCCard(){
        JPanel p = new JPanel(new BorderLayout(0, 10));

//       | RETURN
//       | FORM
//       | BUTTON

        //RETURN
        PilotButton returnBtn = createReturnButton("CLEARANCE");
        p.add(returnBtn, BorderLayout.NORTH);

        //FORM
        JPanel formArea = createPDCFormArea();
        formArea.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JScrollPane scrollPane = createScrollPane(formArea);
        p.add(scrollPane, BorderLayout.CENTER);

        // BUTTON
        PilotButton requestBtn = new PilotButton("REQUEST CLEARANCE");
        requestBtn.setPreferredSize(new Dimension(0, 40));
        requestBtn.setCustomColor(new Color(0, 100, 150), Color.WHITE);
        p.add(requestBtn, BorderLayout.SOUTH);

        requestBtn.addActionListener(e -> sendPDCRequest());

        return p;
    }

    private JPanel createPDCFormArea() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        // --- Simbrief ID & Fetch Button ---
        JPanel simbriefRow = new JPanel(new BorderLayout(5, 0));
        simbriefRow.setOpaque(false);
        JTextField simbriefField = createStyledTextField("");
        PilotButton fetchBtn = new PilotButton("FETCH");
        fetchBtn.setPreferredSize(new Dimension(80, 30));
        fetchBtn.addActionListener(e -> {
            prefs.put("lastSimbriefID", simbriefField.getText());
            fetchFromSimbrief(simbriefField.getText());
        });
        simbriefField.setText(prefs.get("lastSimbriefID", ""));


        simbriefRow.add(simbriefField, BorderLayout.CENTER);
        simbriefRow.add(fetchBtn, BorderLayout.EAST);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(createLabeledComponent("SIMBRIEF ID", simbriefRow), gbc);

        // --- Station & Callsign ---
        gbc.gridwidth = 1; gbc.gridy = 1;
        panel.add(createLabeledComponent("STATION", stationField = createStyledTextField("")), gbc);
        gbc.gridx = 1;
        panel.add(createLabeledComponent("ATIS", atisField = createStyledTextField("")), gbc);

        // --- Departure & Destination ---
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(createLabeledComponent("DEPARTURE", depField = createStyledTextField("")), gbc);
        gbc.gridx = 1;
        panel.add(createLabeledComponent("DESTINATION", destField = createStyledTextField("")), gbc);

        // --- Aircraft Type & Stand ---
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(createLabeledComponent("ACFT TYPE", typeField = createStyledTextField("")), gbc);
        gbc.gridx = 1;
        panel.add(createLabeledComponent("STAND / GATE", standField = createStyledTextField("")), gbc);

        return panel;
    }

    private JPanel createLabeledComponent(String labelText, JComponent component) {
        JPanel container = new JPanel(new BorderLayout(0, 3));
        container.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font(UI_FONT, Font.BOLD, 12));
        label.setForeground(Color.LIGHT_GRAY);

        container.add(label, BorderLayout.NORTH);
        container.add(component, BorderLayout.CENTER);
        return container;
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font(UI_FONT, Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY),
                    BorderFactory.createEmptyBorder(0, 5, 0, 5)
                ));
        field.setPreferredSize(new Dimension(0, 30));

        ((AbstractDocument) field.getDocument()).setDocumentFilter(new UppercaseFilter());
        return field;
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

        btnLogonATC.addActionListener(e -> handleLogonATC());
        btnClearance.addActionListener(e -> handleClearence());

        updateMenuState();

        cpdlcMenu.add(btnClearance);
        cpdlcMenu.add(btnLogonATC);
        cpdlcMenu.add(btnRequest);
        cpdlcMenu.add(btnReport);

        return cpdlcMenu;
    }

    private PilotButton createCpdlcMenuButton(String text) {
        PilotButton btn = new PilotButton(text);
        btn.setFont(new Font(UI_FONT, Font.BOLD, 14));
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
            btnLogonATC.setForeground(RED_RESPONSE);

            //These button only works if logged on to ATC
            enableButton(btnRequest);
            enableButton(btnReport);
        } else { //We are not connected to an ATC
            //Change to LOGON button
            btnLogonATC.setForeground(new Color(180, 100, 200));
            btnLogonATC.setText("ATC LOGON");


            disableButton(btnRequest);
            disableButton(btnReport);
            //These button only works if logged on to ATC

        }
    }

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

    //To call when clicked DISCONNECT button
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

    //To call when clicked TELEX button
    private void handleTelex() {
        // Telex button
        cardLayout.show(cardContainer, "TELEX");
//        System.out.println("Loading telex menu...");
    }

    //To call when clicked CPDLC button
    private void handleCpdlc() {
        // CPDLC button
        cardLayout.show(cardContainer, "CPDLC");
    }

    //To call when clicked ATC LOGON button
    private void handleLogonATC() {
        if (!isLoggedOn) {
            // If not logged to ATC show form (get the station)
            cardLayout.show(cardContainer, "LOGON_FORM");
        } else {
            // If logged to ATC then logoff
            sendLogoffAction();
        }
    }

    //To call when clicked PDC/CLX button
    private void handleClearence() {
        cardLayout.show(cardContainer, "CLEARANCE");
    }

    private void handleDepartureForm(){
        cardLayout.show(cardContainer, "PDC");
    }

    private void sendTelex(String station, String message) {
        //Thread for networking
        new Thread(()->{
            AcarsMessage msg = hoppieAPI.sendTelex(station, callsign, message);
            setConnectionStatus(!msg.getType().equalsIgnoreCase("system"));
            addMessage(msg);
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

    private void sendPDCRequest() {
        //Fields to check
        JTextField[] fields = {stationField, atisField, typeField, depField, destField, standField};
        String[] names = {"STATION", "ATIS", "ACFT TYPE", "DEP", "DEST", "STAND"};

        // Check if fields empty
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getText().trim().isEmpty()) {
                fields[i].setBorder(BorderFactory.createLineBorder(Color.RED));
                fields[i].requestFocus();
                return;
            }
            fields[i].setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        }

        String station = stationField.getText().trim();
        String origin = depField.getText().trim();
        String destination = destField.getText().trim();
        String type = typeField.getText().trim();
        String stand = standField.getText().trim();
        String atis = atisField.getText().trim();

    //        System.out.println("======= PDC REQUEST DEBUG =======");
    //        System.out.println("STATION    : " + stationField.getText().trim());
    //        System.out.println("ATIS       : " + atisField.getText().trim());
    //        System.out.println("ACFT TYPE  : " + typeField.getText().trim());
    //        System.out.println("DEP / DEST : " + depField.getText().trim() + " / " + destField.getText().trim());
    //        System.out.println("STAND      : " + standField.getText().trim());
    //
    //        System.out.println("=================================");

        Flight flight = new Flight(callsign, origin, destination, type);
        AcarsMessage msg = hoppieAPI.sendPdcRequest(station, flight, stand, atis);
        addMessage(msg);
        if(msg.getType().equalsIgnoreCase("system")) messageList.setSelectedIndex(0);
        cardLayout.show(cardContainer, "LIST");
    }

    private void sendLogonAction(String targetStation, String remarks) {
        AcarsMessage msg = hoppieAPI.sendLogonATC(targetStation, callsign, remarks);
        if (!msg.getType().equalsIgnoreCase("system")) {
            this.pendingLogonStation = targetStation.trim();
            setConnectionStatus(true);
        }
        addMessage(msg);

        System.out.println("Logon request sent to: " + targetStation + ". Waiting for acceptance...");
    }

    private void sendLogoffAction() {
        AcarsMessage msg = hoppieAPI.sendLogoffATC(currentATS, callsign);
        if (!msg.getType().equalsIgnoreCase("system")) {
            changeATSUnit(null);
            setConnectionStatus(true);
        }
        addMessage(msg);
    }

    private void sendResponseAndReturn(String response, CpdlcMessage originalMsg) {
        //Thread for networking
        new Thread(() -> {
            AcarsMessage acarsMsg = null;
            switch (response) {
                case "WILCO": acarsMsg = hoppieAPI.wilco(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                case "UNABLE": acarsMsg = hoppieAPI.unable(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                case "ROGER": acarsMsg = hoppieAPI.roger(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                case "STANDBY": acarsMsg = hoppieAPI.standby(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                case "AFFIRM": acarsMsg = hoppieAPI.affirm(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
                case "NEGATIVE": acarsMsg = hoppieAPI.negative(originalMsg.getFrom(), this.callsign, originalMsg.getMsgNumber()); break;
            }
            addMessage(acarsMsg);
            messageList.clearSelection();
            cardLayout.show(cardContainer, "LIST");
        }).start();
    }

    private void checkLogonAccepted(AcarsMessage msg) {
        String sender = msg.getFrom(); // ATS sending
        String text = msg.getMessage();
        if (text.contains("LOGON ACCEPTED") && sender.equalsIgnoreCase(pendingLogonStation)) {
            changeATSUnit(pendingLogonStation);
            pendingLogonStation = "";
        }
    }

    private void startAutoFetch() {
        fetcherService = Executors.newSingleThreadScheduledExecutor();
        fetcherService.scheduleAtFixedRate(() -> {
            List<AcarsMessage> newMessages = hoppieAPI.fetchMessages(this.callsign);
            if (!newMessages.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    boolean soundPlayed = false;
                    for (AcarsMessage msg : newMessages) {
                        AcarsMessage lastMsg = messageModel.get(0);
                        //If a system (trouble) message has already been added to list and notified, there is no need to repeat it.
                        if(msg.getType().equalsIgnoreCase("system") && msg.getMessage().equalsIgnoreCase(lastMsg.getMessage())) {
                            continue;
                        }
                        System.out.println("Adding msg "+msg);
                        checkLogonAccepted(msg);
                        addMessage(msg);
                        setConnectionStatus(!msg.getType().equalsIgnoreCase("system"));
                        if (!soundPlayed) {
                            if (msg.getType().equalsIgnoreCase("system")) {
                                SoundManager.playWarning();
                                alertNewMessage(); // Request focus
                            } else {
                                SoundManager.playNotification();
                                alertNewMessage();
                            }
                            soundPlayed = true; // Sound already played for this batch
                        }
                    }
                    if (!messageModel.isEmpty()) {
                        messageList.setSelectedIndex(0);
                    }
                });
            }else {setConnectionStatus(true);} //If no messages are returned that means fetching was successful
        }, 0, 40, TimeUnit.SECONDS);
    }

    private void stopFetcher() {
        if (fetcherService != null && !fetcherService.isShutdown()) {
            System.out.println("Stopping fetcher...");
            fetcherService.shutdownNow();
        }
    }

    private void fetchFromSimbrief(String simbriefID){
        System.out.println("Fetching " + simbriefID);
        if (simbriefID.trim().isEmpty()) return;
        new Thread(() -> {
            SimbriefAPI simbriefAPI = new SimbriefAPI(simbriefID);
            try {
                Flight flight = simbriefAPI.getFlight();
                System.out.println(flight);
                depField.setText(flight.getOrigin());
                destField.setText(flight.getDestination());
                typeField.setText(flight.getAircraft());
            } catch (IOException e) {
                addMessage(new AcarsMessage("system", "ERROR: "+e.getMessage()));
                messageList.setSelectedIndex(0);
            }
        }).start();
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
                        appClass.getMethod("requestUserAttention", boolean.class).invoke(application, true);
                    } catch (Exception e) {
                        frame.toFront();
                        frame.requestFocus();
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

    private void addMessage(AcarsMessage message) {
        SwingUtilities.invokeLater(() -> {
            if (message!=null) messageModel.add(0, message);
        });

    }
}