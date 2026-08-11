package gui2.components;

import gui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

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
    private Runnable onResizeListener;

    private final LineItem[] leftLines = new LineItem[6];
    private final LineItem[] rightLines = new LineItem[6];

    public CduDisplay() {
        setOpaque(true);
        setBackground(new Color(3, 4, 6));
        setPreferredSize(new Dimension(380, 300));
        setMinimumSize(new Dimension(280, 250));

        for (int i = 0; i < 6; i++) {
            leftLines[i] = new LineItem();
            rightLines[i] = new LineItem();
        }

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (onResizeListener != null) {
                    onResizeListener.run();
                }
            }
        });
    }

    public void setOnResizeListener(Runnable listener) {
        this.onResizeListener = listener;
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

    public int getLineMaxCharCount() {
        Font fontDisplay = FontManager.PMDG != null ? FontManager.PMDG : (FontManager.BOLD != null ? FontManager.BOLD : new Font("Monospaced", Font.BOLD, 13));
        FontMetrics fm = getFontMetrics(fontDisplay.deriveFont(13f));
        int availWidth = Math.max(100, getWidth() - 24);
        int charWidth = fm.charWidth('W');
        if (charWidth <= 0) charWidth = 10;
        return Math.max(20, availWidth / charWidth);
    }

    private TexturePaint lcdGridTexture;

    private TexturePaint getLcdGridTexture() {
        if (lcdGridTexture == null) {
            int cellSize = 2; // High-density 2x2 micro LCD subpixel cell
            BufferedImage img = new BufferedImage(cellSize, cellSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();

            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, cellSize, cellSize);

            // Fine high-DPI LCD Subpixel Grid Seams (1px right & bottom borders at 8% opacity)
            g.setColor(new Color(0, 0, 0, 20));
            g.drawLine(cellSize - 1, 0, cellSize - 1, cellSize - 1);
            g.drawLine(0, cellSize - 1, cellSize - 1, cellSize - 1);

            // Subpixel aperture highlight
            g.setColor(new Color(255, 255, 255, 6));
            g.fillRect(0, 0, 1, 1);

            g.dispose();
            lcdGridTexture = new TexturePaint(img, new Rectangle(0, 0, cellSize, cellSize));
        }
        return lcdGridTexture;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            g2.dispose();
            return;
        }

        // Render text & display content onto an off-screen buffer with max quality hints
        BufferedImage textBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = textBuffer.createGraphics();
        tg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        tg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        tg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        tg.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        tg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        Font fontBase = FontManager.PMDG != null ? FontManager.PMDG : (FontManager.BOLD != null ? FontManager.BOLD : new Font("Monospaced", Font.BOLD, 13));
        Font fontBold = fontBase.deriveFont(Font.BOLD, 13f);
        Font fontReg = fontBase.deriveFont(Font.PLAIN, 11f);

        // Header Title (Centered at Y=18; Callsign rendered in CYAN)
        tg.setFont(fontBold.deriveFont(14f));
        FontMetrics fmHeader = tg.getFontMetrics();
        if (headerTitle != null && headerTitle.contains(" - ")) {
            int dashIdx = headerTitle.indexOf(" - ");
            String csPart = headerTitle.substring(0, dashIdx);
            String restPart = headerTitle.substring(dashIdx);

            int totalW = fmHeader.stringWidth(headerTitle);
            int startX = (width - totalW) / 2;

            tg.setColor(DisplayColor.CYAN.getColor());
            tg.drawString(csPart, startX, 18);

            tg.setColor(DisplayColor.WHITE.getColor());
            tg.drawString(restPart, startX + fmHeader.stringWidth(csPart), 18);
        } else {
            tg.setColor(DisplayColor.WHITE.getColor());
            int headerX = (width - fmHeader.stringWidth(headerTitle)) / 2;
            tg.drawString(headerTitle, headerX, 18);
        }

        // Subheaders (Y=32)
        tg.setFont(fontReg.deriveFont(10f));
        tg.setColor(DisplayColor.WHITE_DIM.getColor());
        if (leftSubheader != null && !leftSubheader.isEmpty()) {
            tg.drawString(leftSubheader, 65, 32);
        }
        if (rightSubheader != null && !rightSubheader.isEmpty()) {
            FontMetrics fm = tg.getFontMetrics();
            tg.drawString(rightSubheader, width - 65 - fm.stringWidth(rightSubheader), 32);
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
            tg.setFont(fontReg.deriveFont(9.5f));

            if (left.label != null && !left.label.isEmpty()) {
                tg.setColor(left.labelColor != null ? left.labelColor.getColor() : DisplayColor.WHITE_DIM.getColor());
                tg.drawString(left.label, 12, yLabel);
            }
            if (right.label != null && !right.label.isEmpty()) {
                tg.setColor(right.labelColor != null ? right.labelColor.getColor() : DisplayColor.WHITE_DIM.getColor());
                FontMetrics fm = tg.getFontMetrics();
                tg.drawString(right.label, width - 12 - fm.stringWidth(right.label), yLabel);
            }

            // Large Value
            tg.setFont(fontBold.deriveFont(13f));

            if (left.value != null && !left.value.isEmpty()) {
                tg.setColor(left.valueColor != null ? left.valueColor.getColor() : DisplayColor.WHITE.getColor());
                tg.drawString(left.value, 12, yValue);
            }
            if (right.value != null && !right.value.isEmpty()) {
                tg.setColor(right.valueColor != null ? right.valueColor.getColor() : DisplayColor.WHITE.getColor());
                FontMetrics fm = tg.getFontMetrics();
                tg.drawString(right.value, width - 12 - fm.stringWidth(right.value), yValue);
            }
        }

        // Scratchpad / Input Line at bottom (Y=282)
        tg.setFont(fontBold.deriveFont(12.5f));
        tg.setColor(DisplayColor.WHITE.getColor());
        if (scratchpad != null && !scratchpad.isEmpty()) {
            tg.drawString(scratchpad, 12, height - 18);
        }

        // Status Line at very bottom (Y=295)
        tg.setFont(fontReg.deriveFont(9.5f));
        tg.setColor(DisplayColor.WHITE_DIM.getColor());
        if (statusText != null && !statusText.isEmpty()) {
            tg.drawString(statusText, 12, height - 5);
        }

        tg.dispose();

        // 1. Deep Dark LCD Panel Backlight Background
        g2.setColor(new Color(3, 4, 6));
        g2.fillRect(0, 0, width, height);

        // 2. High-Precision LCD Subpixel Light Diffusion / Soft Glow
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        g2.drawImage(textBuffer, -1, 0, null);
        g2.drawImage(textBuffer, 1, 0, null);
        g2.drawImage(textBuffer, 0, -1, null);
        g2.drawImage(textBuffer, 0, 1, null);
        g2.setComposite(AlphaComposite.SrcOver);

        // 3. Main Text Layer (Crisp high-resolution text)
        g2.drawImage(textBuffer, 0, 0, null);

        // 4. Fine High-Density LCD Subpixel Matrix Grid Overlay
        g2.setPaint(getLcdGridTexture());
        g2.fillRect(0, 0, width, height);

        // 5. Anti-Reflective LCD Glass Sheen
        LinearGradientPaint glassSheen = new LinearGradientPaint(
            new Point2D.Float(0, 0),
            new Point2D.Float(width, height),
            new float[]{0.0f, 0.45f, 1.0f},
            new Color[]{
                new Color(160, 185, 220, 12),
                new Color(100, 130, 180, 4),
                new Color(0, 0, 0, 20)
            }
        );
        g2.setPaint(glassSheen);
        g2.fillRect(0, 0, width, height);

        // 6. Inner Screen Bevel Frame
        g2.setColor(new Color(0, 0, 0, 220));
        g2.drawRect(0, 0, width - 1, height - 1);
        g2.setColor(new Color(255, 255, 255, 10));
        g2.drawRect(1, 1, width - 3, height - 3);

        g2.dispose();
    }
}
