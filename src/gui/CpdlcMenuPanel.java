package gui;

import gui.button.PilotButton;
import gui.button.ReturnButton;
import service.CpdlcService;

import javax.swing.*;
import java.awt.*;

public class CpdlcMenuPanel extends JPanel {

    private final CpdlcService service;
    private final Runnable onBack;
    private final CardSwitcher switcher;

    private PilotButton btnClearance;
    private PilotButton btnLogonATC;
    private PilotButton btnRequest;
    private PilotButton btnReport;

    public interface CardSwitcher {
        void showCard(String cardName);
    }

    public CpdlcMenuPanel(CpdlcService service, Runnable onBack, CardSwitcher switcher) {
        this.service = service;
        this.onBack = onBack;
        this.switcher = switcher;
        setupUI();
        updateMenuState();
    }

    private void setupUI() {
        setLayout(new BorderLayout(0, 10));

        PilotButton returnBtn = new ReturnButton();
        returnBtn.addActionListener(e -> onBack.run());
        add(returnBtn, BorderLayout.NORTH);

        JPanel menu = new JPanel(new GridLayout(2, 2, 20, 20));
        menu.setBackground(new Color(30, 30, 30));
        menu.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        btnClearance = createMenuButton("PDC / CLX");
        btnLogonATC = createMenuButton("ATC LOGON");
        btnRequest = createMenuButton("REQUEST");
        btnReport = createMenuButton("REPORT");

        btnClearance.addActionListener(e -> switcher.showCard("CLEARANCE_MENU"));
        btnLogonATC.addActionListener(e -> {
            if (service.isLoggedOn()) {
                service.sendLogoff();
            } else {
                switcher.showCard("LOGON_FORM");
            }
        });
        btnRequest.addActionListener(e -> switcher.showCard("REQUEST"));
        btnReport.addActionListener(e -> switcher.showCard("REPORT"));

        menu.add(btnClearance);
        menu.add(btnLogonATC);
        menu.add(btnRequest);
        menu.add(btnReport);

        add(menu, BorderLayout.CENTER);
    }

    private PilotButton createMenuButton(String text) {
        PilotButton btn = new PilotButton(text);
        btn.setFont(new Font(DashboardPanel.UI_FONT, Font.BOLD, 14));
        return btn;
    }

    public void updateMenuState() {
        if (service.isLoggedOn()) {
            btnLogonATC.setText("LOGOFF " + service.getCurrentATS());
            btnLogonATC.setForeground(new Color(200, 0, 0));
            btnRequest.setEnabled(true);
            btnRequest.setForeground(Color.WHITE);
            btnReport.setEnabled(true);
            btnReport.setForeground(Color.WHITE);
        } else {
            btnLogonATC.setText("ATC LOGON");
            btnLogonATC.setForeground(new Color(180, 100, 200));
            btnRequest.setEnabled(false);
            btnRequest.setForeground(Color.GRAY);
            btnReport.setEnabled(false);
            btnReport.setForeground(Color.GRAY);
        }
    }
}
