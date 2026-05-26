package gui;

import gui.button.PilotButton;
import gui.button.ReturnButton;
import gui.request.RequestDirectForm;
import gui.request.RequestForm;
import gui.request.RequestLevelForm;
import gui.request.RequestSpeedForm;
import service.CpdlcService;

import javax.swing.*;
import java.awt.*;

public class RequestPanel extends JPanel {

    private final CpdlcService service;
    private final Runnable onBack;
    private final Runnable onSend;
    private final CardLayout cardLayout;
    private final JPanel cardContainer;

    public RequestPanel(CpdlcService service, Runnable onBack, Runnable onSend) {
        this.service = service;
        this.onBack = onBack;
        this.onSend = onSend;
        this.cardLayout = new CardLayout();
        this.cardContainer = new JPanel(cardLayout);

        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        cardContainer.add(createMenuPanel(), "MENU");
        cardContainer.add(createRequestFormPanel("DIRECT TO", new RequestDirectForm()), "DIRECT");
        cardContainer.add(createRequestFormPanel("LEVEL", new RequestLevelForm()), "LEVEL");
        cardContainer.add(createRequestFormPanel("SPEED", new RequestSpeedForm()), "SPEED");
        cardContainer.add(createRequestFormPanel("WHEN CAN WE EXPECT", new gui.request.RequestWhenCanWeForm()), "WHEN_CAN_WE");

        add(cardContainer, BorderLayout.CENTER);
        cardLayout.show(cardContainer, "MENU");
    }

    private JPanel createMenuPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        
        PilotButton returnBtn = new ReturnButton();
        returnBtn.addActionListener(e -> onBack.run());
        p.add(returnBtn, BorderLayout.NORTH);

        JPanel menu = new JPanel(new GridLayout(2, 2, 20, 20));
        menu.setBackground(new Color(30, 30, 30));
        menu.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        menu.add(createMenuButton("DIRECT", "DIRECT"));
        menu.add(createMenuButton("LEVEL", "LEVEL"));
        menu.add(createMenuButton("SPEED", "SPEED"));
        menu.add(createMenuButton("WHEN CAN WE", "WHEN_CAN_WE"));
        
        p.add(menu, BorderLayout.CENTER);
        return p;
    }

    private PilotButton createMenuButton(String text, String cardName) {
        PilotButton btn = new PilotButton(text);
        btn.setFont(new Font(DashboardPanel.UI_FONT, Font.BOLD, 14));
        btn.addActionListener(e -> cardLayout.show(cardContainer, cardName));
        return btn;
    }

    private JPanel createRequestFormPanel(String title, RequestForm form) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        
        PilotButton backBtn = new ReturnButton();
        backBtn.addActionListener(e -> {
            form.clean();
            cardLayout.show(cardContainer, "MENU");
        });
        p.add(backBtn, BorderLayout.NORTH);
        
        p.add(form, BorderLayout.CENTER);

        PilotButton sendBtn = new PilotButton("REQUEST " + title);
        sendBtn.setPreferredSize(new Dimension(0, 40));
        sendBtn.setCustomColor(new Color(60, 120, 60), Color.WHITE);
        sendBtn.addActionListener(e -> {
            String reqText = form.getRequestText();
            if (reqText != null && !reqText.isEmpty()) {
                String fullMsg = reqText + " " + form.getDueText();
                service.sendRequest(fullMsg.trim());
                form.clean();
                onSend.run();
            }
        });
        p.add(sendBtn, BorderLayout.SOUTH);

        return p;
    }
}
