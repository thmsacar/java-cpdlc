package gui.report;

import javax.swing.*;
import java.awt.*;

public class ReportSpeedForm extends ReportForm {

    private final JTextField speedField;
    private final JRadioButton machRadio;
    private final JRadioButton iasRadio;

    public ReportSpeedForm() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;

        gbc.gridy = 0;
        add(createStyledLabel("TYPE"), gbc);

        gbc.gridy = 1;
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        typePanel.setOpaque(false);
        machRadio = new JRadioButton("MACH");
        iasRadio = new JRadioButton("IAS", true);
        styleRadioButton(machRadio);
        styleRadioButton(iasRadio);
        ButtonGroup group = new ButtonGroup();
        group.add(machRadio); group.add(iasRadio);
        typePanel.add(iasRadio); typePanel.add(machRadio);
        add(typePanel, gbc);

        gbc.gridy = 2;
        add(createStyledLabel("SPEED"), gbc);

        gbc.gridy = 3;
        speedField = createStyledTextField();
        ((javax.swing.text.AbstractDocument) speedField.getDocument()).setDocumentFilter(new gui.NumericFilter(3));
        add(speedField, gbc);

        gbc.gridy = 4;
        gbc.weighty = 1.0;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        add(spacer, gbc);
    }

    @Override
    public String getReportText() {
        String speed = speedField.getText().trim();
        if (speed.isEmpty()) return null;
        String type = machRadio.isSelected() ? "M." : "IAS ";
        return "PRESENT SPEED " + type + speed;
    }

    @Override
    public void clean() {
        speedField.setText("");
        iasRadio.setSelected(true);
    }
}
