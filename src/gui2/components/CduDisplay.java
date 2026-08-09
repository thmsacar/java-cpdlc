package gui2.components;

import gui.FontManager;

import javax.swing.*;
import java.awt.*;

/**
 * Monospace CRT/LCD DCDU display screen with pixel-perfect row alignments matching LSK buttons.
 */
public class CduDisplay extends JPanel {

    public enum DisplayColor {
        WHITE(new Color(240, 245, 250)),
        CYAN(new Color(90, 215, 255)),
        GREEN(new Color(50, 230, 110)),
        AMBER(new Color(255, 180, 40)),
        MAGENTA(new Color(255, 100, 220)),
        WHITE_DIM(new Color(160, 175, 190));

        private final Color color;
        DisplayColor(Color color) { this.color = color; }
        public Color getColor() { return color; }
    }

    public static class LineItem {
        public String label = "";
        public String value = "";
        public DisplayColor labelColor = DisplayColor.WHITE_DIM;
        public DisplayColor valueColor = DisplayColor.WHITE;

        public LineItem() {}

        public LineItem(String label, String value) {
            this.label = label != null ? label : "";
            this.value = value != null ? value : "";
        }

        public LineItem(String label, String value, DisplayColor valueColor) {
            this.label = label != null ? label : "";
            this.value = value != null ? value : "";
            this.valueColor = valueColor != null ? valueColor : DisplayColor.WHITE;
        }
    }

    private String headerTitle = "ATC-LOGON/STATUS";
    private String leftSubheader = "CDA";
    private String rightSubheader = "NDA";
    private String scratchpad = "";
    private String statusText = "ACARS READY";

    private final LineItem[] leftLines = new LineItem[6];
    private final LineItem[] rightLines = new LineItem[6];

    public CduDisplay() {
        setOpaque(true);
        setBackground(new Color(5, 7, 10));
        setPreferredSize(new Dimension(380, 300));
        setMinimumSize(new Dimension(380, 300));
        setMaximumSize(new Dimension(380, 300));

        for (int i = 0; i < 6; i++) {
            leftLines[i] = new LineItem();
            rightLines[i] = new LineItem();
        }
    }

    public void setHeader(String title, String leftSub, String rightSub) {
        this.headerTitle = title != null ? title : "";
        this.leftSubheader = leftSub != null ? leftSub : "";
        this.rightSubheader = rightSub != null ? rightSub : "";
        repaint();
    }

    public void setLine(int index, LineItem leftItem, LineItem rightItem) {
        if (index >= 0 && index < 6) {
            leftLines[index] = leftItem != null ? leftItem : new LineItem();
            rightLines[index] = rightItem != null ? rightItem : new LineItem();
            repaint();
        }
    }

    public void clearLines() {
        for (int i = 0; i < 6; i++) {
            leftLines[i] = new LineItem();
            rightLines[i] = new LineItem();
        }
        repaint();
    }

    public void setScratchpad(String text) {
        this.scratchpad = text != null ? text : "";
        repaint();
    }

    public void setStatusText(String text) {
        this.statusText = text != null ? text : "";
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Solid CRT background fill
        g2.setColor(new Color(5, 7, 10));
        g2.fillRect(0, 0, width, height);

        // CRT scanlines effect
        g2.setColor(new Color(0, 0, 0, 35));
        for (int y = 0; y < height; y += 3) {
            g2.drawLine(0, y, width, y);
        }

        // Inner screen shadow bevel
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawRect(0, 0, width - 1, height - 1);

        Font fontBold = FontManager.BOLD != null ? FontManager.BOLD : new Font("Monospaced", Font.BOLD, 14);
        Font fontReg = FontManager.REGULAR != null ? FontManager.REGULAR : new Font("Monospaced", Font.PLAIN, 12);

        // Header Title (Centered at Y=18)
        g2.setFont(fontBold.deriveFont(15f));
        g2.setColor(DisplayColor.WHITE.getColor());
        FontMetrics fmHeader = g2.getFontMetrics();
        int headerX = (width - fmHeader.stringWidth(headerTitle)) / 2;
        g2.drawString(headerTitle, headerX, 18);

        // Subheaders (Y=32)
        g2.setFont(fontReg.deriveFont(11f));
        g2.setColor(DisplayColor.WHITE_DIM.getColor());
        if (leftSubheader != null && !leftSubheader.isEmpty()) {
            g2.drawString(leftSubheader, 65, 32);
        }
        if (rightSubheader != null && !rightSubheader.isEmpty()) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(rightSubheader, width - 65 - fm.stringWidth(rightSubheader), 32);
        }

        // 6 Line Select Key Rows aligned with LSK center Y = (56 + i * 40)
        int firstCenterY = 56;
        int rowGap = 40;

        for (int i = 0; i < 6; i++) {
            LineItem left = leftLines[i];
            LineItem right = rightLines[i];

            if (left == null) left = new LineItem();
            if (right == null) right = new LineItem();

            int centerY = firstCenterY + (i * rowGap);
            int yLabel = centerY - 7;
            int yValue = centerY + 7;

            // Small Label
            g2.setFont(fontReg.deriveFont(10.5f));

            if (left.label != null && !left.label.isEmpty()) {
                g2.setColor(left.labelColor != null ? left.labelColor.getColor() : DisplayColor.WHITE_DIM.getColor());
                g2.drawString(left.label, 12, yLabel);
            }
            if (right.label != null && !right.label.isEmpty()) {
                g2.setColor(right.labelColor != null ? right.labelColor.getColor() : DisplayColor.WHITE_DIM.getColor());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(right.label, width - 12 - fm.stringWidth(right.label), yLabel);
            }

            // Large Value
            g2.setFont(fontBold.deriveFont(14f));

            if (left.value != null && !left.value.isEmpty()) {
                g2.setColor(left.valueColor != null ? left.valueColor.getColor() : DisplayColor.WHITE.getColor());
                g2.drawString(left.value, 12, yValue);
            }
            if (right.value != null && !right.value.isEmpty()) {
                g2.setColor(right.valueColor != null ? right.valueColor.getColor() : DisplayColor.WHITE.getColor());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(right.value, width - 12 - fm.stringWidth(right.value), yValue);
            }
        }

        // Scratchpad / Input Line at bottom (Y=282)
        g2.setFont(fontBold.deriveFont(13.5f));
        g2.setColor(DisplayColor.WHITE.getColor());
        if (scratchpad != null && !scratchpad.isEmpty()) {
            g2.drawString(scratchpad, 12, height - 18);
        }

        // Status Line at very bottom (Y=295)
        g2.setFont(fontReg.deriveFont(10.5f));
        g2.setColor(DisplayColor.WHITE_DIM.getColor());
        if (statusText != null && !statusText.isEmpty()) {
            g2.drawString(statusText, 12, height - 5);
        }

        g2.dispose();
    }
}
