package gui.report;

import gui.NumericFilter;
import service.CpdlcMessageFormatter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

public class ReportPositionForm extends ReportForm {

    private final JTextField posField;
    private final JTextField timeField;
    private final JTextField thereafterField;
    private final JTextField levelField;
    private final JTextField nextPosField;
    private final JTextField nextTimeField;

    public ReportPositionForm() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;

        // --- Column 0 ---
        gbc.gridx = 0;
        
        // Present Pos
        gbc.insets = new Insets(2, 5, 0, 5);
        gbc.gridy = 0; add(createStyledLabel("POSITION"), gbc);
        gbc.insets = new Insets(0, 5, 8, 5);
        gbc.gridy = 1; posField = createStyledTextField(); add(posField, gbc);

        // Time
        gbc.insets = new Insets(2, 5, 0, 5);
        gbc.gridy = 2; add(createStyledLabel("TIME (ZULU)"), gbc);
        gbc.insets = new Insets(0, 5, 8, 5);
        gbc.gridy = 3; timeField = createStyledTextField(); 
        ((AbstractDocument) timeField.getDocument()).setDocumentFilter(new NumericFilter(4));
        add(timeField, gbc);

        // Thereafter
        gbc.insets = new Insets(2, 5, 0, 5);
        gbc.gridy = 4; add(createStyledLabel("THEREAFTER"), gbc);
        gbc.insets = new Insets(0, 5, 8, 5);
        gbc.gridy = 5; thereafterField = createStyledTextField(); add(thereafterField, gbc);

        // --- Column 1 ---
        gbc.gridx = 1;
        
        // Next Pos
        gbc.insets = new Insets(2, 5, 0, 5);
        gbc.gridy = 0; add(createStyledLabel("NEXT POSITION"), gbc);
        gbc.insets = new Insets(0, 5, 8, 5);
        gbc.gridy = 1; nextPosField = createStyledTextField(); add(nextPosField, gbc);

        // Next Time
        gbc.insets = new Insets(2, 5, 0, 5);
        gbc.gridy = 2; add(createStyledLabel("ETA NEXT"), gbc);
        gbc.insets = new Insets(0, 5, 8, 5);
        gbc.gridy = 3; nextTimeField = createStyledTextField(); 
        ((AbstractDocument) nextTimeField.getDocument()).setDocumentFilter(new NumericFilter(4));
        add(nextTimeField, gbc);

        // Level (Moved here)
        gbc.insets = new Insets(2, 5, 0, 5);
        gbc.gridy = 4; add(createStyledLabel("FL / ALT"), gbc);
        gbc.insets = new Insets(0, 5, 8, 5);
        gbc.gridy = 5; levelField = createStyledTextField(); 
        ((AbstractDocument) levelField.getDocument()).setDocumentFilter(new NumericFilter(5));
        add(levelField, gbc);

        // Empty spacer in col 1 to balance
        gbc.gridy = 10; gbc.weighty = 1.0; 
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        add(spacer, gbc);
    }

    @Override
    public String getReportText() {
        String pos = posField.getText().trim();
        String time = timeField.getText().trim();
        String thereafter = thereafterField.getText().trim();
        String level = levelField.getText().trim();
        String next = nextPosField.getText().trim();
        String eta = nextTimeField.getText().trim();

        return CpdlcMessageFormatter.formatPositionReport(pos, time, level, thereafter, next, eta);
    }

    @Override
    public void clean() {
        posField.setText("");
        timeField.setText("");
        thereafterField.setText("");
        levelField.setText("");
        nextPosField.setText("");
        nextTimeField.setText("");
    }
}
