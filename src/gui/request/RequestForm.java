package gui.request;

import gui.SquareIcon;
import gui.UppercaseFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

import static gui.DashboardPanel.UI_FONT;

public class RequestForm extends JPanel{

    private String requestText;

    private JTextField reqField;
    private JTextField remarkField;

    private ButtonGroup dueGroup;
    private JRadioButton perfOpt;
    private JRadioButton weatherOpt;
    private JRadioButton freeTextOpt;


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
        JPanel reqContainer = createRequestField();

        gbc.gridx = 0; gbc.gridy = 0;
        this.add(reqContainer, gbc);


        // 2. DUE TO OPTIONS
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
        styleDueOption(perfOpt);
        styleDueOption(weatherOpt);
        styleDueOption(freeTextOpt);

        dueGroup = new ButtonGroup();
        dueGroup.add(perfOpt); dueGroup.add(weatherOpt); dueGroup.add(freeTextOpt);
        JPanel buttonGroup = new JPanel(new BorderLayout(15, 10));
        buttonGroup.add(perfOpt, BorderLayout.WEST);
        buttonGroup.add(weatherOpt, BorderLayout.CENTER);
        buttonGroup.add(freeTextOpt, BorderLayout.SOUTH);

        dueContainer.add(dueLabel, BorderLayout.NORTH);
        dueContainer.add(buttonGroup, BorderLayout.CENTER);

        gbc.gridy = 1; gbc.weightx = 2.0;
        this.add(dueContainer, gbc);



        // 3. FREE TEXT AREA
        remarkField = new JTextField();
        remarkField.setFont(new Font(UI_FONT, Font.PLAIN, 14));
        remarkField.setPreferredSize(new Dimension(200, 30));
        remarkField.setMinimumSize(remarkField.getPreferredSize());
        remarkField.setEnabled(false); // Enable only when free text selected
        ((AbstractDocument) remarkField.getDocument()).setDocumentFilter(new UppercaseFilter());
        remarkField.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        gbc.gridy = 2; gbc.gridx=0;
        this.add(remarkField, gbc);

        freeTextOpt.addActionListener(e -> remarkField.setEnabled(true));
        perfOpt.addActionListener(e -> { remarkField.setEnabled(false); remarkField.setText(""); });
        weatherOpt.addActionListener(e -> { remarkField.setEnabled(false); remarkField.setText(""); });

    }

    // Styling radio buttons in DUE TO options
    private void styleDueOption(JRadioButton rb) {
        rb.setOpaque(false);
        rb.setFont(new Font(UI_FONT, Font.PLAIN, 13));
        rb.setForeground(Color.LIGHT_GRAY);
        rb.setFocusPainted(false);

        //Square radio
        rb.setIcon(new SquareIcon(false));
        rb.setSelectedIcon(new SquareIcon(true));
        rb.setRolloverIcon(new SquareIcon(false));
    }

    public String getRequestText(){
        String reqText = reqField.getText().trim().toUpperCase();
        if(reqText.isEmpty()){
            reqField.setBorder(BorderFactory.createLineBorder(Color.RED));
        }
        else{
            reqField.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        }
        return reqText;
    }

    public String getDueText(){
        String dueText = "";
        ButtonModel selection = dueGroup.getSelection();
        if(selection == null){return dueText;}
        if (selection.getActionCommand().equals("PERFORMANCE")) {
            dueText = "DUE TO PERFORMANCE";
        } else if (selection.getActionCommand().equals("WEATHER")) {
            dueText = "DUE TO WEATHER";
        } else if (selection.getActionCommand().equals("FREE TEXT")) {
            dueText = "DUE TO " + remarkField.getText().trim();
        }
        return dueText;
    }

    public JPanel createRequestField(){
        JPanel reqContainer = new JPanel(new BorderLayout(0, 5));
        JLabel reqLabel = new JLabel(requestText);
        reqLabel.setFont(new Font(UI_FONT, Font.BOLD, 14));

        reqField = new JTextField();
        reqField.setFont(new Font(UI_FONT, Font.PLAIN, 14));
        reqField.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        reqField.setPreferredSize(new Dimension(200, 30));
        reqField.setMinimumSize(reqField.getPreferredSize());
        ((AbstractDocument) reqField.getDocument()).setDocumentFilter(new UppercaseFilter());

        reqContainer.add(reqLabel, BorderLayout.NORTH);
        reqContainer.add(reqField, BorderLayout.CENTER);

        return reqContainer;
    }

}
