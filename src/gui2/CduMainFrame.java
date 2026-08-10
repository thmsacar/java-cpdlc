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
        setAppIcon(this);

        getContentPane().setBackground(new Color(25, 28, 32));

        cduPanel = new CduPanel();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (cduPanel != null && cduPanel.getController() != null) {
                    cduPanel.getController().handleWindowClose();
                }
            }
        });
        setLayout(new BorderLayout());
        add(cduPanel, BorderLayout.CENTER);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void setAppIcon(JFrame frame) {
        String iconPath = "/resources/images/icon.png";
        java.net.URL iconURL = getClass().getResource(iconPath);

        if (iconURL != null) {
            Image img = new ImageIcon(iconURL).getImage();
            frame.setIconImage(img);

            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                try {
                    Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
                    java.lang.reflect.Method getTaskbar = taskbarClass.getDeclaredMethod("getTaskbar");
                    Object taskbar = getTaskbar.invoke(null);
                    java.lang.reflect.Method setIconImage = taskbarClass.getDeclaredMethod("setIconImage", Image.class);
                    setIconImage.invoke(taskbar, img);
                } catch (Exception e1) {
                    try {
                        Class<?> appClass = Class.forName("com.apple.eawt.Application");
                        Object application = appClass.getMethod("getApplication").invoke(null);
                        appClass.getMethod("setDockIconImage", Image.class).invoke(application, img);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CduMainFrame frame = new CduMainFrame();
            frame.setVisible(true);
            UpdateChecker.checkForUpdatesAsync(frame);
        });
    }
}
