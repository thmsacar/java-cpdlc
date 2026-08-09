package gui2.components;

import javax.swing.*;
import java.awt.*;

/**
 * Alphanumeric keypad panel for CDU data entry and page navigation.
 */
public class CduKeypad extends JPanel {

    public interface KeypadListener {
        void onKeyTyped(String key);
    }

    private final KeypadListener listener;

    public CduKeypad(KeypadListener listener) {
        this.listener = listener;
        setOpaque(false);
        setLayout(new BorderLayout(8, 8));
        setupUI();
    }

    private void setupUI() {
        // Function keys row
        JPanel navRow = new JPanel(new GridLayout(2, 5, 4, 4));
        navRow.setOpaque(false);

        String[] navKeys = {
            "INIT REF", "RTE", "CLB", "CRZ", "DES",
            "MENU", "LEGS", "DEP ARR", "PREV", "NEXT"
        };

        for (String k : navKeys) {
            JButton btn = createKeyButton(k, new Color(50, 54, 62), Color.WHITE, 10f);
            btn.addActionListener(e -> listener.onKeyTyped(k));
            navRow.add(btn);
        }

        add(navRow, BorderLayout.NORTH);

        // Alpha & Numeric section
        JPanel alphaNumPanel = new JPanel(new GridLayout(6, 6, 4, 4));
        alphaNumPanel.setOpaque(false);

        String[] alphaNumKeys = {
            "A", "B", "C", "D", "E", "1",
            "F", "G", "H", "I", "J", "2",
            "K", "L", "M", "N", "O", "3",
            "P", "Q", "R", "S", "T", "4",
            "U", "V", "W", "X", "Y", "5",
            "Z", "/", "SP", "CLR", "DEL", "0"
        };

        for (String k : alphaNumKeys) {
            Color bg = k.equals("CLR") || k.equals("DEL") ? new Color(75, 40, 40) : new Color(42, 45, 52);
            JButton btn = createKeyButton(k, bg, Color.WHITE, 12f);
            btn.addActionListener(e -> listener.onKeyTyped(k));
            alphaNumPanel.add(btn);
        }

        add(alphaNumPanel, BorderLayout.CENTER);
    }

    private JButton createKeyButton(String label, Color bg, Color fg, float fontPt) {
        JButton btn = new JButton(label);
        btn.setFocusPainted(false);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, (int) fontPt));
        btn.setMargin(new Insets(2, 2, 2, 2));
        btn.setPreferredSize(new Dimension(36, 26));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
