package gui;

import gui.button.PilotButton;
import gui.button.ReturnButton;
import hoppie.AcarsMessage;
import hoppie.CpdlcMessage;
import service.CpdlcService;

import javax.swing.*;
import java.awt.*;

public class MessageDetailPanel extends JPanel {

    private final CpdlcService service;
    private final Runnable onBack;
    
    private final JTextArea detailTextArea;
    private final JPanel responsePanel;
    
    private final PilotButton wilcoBtn;
    private final PilotButton unableBtn;
    private final PilotButton affirmBtn;
    private final PilotButton negativeBtn;
    private final PilotButton rogerBtn;
    private final PilotButton standbyBtn;

    private static final Color GREEN_RESPONSE = new Color(0, 150, 0);
    private static final Color RED_RESPONSE = new Color(200 , 0, 0);
    private static final Color YELLOW_RESPONSE = new Color(200, 150, 50);
    private static final Color BLUE_RESPONSE = new Color(0, 100, 150);

    public MessageDetailPanel(CpdlcService service, Runnable onBack) {
        this.service = service;
        this.onBack = onBack;
        
        this.detailTextArea = new JTextArea();
        this.responsePanel = new JPanel(new GridLayout(1, 3, 5, 0));
        
        this.wilcoBtn = createResponseButton("* WILCO", "WILCO", GREEN_RESPONSE);
        this.unableBtn = createResponseButton("* UNABLE", "UNABLE", RED_RESPONSE);
        this.standbyBtn = createResponseButton("* STANDBY", "STANDBY", YELLOW_RESPONSE);
        this.affirmBtn = createResponseButton("* AFFIRM", "AFFIRM", GREEN_RESPONSE);
        this.negativeBtn = createResponseButton("* NEGATIVE", "NEGATIVE", RED_RESPONSE);
        this.rogerBtn = createResponseButton("* ROGER", "ROGER", BLUE_RESPONSE);

        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout(0, 10));

        // Return
        PilotButton returnBtn = new ReturnButton();
        returnBtn.addActionListener(e -> onBack.run());
        add(returnBtn, BorderLayout.NORTH);

        // Detail text
        detailTextArea.setEditable(false);
        detailTextArea.setFocusable(false);
        detailTextArea.setFont(new Font(DashboardPanel.UI_FONT, Font.BOLD, 14));
        detailTextArea.setLineWrap(true);
        detailTextArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(detailTextArea);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        add(scrollPane, BorderLayout.CENTER);

        responsePanel.setPreferredSize(new Dimension(0, 40));
        add(responsePanel, BorderLayout.SOUTH);
    }

    private PilotButton createResponseButton(String label, String responseType, Color bg) {
        PilotButton btn = new PilotButton(label);
        btn.setBackground(bg);
        btn.addActionListener(e -> {
            if (currentMessage instanceof CpdlcMessage) {
                CpdlcMessage cpdlc = (CpdlcMessage) currentMessage;
                if (cpdlc.hasBeenReplied()) return;
                service.sendResponse(responseType, cpdlc);
                onBack.run();
            }
        });
        return btn;
    }

    private PilotButton getButtonForResponse(String responseType) {
        if (responseType == null) return null;
        switch (responseType.toUpperCase()) {
            case "WILCO": return wilcoBtn;
            case "UNABLE": return unableBtn;
            case "STANDBY": return standbyBtn;
            case "AFFIRM": return affirmBtn;
            case "NEGATIVE": return negativeBtn;
            case "ROGER": return rogerBtn;
            default: return null;
        }
    }

    private AcarsMessage currentMessage;

    public void setMessage(AcarsMessage message) {
        this.currentMessage = message;
        if (message == null) {
            detailTextArea.setText("");
            responsePanel.setVisible(false);
            return;
        }

        detailTextArea.setText(message.getDetailFormat());
        
        boolean isOurMessage = message.isOutgoing();
        boolean isCpdlc = message instanceof CpdlcMessage;
        
        if (isCpdlc && !isOurMessage) {
            CpdlcMessage cpdlc = (CpdlcMessage) message;
            responsePanel.removeAll();

            if (cpdlc.hasBeenReplied()) {
                PilotButton chosenBtn = getButtonForResponse(cpdlc.getSentResponse());
                if (chosenBtn != null) {
                    chosenBtn.setEnabled(false);
                    chosenBtn.setCustomColor(new Color(70, 70, 70), Color.LIGHT_GRAY);
                    responsePanel.add(chosenBtn);
                    responsePanel.setVisible(true);
                } else {
                    responsePanel.setVisible(false);
                }
            } else {
                wilcoBtn.setEnabled(true); wilcoBtn.setCustomColor(GREEN_RESPONSE, Color.WHITE);
                unableBtn.setEnabled(true); unableBtn.setCustomColor(RED_RESPONSE, Color.WHITE);
                standbyBtn.setEnabled(true); standbyBtn.setCustomColor(YELLOW_RESPONSE, Color.WHITE);
                affirmBtn.setEnabled(true); affirmBtn.setCustomColor(GREEN_RESPONSE, Color.WHITE);
                negativeBtn.setEnabled(true); negativeBtn.setCustomColor(RED_RESPONSE, Color.WHITE);
                rogerBtn.setEnabled(true); rogerBtn.setCustomColor(BLUE_RESPONSE, Color.WHITE);

                switch (cpdlc.getParsedResponseType()) {
                    case WILCO_UNABLE:
                        responsePanel.add(wilcoBtn);
                        responsePanel.add(unableBtn);
                        responsePanel.add(standbyBtn);
                        responsePanel.setVisible(true);
                        break;
                    case ROGER:
                        responsePanel.add(rogerBtn);
                        responsePanel.add(standbyBtn);
                        responsePanel.setVisible(true);
                        break;
                    case AFFIRM_NEGATIVE:
                        responsePanel.add(affirmBtn);
                        responsePanel.add(negativeBtn);
                        responsePanel.add(standbyBtn);
                        responsePanel.setVisible(true);
                        break;
                    case NONE:
                    default:
                        responsePanel.setVisible(false);
                        break;
                }
            }
        } else {
            responsePanel.setVisible(false);
        }
        
        revalidate();
        repaint();
    }
}
