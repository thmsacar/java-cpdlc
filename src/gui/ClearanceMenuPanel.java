package gui;

import gui.button.PilotButton;
import gui.button.ReturnButton;
import service.CpdlcService;

import javax.swing.*;
import java.awt.*;

public class ClearanceMenuPanel extends JPanel {

    private final CpdlcService service;
    private final Runnable onBack;
    private final CpdlcMenuPanel.CardSwitcher switcher;

    public ClearanceMenuPanel(CpdlcService service, Runnable onBack, CpdlcMenuPanel.CardSwitcher switcher) {
        this.service = service;
        this.onBack = onBack;
        this.switcher = switcher;
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout(0, 10));

        PilotButton returnBtn = new ReturnButton();
        returnBtn.addActionListener(e -> onBack.run());
        add(returnBtn, BorderLayout.NORTH);

        JPanel menu = new JPanel(new GridLayout(1, 2, 20, 20));
        menu.setBackground(new Color(30, 30, 30));
        menu.setBorder(BorderFactory.createEmptyBorder(50, 20, 50, 20));

        PilotButton btnDepartureClx = createMenuButton("DEPARTURE CLX");
        PilotButton btnOceanicClx = createMenuButton("OCEANIC CLX");

        btnDepartureClx.addActionListener(e -> switcher.showCard("PDC_FORM"));
        
        btnOceanicClx.setEnabled(false);
        btnOceanicClx.setForeground(Color.GRAY);

        menu.add(btnDepartureClx);
        menu.add(menu.add(btnOceanicClx));

        add(menu, BorderLayout.CENTER);
    }

    private PilotButton createMenuButton(String text) {
        PilotButton btn = new PilotButton(text);
        btn.setFont(new Font(DashboardPanel.UI_FONT, Font.BOLD, 14));
        return btn;
    }
}
