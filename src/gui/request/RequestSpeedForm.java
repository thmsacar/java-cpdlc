package gui.request;

import gui.NumericFilter;
import gui.UppercaseFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

import static gui.DashboardPanel.UI_FONT;

public class RequestSpeedForm extends RequestForm {

    private ButtonGroup group;

    public RequestSpeedForm() {
        super("REQUEST SPEED");
    }



    @Override
    public JPanel createRequestField(){
        JPanel reqContainer = new JPanel(new BorderLayout(0, 5));
        JLabel reqLabel = new JLabel(requestText);
        reqLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));

        JPanel fieldContainer = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 10);

        JRadioButton knots = new JRadioButton("IAS");
        knots.setActionCommand("ias");
        JRadioButton mach = new JRadioButton("MACH");
        mach.setActionCommand("mach");
        styleDueOption(knots);
        styleDueOption(mach);

        group = new ButtonGroup();
        group.add(knots);
        group.add(mach);

        gbc.gridx = 0; gbc.gridy = 0;
        fieldContainer.add(knots, gbc);
        gbc.gridx = 1;
        fieldContainer.add(mach, gbc);

        gbc.gridx = 2;
        ((AbstractDocument) reqField.getDocument()).setDocumentFilter(new NumericFilter());
        fieldContainer.add(reqField, gbc);

        reqContainer.add(reqLabel, BorderLayout.NORTH);
        reqContainer.add(fieldContainer, BorderLayout.WEST);

        return reqContainer;
    }

    @Override
    public String getRequestText() {
        String rawSpeed = super.getRequestText();
        if (rawSpeed == null || rawSpeed.isEmpty()) return "";
        ButtonModel bm = group.getSelection();
        switch (bm.getActionCommand()) {
            case "ias":
                return "REQUEST SPEED IAS " + rawSpeed;
            case "mach":
                return "REQUEST SPEED M." + rawSpeed;
            default:
                return "";
        }
    }

}







