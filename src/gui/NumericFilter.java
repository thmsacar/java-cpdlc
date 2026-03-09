package gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class NumericFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        if (string == null) return;
        int total = fb.getDocument().getLength() + string.length();
        if (isNumeric(string) && total <= 3) {
            super.insertString(fb, offset, string, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (text == null) return;
        int total = fb.getDocument().getLength() - length + text.length();
        if (isNumeric(text) && total <= 3) {
            super.replace(fb, offset, length, text, attrs);
        }
    }

    private boolean isNumeric(String text) {
        // Sadece 0-9 arası rakamlara izin ver
        return text.matches("\\d*");
    }
}
