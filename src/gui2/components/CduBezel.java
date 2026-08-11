package gui2.components;

import javax.swing.*;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

        // Header Title (Aligned to top-left corner of screen container at X=66 with industrial engraved 3D effect)
        drawEngravedHeader(g2, 66, 20, "JAVA CPDLC v" + service.UpdateChecker.CURRENT_VERSION);

        // Position MSG annunciator right edge at w - 66 (aligned with black screen right edge) and status LED to its right
        int msgX = w - 110;
        int ledX = w - 54;

        // MSG Annunciator LED (Blinks when unread message exists)
        boolean isMsgLit = msgLed && msgBlinkState;
        drawAnnunciator(g2, msgX, 8, "MSG", isMsgLit);

        // Mini Round Connection Status LED (OFF when disconnected, GREEN when connected, RED on error/timeout)
        if (failLed) {
            Color coreColor = new Color(240, 50, 40);
            Color highlightColor = new Color(255, 190, 180);
            drawRoundLed(g2, ledX, 9, 12, coreColor, highlightColor, true);
        } else if (execLed) {
            Color coreColor = new Color(0, 235, 60);
            Color highlightColor = new Color(200, 255, 210);
            drawRoundLed(g2, ledX, 9, 12, coreColor, highlightColor, true);
        } else {
            // Off state: dark dim socket
            drawRoundLed(g2, ledX, 9, 12, new Color(35, 38, 44), new Color(55, 58, 64), false);
        }

        g2.dispose();
    }



    private void drawEngravedHeader(Graphics2D g2, int x, int y, String text) {
        Font font = new Font("Arial", Font.BOLD, 12);
        try {
            Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
            attributes.put(TextAttribute.TRACKING, 0.05);
            font = font.deriveFont(attributes);
        } catch (Throwable ignored) {}

        g2.setFont(font);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GlyphVector gv = font.createGlyphVector(g2.getFontRenderContext(), text);
        Shape textShape = gv.getOutline(x, y);

        // 1. Light text-shadow offset down and right (light catch on lower/right cut edge)
        Shape lightShadow = gv.getOutline(x + 0.5f, y + 0.8f);
        g2.setColor(new Color(255, 255, 255, 75));
        g2.fill(lightShadow);

        // 2. Dark top/inner shadow offset up (groove depth shadow)
        Shape darkShadow = gv.getOutline(x - 0.3f, y - 0.6f);
        g2.setColor(new Color(10, 12, 16, 180));
        g2.fill(darkShadow);

        // 3. Dark engraved text body (carved directly into background material with matching noise grain)
        g2.setColor(new Color(18, 21, 26));
        g2.fill(textShape);
        g2.setPaint(getNoiseTexture());
        g2.fill(textShape);

    }

    private void drawScrew(Graphics2D g2, int x, int y) {
        int size = 12;
        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;

        // 1. Recessed countersink shadow — subtle, screw is flush-mounted not raised
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fill(new Ellipse2D.Double(x - 1, y - 0.5, size + 2, size + 2));

        // 2. Screw head base — flat matte dark metal, very gentle gradient (not glossy)
        Point2D headCenter = new Point2D.Float(centerX - size * 0.12f, centerY - size * 0.12f);
        RadialGradientPaint headMetal = new RadialGradientPaint(
                headCenter,
                size * 0.8f,
                new float[]{0.0f, 0.6f, 1.0f},
                new Color[]{
                        new Color(58, 60, 66),
                        new Color(38, 40, 45),
                        new Color(24, 25, 29)
                }
        );
        Ellipse2D.Double screwHead = new Ellipse2D.Double(x, y, size, size);
        g2.setPaint(headMetal);
        g2.fill(screwHead);
        g2.setPaint(getNoiseTexture());
        g2.fill(screwHead);

        // 3. Thin machined rim ring — subtle lighter edge, not a glossy arc
        g2.setStroke(new BasicStroke(0.8f));
        g2.setColor(new Color(95, 98, 105, 160));
        g2.draw(new Ellipse2D.Double(x + 0.5, y + 0.5, size - 1, size - 1));

        // 4. Outer contact edge — dark seam where head meets panel
        g2.setColor(new Color(12, 13, 15));
        g2.draw(new Ellipse2D.Double(x, y, size, size));

        // 5. Faint top-left highlight sliver — just enough to read as slightly domed, not glossy
        g2.setColor(new Color(120, 123, 130, 60));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new Arc2D.Double(x + 1.3, y + 1.3, size - 2.6, size - 2.6, 110, 70, Arc2D.OPEN));

        // 6. Phillips slot — dark, low-contrast grooves (subtle, not carved-looking)
        double cx = centerX, cy = centerY;
        double armLen = size * 0.27;
        double armHalfW = 0.7;

        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2.setColor(new Color(10, 11, 13, 190));
        g2.draw(new Line2D.Double(cx - armLen, cy + armHalfW, cx + armLen, cy + armHalfW));
        g2.draw(new Line2D.Double(cx + armHalfW, cy - armLen, cx + armHalfW, cy + armLen));

        g2.setColor(new Color(70, 72, 78, 90));
        g2.draw(new Line2D.Double(cx - armLen, cy - armHalfW, cx + armLen, cy - armHalfW));
        g2.draw(new Line2D.Double(cx - armHalfW, cy - armLen, cx - armHalfW, cy + armLen));

        // 7. Tiny center pit
        g2.setColor(new Color(6, 6, 8, 150));
        g2.fill(new Ellipse2D.Double(cx - 1.0, cy - 1.0, 2.0, 2.0));
    }

    private void drawAnnunciator(Graphics2D g2, int x, int y, String label, boolean isLit) {
        int width = 44;
        int height = 15;
        int cornerRadius = 4;

        RoundRectangle2D.Float socketOuter = new RoundRectangle2D.Float(
                x - 1, y - 1, width + 2, height + 2, cornerRadius + 2, cornerRadius + 2);
        RoundRectangle2D.Float lensShape = new RoundRectangle2D.Float(
                x, y, width, height, cornerRadius, cornerRadius);

        // 0. Cast shadow onto panel — grounds the whole unit, offset down-right from an
        //    implied upper-left light source
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fill(new RoundRectangle2D.Float(x, y + 1.5f, width, height, cornerRadius + 2, cornerRadius + 2));

        // 1. Dark bezel socket cut into polymer panel
        g2.setColor(new Color(15, 17, 21));
        g2.fill(socketOuter);

        // 2. Socket rim — diagonal bevel stroke on the ACTUAL shape (not a mismatched arc),
        //    lit edge upper-left, shadow edge lower-right
        g2.setStroke(new BasicStroke(1.0f));
        g2.setPaint(new GradientPaint(x - 1, y - 1, new Color(80, 85, 95),
                x + width, y + height, new Color(30, 32, 38)));
        g2.draw(socketOuter);

        // 4. Lens fill
        if (isLit) {
            // a. Base saturated amber gradient — deeper/richer than a generic yellow-orange,
            //    darker at bottom to imply the light source sits toward the top of the lens
            LinearGradientPaint lensGradient = new LinearGradientPaint(
                    new Point2D.Float(x, y), new Point2D.Float(x, y + height),
                    new float[]{0.0f, 0.45f, 1.0f},
                    new Color[]{new Color(255, 200, 60), new Color(250, 150, 10), new Color(195, 105, 0)}
            );
            g2.setPaint(lensGradient);
            g2.fill(lensShape);

            // b. Bright hotspot — real lit lenses have a concentrated bright core near the
            //    filament/LED, not a uniform gradient across the whole surface
            Shape oldClip = g2.getClip();
            g2.clip(lensShape);
            RadialGradientPaint hotspot = new RadialGradientPaint(
                    new Point2D.Float(x + width * 0.4f, y + height * 0.4f),
                    width * 0.5f,
                    new float[]{0.0f, 0.5f, 1.0f},
                    new Color[]{new Color(255, 235, 150, 200), new Color(255, 180, 40, 90), new Color(255, 150, 0, 0)}
            );
            g2.setPaint(hotspot);
            g2.fill(lensShape);

            // c. Diagonal glass glare streak — angled and tapered, clipped to the lens,
            //    reads as a curved glossy surface instead of a flat highlight bar
            Graphics2D gg = (Graphics2D) g2.create();
            gg.rotate(Math.toRadians(-20), x + width * 0.3, y + height * 0.3);
            GradientPaint glare = new GradientPaint(
                    x, y - 2, new Color(255, 255, 235, 150),
                    x, y + 5, new Color(255, 255, 235, 0));
            gg.setPaint(glare);
            gg.fill(new RoundRectangle2D.Float(x - 2, y - 3, width * 0.55f, 5f, 3, 3));
            gg.dispose();
            g2.setClip(oldClip);

            // d. Bottom inner shadow — the far edge of a curved lens naturally falls off
            //    darker; without this the bottom edge looks pasted-on
            g2.setPaint(new GradientPaint(
                    x, y + height * 0.6f, new Color(0, 0, 0, 0),
                    x, y + height, new Color(0, 0, 0, 90)));
            Shape oldClip2 = g2.getClip();
            g2.clip(lensShape);
            g2.fill(new Rectangle2D.Float(x, y + height * 0.6f, width, height * 0.4f));
            g2.setClip(oldClip2);

        } else {
            // a. Base lens material — off-center radial, molded translucent plastic look
            RadialGradientPaint lensBase = new RadialGradientPaint(
                    new Point2D.Float(x + width * 0.35f, y + height * 0.3f), width * 0.9f,
                    new float[]{0.0f, 0.6f, 1.0f},
                    new Color[]{new Color(54, 40, 14), new Color(38, 28, 10), new Color(24, 17, 6)}
            );
            g2.setPaint(lensBase);
            g2.fill(lensShape);

            Shape oldClip = g2.getClip();
            g2.clip(lensShape);

            // b. Bulb/filament silhouette faintly visible behind unlit translucent lens
            g2.setColor(new Color(12, 9, 3, 90));
            g2.fill(new Ellipse2D.Double(x + width * 0.5 - 5, y + height * 0.5 - 2.5, 10, 5));

            // c. Faint ambient glass glare — same angled streak as the lit state, far dimmer,
            //    so the material reads as the same physical object in both states
            Graphics2D gg = (Graphics2D) g2.create();
            gg.rotate(Math.toRadians(-20), x + width * 0.3, y + height * 0.3);
            GradientPaint glare = new GradientPaint(
                    x, y - 2, new Color(160, 140, 90, 40),
                    x, y + 5, new Color(160, 140, 90, 0));
            gg.setPaint(glare);
            gg.fill(new RoundRectangle2D.Float(x - 2, y - 3, width * 0.55f, 5f, 3, 3));
            gg.dispose();

            g2.setClip(oldClip);

            // d. Beveled edge on the real shape — light upper-left, shadow lower-right
            g2.setStroke(new BasicStroke(0.8f));
            g2.setPaint(new GradientPaint(x, y, new Color(75, 58, 26, 140),
                    x + width, y + height, new Color(6, 5, 2, 160)));
            g2.draw(lensShape);
        }

        // 5. Text
        Font font = new Font("Arial", Font.BOLD, 10);
        try {
            Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
            attributes.put(TextAttribute.TRACKING, 0.06);
            font = font.deriveFont(attributes);
        } catch (Throwable ignored) {}

        g2.setFont(font);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        FontMetrics fm = g2.getFontMetrics(font);
        int strX = x + (width - fm.stringWidth(label)) / 2;
        int strY = y + ((height - fm.getHeight()) / 2) + fm.getAscent();

        if (isLit) {
            float centerX = strX + fm.stringWidth(label) / 2.0f;
            float centerY = strY - (fm.getAscent() / 2.0f);
            RadialGradientPaint textDiffusion = new RadialGradientPaint(
                    new Point2D.Float(centerX, centerY), 20.0f,
                    new float[]{0.0f, 0.45f, 1.0f},
                    new Color[]{new Color(255, 240, 160, 190), new Color(255, 180, 30, 100), new Color(255, 130, 0, 0)}
            );
            g2.setPaint(textDiffusion);
            g2.fillRoundRect(x + 4, y + 1, width - 8, height - 2, 4, 4);

            // Dark saturated text — reads as sitting under a bright translucent lens
            g2.setColor(new Color(70, 45, 10));
            g2.drawString(label, strX, strY);
        } else {
            g2.setColor(new Color(90, 68, 24));
            g2.drawString(label, strX, strY);
        }

// 3. Ambient light spill — now an actual soft elliptical bloom, not a clipped rounded-rect
        if (isLit) {
            float centerX = x + width / 2.0f;
            float centerY = y + height / 2.0f;

            float glowRx = width * 0.85f;   // horizontal reach
            float glowRy = height * 2.0f;   // vertical reach — taller since box is thin

            AffineTransform glowTransform = new AffineTransform();
            glowTransform.translate(centerX, centerY);
            glowTransform.scale(glowRx, glowRy);

            RadialGradientPaint boxGlow = new RadialGradientPaint(
                    new Point2D.Float(0, 0), 1.0f, new Point2D.Float(0, 0),
                    new float[]{0.00f, 0.35f, 0.55f, 0.75f, 1.00f},
                    new Color[]{
                            new Color(255, 150, 0, 95), new Color(255, 150, 0, 72),
                            new Color(255, 150, 0, 40), new Color(255, 150, 0, 15),
                            new Color(255, 150, 0, 0)
                    },
                    MultipleGradientPaint.CycleMethod.NO_CYCLE,
                    MultipleGradientPaint.ColorSpaceType.SRGB, glowTransform
            );
            g2.setPaint(boxGlow);

            // Fill an ellipse sized to match the gradient's actual reach, not a rounded rect
            g2.fill(new Ellipse2D.Float(centerX - glowRx, centerY - glowRy,
                    glowRx * 2, glowRy * 2));
        }
    }

    private void drawRoundLed(Graphics2D g2, int x, int y, int size, Color coreColor, Color highlightColor, boolean isLit) {
        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;

        // 1. Metallic bezel ring — drawn first, as the base
        if (!isLit) {
            g2.setColor(new Color(15, 17, 20));
            g2.fill(new Ellipse2D.Double(x - 1, y - 1, size + 2, size + 2));
            g2.setColor(new Color(90, 95, 105));
            g2.draw(new Ellipse2D.Double(x - 1, y - 1, size+2, size+2));
        }

        // 2. Core LED body
        g2.setColor(coreColor);
        g2.fill(new Ellipse2D.Double(x, y, size, size));

        // 3. Glossy specular highlight
        g2.setColor(highlightColor);
        g2.fill(new Ellipse2D.Double(x + 2, y + 2, size / 3.0, size / 3.0));


        // 4. Ambient glow — now drawn ON TOP of the bezel, spilling over it
        if (isLit) {
            Point2D center = new Point2D.Float(centerX, centerY);

            int r = coreColor.getRed();
            int g = coreColor.getGreen();
            int b = coreColor.getBlue();

            float glowRadius = size * 1.5f;

            RadialGradientPaint ledGlow = new RadialGradientPaint(
                    center,
                    glowRadius,
                    new float[]{0.00f, 0.35f, 0.55f, 0.75f, 1.00f},
                    new Color[]{
                            new Color(r, g, b, 90),
                            new Color(r, g, b, 70),
                            new Color(r, g, b, 40),
                            new Color(r, g, b, 15),
                            new Color(r, g, b, 0)
                    }
            );

            g2.setPaint(ledGlow);
            double glowDiameter = glowRadius * 2.0;
            double glowX = centerX - glowRadius;
            double glowY = centerY - glowRadius;
            g2.fill(new Ellipse2D.Double(glowX, glowY, glowDiameter, glowDiameter));
        }


    }
}
