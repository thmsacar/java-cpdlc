package gui.report;

import javax.swing.*;
import java.awt.*;

public class ReportLevelForm extends ReportForm {

    private final JTextField levelField;
    private final JRadioButton maintainingBtn;
    private final JRadioButton reachingBtn;
    private final JRadioButton leavingBtn;
    private final ButtonGroup statusGroup;

    public ReportLevelForm() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;

        gbc.gridy = 0;
        add(createStyledLabel("STATUS"), gbc);

        gbc.gridy = 1;
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusPanel.setOpaque(false);
        
        maintainingBtn = new JRadioButton("MAINTAINING", true);
        reachingBtn = new JRadioButton("REACHING");
        leavingBtn = new JRadioButton("LEAVING");
        
        styleRadioButton(maintainingBtn);
        styleRadioButton(reachingBtn);
        styleRadioButton(leavingBtn);
        
        statusGroup = new ButtonGroup();
        statusGroup.add(maintainingBtn);
        statusGroup.add(reachingBtn);
        statusGroup.add(leavingBtn);
        
        statusPanel.add(maintainingBtn);
        statusPanel.add(reachingBtn);
        statusPanel.add(leavingBtn);
        add(statusPanel, gbc);

        gbc.gridy = 2;
        add(createStyledLabel("LEVEL"), gbc);

        gbc.gridy = 3;
        levelField = createStyledTextField();
        ((javax.swing.text.AbstractDocument) levelField.getDocument()).setDocumentFilter(new gui.NumericFilter(5));
        add(levelField, gbc);

        // Spacer to push everything up
        gbc.gridy = 4;
        gbc.weighty = 1.0;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        add(spacer, gbc);
    }

    @Override
    public String getReportText() {
        String level = levelField.getText().trim();
        if (level.isEmpty()) return null;
        
        String status = "MAINTAINING";
        if (reachingBtn.isSelected()) status = "REACHING";
        else if (leavingBtn.isSelected()) status = "LEAVING";
        
        return status + " LEVEL " + level;
    }

    @Override
    public void clean() {
        levelField.setText("");
        maintainingBtn.setSelected(true);
    }
}
