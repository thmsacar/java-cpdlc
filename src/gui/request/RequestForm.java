package gui.request;

import gui.GuiUtils;
import gui.UppercaseFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

import static gui.DashboardPanel.UI_FONT;


//TODO make this one abstract and create different RequestForms ex: RequestDirectForm
//then override createRequestField
//createRequestField in this class can be abstract
/**
 * Abstract base class for all CPDLC request forms.
 * Manages the "DUE TO" logic and common field styling.
 */
public abstract class RequestForm extends JPanel{

    protected String requestText;
    protected JTextField reqField;
    private DueToPanel dueToPanel;


    public RequestForm(String requestText){
        super(new GridBagLayout());

        this.requestText = requestText;

        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 2)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 1. FIX
        styleReqField();
        JPanel reqContainer = createRequestField();

        gbc.gridx = 0; gbc.gridy = 0;
        this.add(reqContainer, gbc);


        // 2. DUE TO PANEL
        dueToPanel = new DueToPanel();
        dueToPanel.setOpaque(false);
        gbc.gridy = 1;
        this.add(dueToPanel, gbc);

    }

    // Styling radio buttons in DUE TO options
    protected void styleDueOption(JRadioButton rb) {
        gui.GuiUtils.styleRadioButton(rb);
    }

    /**
     * Constructs the primary request text.
     * @return The formatted request string.
     */
    public String getRequestText(){
        String reqText = reqField.getText().trim().toUpperCase();
        if(reqText.isEmpty()){
            reqField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.RED),
                    BorderFactory.createEmptyBorder(0, 5, 0, 5)
            ));
        }
        else{
            reqField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY),
                    BorderFactory.createEmptyBorder(0, 5, 0, 5)
            ));
        }
        return reqText;
    }

    /**
     * Constructs the "DUE TO" part of the message.
     * @return The formatted due-to string.
     */
    public String getDueText(){
        return dueToPanel.getDueText();
    }

    /**
     * Creates the specific input fields for the request type.
     */
    public abstract JPanel createRequestField();

//    public abstract JPanel createRequestField(){
//        JPanel reqContainer = new JPanel(new BorderLayout(0, 5));
//        JLabel reqLabel = new JLabel(requestText);
//        reqLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));
//
//        reqField = new JTextField();
//        reqField.setFont(new Font(UI_FONT, Font.PLAIN, 14));
//        reqField.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
//        reqField.setPreferredSize(new Dimension(200, 30));
//        reqField.setMinimumSize(reqField.getPreferredSize());
//        ((AbstractDocument) reqField.getDocument()).setDocumentFilter(new UppercaseFilter());
//
//        reqContainer.add(reqLabel, BorderLayout.NORTH);
//        reqContainer.add(reqField, BorderLayout.CENTER);
//
//        return reqContainer;
//    }

    protected void styleReqField(){
        reqField = new JTextField();
        reqField.setFont(new Font(UI_FONT, Font.PLAIN, 14));
        reqField.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        reqField.setPreferredSize(new Dimension(200, 30));
        reqField.setMinimumSize(reqField.getPreferredSize());
        reqField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)
        ));
        ((AbstractDocument) reqField.getDocument()).setDocumentFilter(new UppercaseFilter());
    }

    /**
     * Clears all fields in the form.
     */
    public void clean(){
        reqField.setText("");
        dueToPanel.clean();
    }


}
