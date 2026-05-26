package gui.report;

import gui.UppercaseFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

import static gui.DashboardPanel.UI_FONT;

public abstract class ReportForm extends JPanel {

    public ReportForm() {
        super(new GridBagLayout());

        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
    }

    protected JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font(UI_FONT, Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(200, 30));
        field.setMinimumSize(field.getPreferredSize());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)
        ));
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new UppercaseFilter());
        return field;
    }

    protected JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(UI_FONT, Font.BOLD, 14));
        return label;
    }

    protected void styleRadioButton(JRadioButton rb) {
        gui.GuiUtils.styleRadioButton(rb);
    }

    /**
     * Constructs the formatted report text to be sent.
     * @return The CPDLC message string, or null if validation fails.
     */
    public abstract String getReportText();
    public abstract void clean();
}
