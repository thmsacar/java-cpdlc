package gui.request;

import gui.NumericFilter;
import gui.UppercaseFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

import static gui.DashboardPanel.UI_FONT;

public class RequestLevelForm extends RequestForm {
    public RequestLevelForm() {
        super("REQUEST LEVEL");
    }

    @Override
    public JPanel createRequestField() {
        JPanel reqContainer = new JPanel(new BorderLayout(0, 5));
        JLabel reqLabel = new JLabel(requestText);
        reqLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));

        reqContainer.add(reqLabel, BorderLayout.NORTH);
        ((AbstractDocument) reqField.getDocument()).setDocumentFilter(new NumericFilter());
        reqContainer.add(reqField, BorderLayout.CENTER);

        return reqContainer;

    }

    @Override
    public String getRequestText() {
        String level = super.getRequestText();
        if (level == null || level.isEmpty()) return "";
        return String.format("REQUEST LEVEL %s", level);
    }

}
