package gui;

import gui.button.PilotButton;
import service.UpdateChecker.ReleaseInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;

public class UpdateDialog extends JDialog {

    public UpdateDialog(Window owner, String currentVersion, ReleaseInfo releaseInfo) {
        super(owner, "Update Available", ModalityType.APPLICATION_MODAL);
        setupUI(currentVersion, releaseInfo);
    }

    public static void showUpdateDialog(Component parent, String currentVersion, ReleaseInfo releaseInfo) {
        Window window = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        UpdateDialog dialog = new UpdateDialog(window, currentVersion, releaseInfo);
        dialog.setLocationRelativeTo(window);
        dialog.setVisible(true);
    }

    private void setupUI(String currentVersion, ReleaseInfo releaseInfo) {
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(new Color(45, 45, 45));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- Header Section ---
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("NEW VERSION AVAILABLE", SwingConstants.CENTER);
        titleLabel.setForeground(Color.CYAN);
        titleLabel.setFont(FontManager.BOLD != null ? FontManager.BOLD.deriveFont(16f) : new Font("Roboto Mono", Font.BOLD, 16));

        JLabel versionLabel = new JLabel("Installed: v" + currentVersion + "  ➔  Latest: " + releaseInfo.tagName, SwingConstants.CENTER);
        versionLabel.setForeground(Color.WHITE);
        versionLabel.setFont(FontManager.REGULAR != null ? FontManager.REGULAR.deriveFont(13f) : new Font("Roboto Mono", Font.PLAIN, 13));

        headerPanel.add(titleLabel);
        headerPanel.add(versionLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- Release Notes Section ---
        if (releaseInfo.releaseNotes != null && !releaseInfo.releaseNotes.trim().isEmpty()) {
            JTextArea notesArea = new JTextArea(releaseInfo.releaseNotes.trim());
            notesArea.setFont(FontManager.REGULAR != null ? FontManager.REGULAR.deriveFont(12f) : new Font("Roboto Mono", Font.PLAIN, 12));
            notesArea.setForeground(Color.LIGHT_GRAY);
            notesArea.setBackground(new Color(30, 30, 30));
            notesArea.setEditable(false);
            notesArea.setLineWrap(true);
            notesArea.setWrapStyleWord(true);
            notesArea.setCaretPosition(0);

            JScrollPane scrollPane = new JScrollPane(notesArea);
            scrollPane.setPreferredSize(new Dimension(380, 120));
            scrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());

            mainPanel.add(scrollPane, BorderLayout.CENTER);
        } else {
            JLabel messageLabel = new JLabel("A new version of Java CPDLC is ready for download.", SwingConstants.CENTER);
            messageLabel.setForeground(Color.LIGHT_GRAY);
            messageLabel.setFont(FontManager.REGULAR != null ? FontManager.REGULAR.deriveFont(13f) : new Font("Roboto Mono", Font.PLAIN, 13));
            mainPanel.add(messageLabel, BorderLayout.CENTER);
        }

        // --- Button Section ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        PilotButton ignoreButton = new PilotButton("IGNORE");
        ignoreButton.setPreferredSize(new Dimension(110, 35));
        ignoreButton.setCustomColor(Color.DARK_GRAY, Color.WHITE);
        ignoreButton.addColorChangerOnPress();
        ignoreButton.addActionListener(e -> dispose());

        PilotButton updateButton = new PilotButton("UPDATE");
        updateButton.setPreferredSize(new Dimension(110, 35));
        updateButton.setCustomColor(new Color(66, 139, 221), Color.WHITE);
        updateButton.addColorChangerOnPress();
        updateButton.addActionListener(e -> {
            openBrowser(releaseInfo.htmlUrl);
            dispose();
        });

        buttonPanel.add(ignoreButton);
        buttonPanel.add(updateButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setMinimumSize(new Dimension(420, getHeight()));
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback for OSes where Desktop.browse might fail
                Runtime.getRuntime().exec(new String[]{"open", url});
            }
        } catch (Exception ex) {
            System.err.println("Could not open browser: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Please visit:\n" + url, "Update Link", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
