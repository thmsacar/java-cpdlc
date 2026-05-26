package gui.request;

import javax.swing.*;
import java.awt.*;
import gui.GuiUtils;

public class RequestWhenCanWeForm extends RequestForm {
    
    private ButtonGroup typeGroup;
    private JRadioButton levelBtn;

    public RequestWhenCanWeForm() {
        super("WHEN CAN WE EXPECT");
    }

    @Override
    public JPanel createRequestField() {
        JPanel reqContainer = new JPanel(new BorderLayout(0, 5));
        JLabel reqLabel = new JLabel(requestText);
        reqLabel.setFont(new Font(gui.DashboardPanel.UI_FONT, Font.BOLD, 14));

        JPanel fieldContainer = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 10);

        levelBtn = new JRadioButton("LEVEL");
        levelBtn.setActionCommand("LEVEL");
        JRadioButton speedBtn = new JRadioButton("SPEED");
        speedBtn.setActionCommand("SPEED");
        JRadioButton directBtn = new JRadioButton("DIRECT TO");
        directBtn.setActionCommand("DIRECT TO");

        GuiUtils.styleRadioButton(levelBtn);
        GuiUtils.styleRadioButton(speedBtn);
        GuiUtils.styleRadioButton(directBtn);

        typeGroup = new ButtonGroup();
        typeGroup.add(levelBtn);
        typeGroup.add(speedBtn);
        typeGroup.add(directBtn);
        
        levelBtn.setSelected(true);

        gbc.gridx = 0; gbc.gridy = 0;
        fieldContainer.add(levelBtn, gbc);
        gbc.gridx = 1;
        fieldContainer.add(speedBtn, gbc);
        gbc.gridx = 2;
        fieldContainer.add(directBtn, gbc);

        gbc.gridx = 3;
        fieldContainer.add(reqField, gbc);

        reqContainer.add(reqLabel, BorderLayout.NORTH);
        reqContainer.add(fieldContainer, BorderLayout.WEST);

        return reqContainer;
    }

    @Override
    public String getRequestText() {
        String val = super.getRequestText();
        if (val == null || val.isEmpty()) return "";
        ButtonModel bm = typeGroup.getSelection();
        if (bm == null) return "";
        return "WHEN CAN WE EXPECT " + bm.getActionCommand() + " " + val;
    }
    
    @Override
    public void clean() {
        super.clean();
        if (levelBtn != null) {
            levelBtn.setSelected(true);
        }
    }
}
