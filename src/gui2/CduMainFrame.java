package gui2;

import gui.FontManager;
import service.UpdateChecker;

import javax.swing.*;
import java.awt.*;

/**
 * Standalone Main Frame launcher for the gui2 CDU UI with automatic Github release update checking.
 */
public class CduMainFrame extends JFrame {

    private final CduPanel cduPanel;

    public CduMainFrame() {
        super("Java CPDLC");

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}

        FontManager.loadFonts();

        getContentPane().setBackground(new Color(25, 28, 32));

        cduPanel = new CduPanel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(cduPanel, BorderLayout.CENTER);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CduMainFrame frame = new CduMainFrame();
            frame.setVisible(true);
            UpdateChecker.checkForUpdatesAsync(frame);
        });
    }
}
