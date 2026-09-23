package gui;

import javax.swing.text.*;
import java.util.Locale;

public class UppercaseFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
            throws BadLocationException {
        if (text != null) {
            text = filterText(text);
            if (text.isEmpty()) {
                return;
            }
        }
        super.insertString(fb, offset, text, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attr)
            throws BadLocationException {
        if (text != null) {
            text = filterText(text);
        }
        super.replace(fb, offset, length, text, attr);
    }

    public static String filterText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        text = text.replaceAll("[\r\n]", "").toUpperCase(Locale.ENGLISH);

        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= ' ' && c <= '~') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

