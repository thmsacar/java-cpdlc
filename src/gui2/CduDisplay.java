package gui2;

import gui.FontManager;

import javax.swing.*;
import java.awt.*;

/**
 * High-contrast CRT/LCD display screen for the CDU unit matching real aircraft DCDU screens.
 */
public class CduDisplay extends JPanel {

    public static class LineData {
        public String leftLabel = "";
        public String rightLabel = "";
        public String leftValue = "";
        public String rightValue = "";
        public Color leftColor = Color.WHITE;
        public Color rightColor = Color.WHITE;

        public LineData() {}

        public LineData(String leftLabel, String rightLabel, String leftValue, String rightValue) {
            this.leftLabel = leftLabel;
            this.rightLabel = rightLabel;
            this.leftValue = leftValue;
            this.rightValue = rightValue;
        }
    }

    private String headerTitle = "ATC-LOGON/STATUS";
    private String leftSubheader = "CDA";
    private String rightSubheader = "NDA";
    private String statusText = "ACARS UPLINK";

    private final LineData[] lines = new LineData[6];

    public CduDisplay() {
        setBackground(new Color(5, 7, 10));
        setPreferredSize(new Dimension(380, 290));

        // Initialize default lines matching screenshot cpdlc_test_3.jpg
        for (int i = 0; i < 6; i++) {
            lines[i] = new LineData();
        }

        // Line 1: Subheader dashes
        lines[0] = new LineData("", "", "--------", "--------");
        
        // Line 2: CALLSIGN & ATC CENTER
        lines[1] = new LineData("CALLSIGN", "ATC CENTER", "AUA364", "<");
        
        // Line 3: ORIG STA & DEST STA
        lines[2] = new LineData("ORIG STA", "DEST STA", "LOWW", "LOWS");

        // Line 6: Bottom Navigation
        lines[5] = new LineData("", "19:29", "<RETURN 19:3Ø", "MSG>");
    }

    public void setHeader(String title, String leftSub, String rightSub) {
        this.headerTitle = title;
        this.leftSubheader = leftSub;
        this.rightSubheader = rightSub;
        repaint();
    }

    public void setLine(int index, String leftLabel, String rightLabel, String leftValue, String rightValue) {
        if (index >= 0 && index < 6) {
            lines[index] = new LineData(leftLabel, rightLabel, leftValue, rightValue);
            repaint();
        }
    }

    public void setStatusText(String text) {
        this.statusText = text;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Subtle screen bezel inner shadow
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, width, 4);
        g2.fillRect(0, 0, 4, height);

        Font monoFont = FontManager.BOLD != null ? FontManager.BOLD : new Font("Monospaced", Font.BOLD, 14);
        Font monoSmall = FontManager.REGULAR != null ? FontManager.REGULAR : new Font("Monospaced", Font.PLAIN, 12);

        // Header section
        g2.setFont(monoFont.deriveFont(15f));
        g2.setColor(Color.WHITE);
        FontMetrics fmHeader = g2.getFontMetrics();
        int headerX = (width - fmHeader.stringWidth(headerTitle)) / 2;
        g2.drawString(headerTitle, headerX, 22);

        // Subheaders
        g2.setFont(monoSmall.deriveFont(12f));
        g2.setColor(new Color(180, 200, 220));
        if (leftSubheader != null && !leftSubheader.isEmpty()) {
            g2.drawString(leftSubheader, 65, 38);
        }
        if (rightSubheader != null && !rightSubheader.isEmpty()) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(rightSubheader, width - 65 - fm.stringWidth(rightSubheader), 38);
        }

        // Draw 6 LSK Line Pairs
        int startY = 52;
        int rowHeight = 36;

        for (int i = 0; i < 6; i++) {
            LineData ld = lines[i];
            int yLabel = startY + (i * rowHeight);
            int yValue = yLabel + 14;

            // Labels (Small)
            g2.setFont(monoSmall.deriveFont(11f));
            g2.setColor(new Color(170, 185, 200));

            if (ld.leftLabel != null && !ld.leftLabel.isEmpty()) {
                g2.drawString(ld.leftLabel, 12, yLabel);
            }
            if (ld.rightLabel != null && !ld.rightLabel.isEmpty()) {
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ld.rightLabel, width - 12 - fm.stringWidth(ld.rightLabel), yLabel);
            }

            // Values (Large)
            g2.setFont(monoFont.deriveFont(14f));
            g2.setColor(ld.leftColor);

            if (ld.leftValue != null && !ld.leftValue.isEmpty()) {
                g2.drawString(ld.leftValue, 12, yValue);
            }
            if (ld.rightValue != null && !ld.rightValue.isEmpty()) {
                g2.setColor(ld.rightColor);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ld.rightValue, width - 12 - fm.stringWidth(ld.rightValue), yValue);
            }
        }

        // Bottom Scratchpad / Status Bar
        g2.setFont(monoFont.deriveFont(13f));
        g2.setColor(Color.WHITE);
        if (statusText != null && !statusText.isEmpty()) {
            g2.drawString(statusText, 12, height - 10);
        }

        g2.dispose();
    }
}
