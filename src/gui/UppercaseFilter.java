package gui;

import javax.swing.text.*;

public class UppercaseFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
            throws BadLocationException {
        if (text != null) {
            text = text.replaceAll("[\r\n]", "").toUpperCase();
        }
        super.insertString(fb, offset, text, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attr)
            throws BadLocationException {
        if (text != null) {
            text = text.replaceAll("[\r\n]", "").toUpperCase();
        }
        super.replace(fb, offset, length, text, attr);
    }
}
