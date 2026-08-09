package gui2.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * Cockpit Bezel container with realistic matte powder-coated metal texture, hardware screws,
 * MSG annunciator, and mini round status LED (OFF when disconnected, GREEN when connected, RED on error).
 */
public class CduBezel extends JPanel {

    private boolean msgLed = false;
    private boolean execLed = false;
    private boolean failLed = false;

    private Timer msgBlinkTimer;
    private boolean msgBlinkState = false;
    private TexturePaint noiseTexture;

    public CduBezel() {
        setOpaque(true);
        setBackground(new Color(30, 33, 38));
    }

    private TexturePaint getNoiseTexture() {
        if (noiseTexture == null) {
            int tileSize = 64;
            BufferedImage img = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
            java.util.Random r = new java.util.Random(12345);
            for (int y = 0; y < tileSize; y++) {
                for (int x = 0; x < tileSize; x++) {
                    int alpha = 5 + r.nextInt(12); // subtle 2-4% noise alpha
                    int val = r.nextBoolean() ? 255 : 0;
                    img.setRGB(x, y, (alpha << 24) | (val << 16) | (val << 8) | val);
                }
            }
            noiseTexture = new TexturePaint(img, new Rectangle(0, 0, tileSize, tileSize));
        }
        return noiseTexture;
    }

    public void setMsgLed(boolean active) {
        this.msgLed = active;
        if (active) {
            if (msgBlinkTimer == null) {
                msgBlinkState = true;
                msgBlinkTimer = new Timer(500, e -> {
                    msgBlinkState = !msgBlinkState;
                    repaint();
                });
                msgBlinkTimer.start();
            } else if (!msgBlinkTimer.isRunning()) {
                msgBlinkState = true;
                msgBlinkTimer.start();
            }
        } else {
            if (msgBlinkTimer != null && msgBlinkTimer.isRunning()) {
                msgBlinkTimer.stop();
            }
            msgBlinkState = false;
        }
        repaint();
    }
    public void setExecLed(boolean active) { this.execLed = active; repaint(); }
    public void setFailLed(boolean active) { this.failLed = active; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Fill background solid first to guarantee no white flicker
        g2.setColor(new Color(25, 28, 32));
        g2.fillRect(0, 0, w, h);

        // Cockpit matte base tone
        g2.setColor(new Color(42, 46, 54));
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        // Soft ambient overhead cockpit light gradient (radial from top-center)
        RadialGradientPaint ambientLight = new RadialGradientPaint(
            new Point2D.Float(w / 2.0f, 0.0f),
            h * 1.3f,
            new float[]{0.0f, 1.0f},
            new Color[]{new Color(56, 61, 70), new Color(30, 33, 38)}
        );
        g2.setPaint(ambientLight);
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        // Procedural powder-coated metal grain texture overlay
        g2.setPaint(getNoiseTexture());
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        // Outer metallic rim bevel
        g2.setColor(new Color(80, 85, 95, 100));
        g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);
        g2.setColor(new Color(15, 17, 20));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        // Hardware screws in 4 corners
        drawScrew(g2, 12, 12);
        drawScrew(g2, w - 24, 12);
        drawScrew(g2, 12, h - 24);
        drawScrew(g2, w - 24, h - 24);

        // Header Title
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(new Color(190, 195, 205));
        g2.drawString("Java CPDLC v" + service.UpdateChecker.CURRENT_VERSION, 28, 20);

        // MSG Annunciator LED (Blinks when unread message exists)
        boolean isMsgLit = msgLed && msgBlinkState;
        drawAnnunciator(g2, w - 100, 12, "MSG", isMsgLit ? new Color(255, 180, 0) : new Color(60, 45, 0));

        // Mini Round Connection Status LED (OFF when disconnected, GREEN when connected, RED on error/timeout)
        if (failLed) {
            Color coreColor = new Color(240, 50, 40);
            Color highlightColor = new Color(255, 190, 180);
            drawRoundLed(g2, w - 50, 13, 12, coreColor, highlightColor, true);
        } else if (execLed) {
            Color coreColor = new Color(0, 230, 80);
            Color highlightColor = new Color(200, 255, 210);
            drawRoundLed(g2, w - 50, 13, 12, coreColor, highlightColor, true);
        } else {
            // Off state: dark dim socket
            drawRoundLed(g2, w - 50, 13, 12, new Color(35, 38, 44), new Color(55, 58, 64), false);
        }

        g2.dispose();
    }

    private void drawScrew(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(20, 22, 26));
        g2.fill(new Ellipse2D.Double(x, y, 12, 12));
        g2.setColor(new Color(100, 105, 115));
        g2.draw(new Ellipse2D.Double(x, y, 12, 12));
        g2.setColor(new Color(150, 155, 165));
        g2.drawLine(x + 3, y + 6, x + 9, y + 6);
        g2.drawLine(x + 6, y + 3, x + 6, y + 9);
    }

    private void drawAnnunciator(Graphics2D g2, int x, int y, String label, Color ledColor) {
        g2.setColor(ledColor);
        g2.fillRoundRect(x, y, 32, 14, 4, 4);
        g2.setColor(new Color(15, 17, 20));
        g2.drawRoundRect(x, y, 32, 14, 4, 4);

        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(Color.BLACK);
        FontMetrics fm = g2.getFontMetrics();
        int strX = x + (32 - fm.stringWidth(label)) / 2;
        g2.drawString(label, strX, y + 10);
    }

    private void drawRoundLed(Graphics2D g2, int x, int y, int size, Color coreColor, Color highlightColor, boolean isLit) {
        // Metallic bezel ring
        g2.setColor(new Color(15, 17, 20));
        g2.fill(new Ellipse2D.Double(x - 1, y - 1, size + 2, size + 2));
        g2.setColor(new Color(90, 95, 105));
        g2.draw(new Ellipse2D.Double(x - 1, y - 1, size + 2, size + 2));

        // Core LED body
        g2.setColor(coreColor);
        g2.fill(new Ellipse2D.Double(x, y, size, size));

        // Glossy specular highlight reflection
        if (isLit) {
            g2.setColor(highlightColor);
            g2.fill(new Ellipse2D.Double(x + 2, y + 2, size / 3.0, size / 3.0));
        }
    }
}
