package gui.request;

import gui.GuiUtils;
import gui.UppercaseFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

import static gui.DashboardPanel.UI_FONT;

/**
 * Reusable panel for the "DUE TO" section in CPDLC requests.
 */
public class DueToPanel extends JPanel {

    private ButtonGroup dueGroup;
    private JRadioButton perfOpt;
    private JRadioButton weatherOpt;
    private JRadioButton freeTextOpt;
    private JTextField remarkField;

    public DueToPanel() {
        super(new GridBagLayout());
        setupUI();
    }

    private void setupUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JPanel dueContainer = new JPanel(new BorderLayout(0, 5));
        JLabel dueLabel = new JLabel("DUE TO");
        dueLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));

        // Radio buttons
        perfOpt = new JRadioButton("PERFORMANCE");
        perfOpt.setActionCommand("PERFORMANCE");
        weatherOpt = new JRadioButton("WEATHER");
        weatherOpt.setActionCommand("WEATHER");
        freeTextOpt = new JRadioButton("FREE TEXT:");
        freeTextOpt.setActionCommand("FREE TEXT");

        // Style
        GuiUtils.styleRadioButton(perfOpt);
        GuiUtils.styleRadioButton(weatherOpt);
        GuiUtils.styleRadioButton(freeTextOpt);

        dueGroup = new ButtonGroup();
        dueGroup.add(perfOpt); dueGroup.add(weatherOpt); dueGroup.add(freeTextOpt);
        
        JPanel buttonGroup = new JPanel(new BorderLayout(15, 10));
        buttonGroup.add(perfOpt, BorderLayout.WEST);
        buttonGroup.add(weatherOpt, BorderLayout.CENTER);
        buttonGroup.add(freeTextOpt, BorderLayout.SOUTH);

        dueContainer.add(dueLabel, BorderLayout.NORTH);
        dueContainer.add(buttonGroup, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;
        this.add(dueContainer, gbc);

        // FREE TEXT AREA
        remarkField = new JTextField();
        remarkField.setFont(new Font(UI_FONT, Font.PLAIN, 14));
        remarkField.setPreferredSize(new Dimension(200, 30));
        remarkField.setMinimumSize(remarkField.getPreferredSize());
        remarkField.setEnabled(false); 
        remarkField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)
        ));
        ((AbstractDocument) remarkField.getDocument()).setDocumentFilter(new UppercaseFilter());
        
        gbc.gridy = 1;
        this.add(remarkField, gbc);

        freeTextOpt.addActionListener(e -> remarkField.setEnabled(true));
        perfOpt.addActionListener(e -> { remarkField.setEnabled(false); remarkField.setText(""); });
        weatherOpt.addActionListener(e -> { remarkField.setEnabled(false); remarkField.setText(""); });
    }

    public String getDueText() {
        ButtonModel selection = dueGroup.getSelection();
        if (selection == null) return "";
        
        if (selection.getActionCommand().equals("PERFORMANCE")) {
            return "DUE TO PERFORMANCE";
        } else if (selection.getActionCommand().equals("WEATHER")) {
            return "DUE TO WEATHER";
        } else if (selection.getActionCommand().equals("FREE TEXT")) {
            return "DUE TO " + remarkField.getText().trim();
        }
        return "";
    }

    public void clean() {
        remarkField.setText("");
        dueGroup.clearSelection();
        remarkField.setEnabled(false);
    }
}
