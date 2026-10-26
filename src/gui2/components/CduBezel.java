package gui2.components;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Cockpit Bezel container with realistic matte bead-blasted / powder-coated avionics metal texture,
 * 3D recessed display shroud, hardware screws, MSG annunciator, and mini round status LED.
 */
public class CduBezel extends JPanel {

    private boolean msgLed = false;
    private boolean execLed = false;
    private boolean failLed = false;

    private Timer msgBlinkTimer;
    private boolean msgBlinkState = false;
    private TexturePaint noiseTexture;
    private TexturePaint avionicsTexture;

    protected final CduScrewButton closeScrewButton;
    protected final CduScrewButton minimizeScrewButton;
    private boolean screwButtonsEnabled = false;

    public CduBezel() {
        setOpaque(true);
        setBackground(new Color(38, 42, 49));
        setLayout(null);

        closeScrewButton = new CduScrewButton(CduScrewButton.Mode.CLOSE);
        closeScrewButton.setBounds(7, 7, 22, 22);
        closeScrewButton.setVisible(false);
        add(closeScrewButton);

        minimizeScrewButton = new CduScrewButton(CduScrewButton.Mode.MINIMIZE);
        minimizeScrewButton.setBounds(32, 7, 22, 22);
        minimizeScrewButton.setVisible(false);
        add(minimizeScrewButton);
    }

    public CduScrewButton getCloseScrewButton() {
        return closeScrewButton;
    }

    public CduScrewButton getMinimizeScrewButton() {
        return minimizeScrewButton;
    }

    public void setScrewButtonsEnabled(boolean enabled) {
        this.screwButtonsEnabled = enabled;
        if (closeScrewButton != null) {
            closeScrewButton.setVisible(enabled);
        }
        if (minimizeScrewButton != null) {
            minimizeScrewButton.setVisible(enabled);
        }
        repaint();
    }

    public boolean isScrewButtonsEnabled() {
        return screwButtonsEnabled;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (closeScrewButton != null) {
            closeScrewButton.setBounds(7, 7, 22, 22);
        }
        if (minimizeScrewButton != null) {
            minimizeScrewButton.setBounds(32, 7, 22, 22);
        }
    }

    private TexturePaint getNoiseTexture() {
        if (noiseTexture == null) {
            int tileSize = 64;
            BufferedImage img = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
            java.util.Random r = new java.util.Random(12345);
            for (int y = 0; y < tileSize; y++) {
                for (int x = 0; x < tileSize; x++) {
                    int alpha = 5 + r.nextInt(10);
                    int val = r.nextBoolean() ? 255 : 0;
                    img.setRGB(x, y, (alpha << 24) | (val << 16) | (val << 8) | val);
                }
            }
            noiseTexture = new TexturePaint(img, new Rectangle(0, 0, tileSize, tileSize));
        }
        return noiseTexture;
    }

    /**
     * Generates a seamless 256x256 matte powder-coated / bead-blasted avionics metal texture.
     * Uses a multi-scale heightmap with top-left directional bump shading so micro-grains
     * catch subtle light and shadow.
     */
    private TexturePaint getAvionicsTexture() {
        if (avionicsTexture == null) {
            final int size = 256;
            final int mask = size - 1;
            java.util.Random rnd = new java.util.Random(420737);

            float[][] raw = new float[size][size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    raw[y][x] = (rnd.nextFloat() - 0.5f) * 2f;
                }
            }

            // 1. Fine bead-blast micro-grain
            float[][] fine = new float[size][size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float sum = raw[y][x] * 0.36f
                            + (raw[y][(x - 1) & mask] + raw[y][(x + 1) & mask]
                            + raw[(y - 1) & mask][x] + raw[(y + 1) & mask][x]) * 0.11f
                            + (raw[(y - 1) & mask][(x - 1) & mask] + raw[(y - 1) & mask][(x + 1) & mask]
                            + raw[(y + 1) & mask][(x - 1) & mask] + raw[(y + 1) & mask][(x + 1) & mask]) * 0.05f;
                    fine[y][x] = sum;
                }
            }

            // 2. Medium powder-coat stipple
            float[][] med = new float[size][size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float s = 0f;
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            s += fine[(y + dy) & mask][(x + dx) & mask];
                        }
                    }
                    med[y][x] = s / 25f;
                }
            }

            // 3. Broad subtle cast-metal mottling
            float[][] broad = new float[size][size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float s = 0f;
                    for (int dy = -6; dy <= 6; dy += 2) {
                        for (int dx = -6; dx <= 6; dx += 2) {
                            s += med[(y + dy) & mask][(x + dx) & mask];
                        }
                    }
                    broad[y][x] = s / 49f;
                }
            }

            float[][] heightMap = new float[size][size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    heightMap[y][x] = fine[y][x] * 0.52f + med[y][x] * 0.95f + broad[y][x] * 1.15f;
                }
            }

            // Base color matches original slate blue-gray cockpit tone: RGB(42, 46, 54)
            final float baseR = 42f;
            final float baseG = 46f;
            final float baseB = 54f;

            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float dx = heightMap[y][x] - heightMap[y][(x - 1) & mask];
                    float dy = heightMap[y][x] - heightMap[(y - 1) & mask][x];
                    float bump = (dx * 0.65f + dy * 0.85f) * 2.4f;

                    float cavity = heightMap[y][x] * 0.9f + broad[y][x] * 1.6f;
                    float delta = bump + cavity;

                    int r = clampRgb(Math.round(baseR + delta * 0.92f));
                    int g = clampRgb(Math.round(baseG + delta * 0.96f));
                    int b = clampRgb(Math.round(baseB + delta * 1.05f));

                    img.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }

            avionicsTexture = new TexturePaint(img, new Rectangle(0, 0, size, size));
        }
        return avionicsTexture;
    }

    private static int clampRgb(int val) {
        return Math.max(0, Math.min(255, val));
    }

    /**
     * Paints the base bead-blasted texture + cockpit overhead lighting onto the current clip/shape.
     * Used by both paintComponent and the inward bezel shroud so their lighting matches seamlessly.
     */
    private void paintAvionicsSurface(Graphics2D g2, Shape target, int w, int h) {
        g2.setPaint(getAvionicsTexture());
        g2.fill(target);

        LinearGradientPaint verticalFalloff = new LinearGradientPaint(
                new Point2D.Float(0, 0),
                new Point2D.Float(0, h),
                new float[]{0.0f, 0.22f, 0.72f, 1.0f},
                new Color[]{
                        new Color(195, 210, 230, 18),
                        new Color(180, 195, 215, 5),
                        new Color(10, 12, 16, 12),
                        new Color(8, 10, 14, 42)
                }
        );
        g2.setPaint(verticalFalloff);
        g2.fill(target);

        float sheenRadius = Math.max(w, h) * 0.95f;
        RadialGradientPaint overheadSheen = new RadialGradientPaint(
                new Point2D.Float(w * 0.46f, -h * 0.08f),
                sheenRadius,
                new float[]{0.0f, 0.45f, 1.0f},
                new Color[]{
                        new Color(210, 222, 240, 20),
                        new Color(180, 195, 215, 6),
                        new Color(12, 14, 18, 28)
                }
        );
        g2.setPaint(overheadSheen);
        g2.fill(target);
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
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // 1. Matte bead-blasted powder-coat base texture & cockpit lighting
        paintAvionicsSurface(g2, new Rectangle(0, 0, w, h), w, h);

        // 2. Machined 3D outer panel chamfer & edge lighting
        drawOuterMachinedBevel(g2, w, h);

        // 3. Recessed counterbore pockets & hardware screws in corners
        if (!screwButtonsEnabled) {
            drawScrew(g2, 12, 12);
        } else {
            drawScrewCounterbore(g2, 12, 12, 12);
        }
        drawScrew(g2, w - 24, 12);
        drawScrew(g2, 12, h - 24);
        drawScrew(g2, w - 24, h - 24);

        // 4. Header Title (Aligned to top-left corner of screen container at X=66)
        drawStickerHeader(g2, 66, 20, "JAVA CPDLC v" + service.UpdateChecker.CURRENT_VERSION);


        // Position MSG annunciator right edge at w - 66 and status LED to its right
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
            drawRoundLed(g2, ledX, 9, 12, new Color(35, 38, 44), new Color(55, 58, 64), false);
        }

        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);

        // Replace the flat light-gray border of the screen container with a smooth inward 3D recessed bezel
        Rectangle screenBounds = findScreenBounds();
        if (screenBounds != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            drawScreenRecessBevelInward(g2, screenBounds, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private Rectangle findScreenBounds() {
        for (Component c : getComponents()) {
            if (c != closeScrewButton && c != minimizeScrewButton && c.isVisible()) {
                if (c.getWidth() > 140 && c.getHeight() > 120) {
                    return c.getBounds();
                }
            }
        }
        return null;
    }

    /**
     * Draws a realistic, smooth 3D molded/machined bezel recess INWARD over the entire 7px
     * light-gray border of the screen component. Matches the natural shading of the first version
     * without harsh miter lines or leftover inner gray stripes.
     */
    private void drawScreenRecessBevelInward(Graphics2D g2, Rectangle r, int panelW, int panelH) {
        int ox = r.x;
        int oy = r.y;
        int ow = r.width;
        int oh = r.height;
        int bevel = 7; // Covers the entire 7px light-gray border right up to the black screen

        int ix = ox + bevel;
        int iy = oy + bevel;
        int iw = ow - bevel * 2;
        int ih = oh - bevel * 2;

        // 1. Overwrite the entire flat light-gray border with the exact same lit avionics metal surface
        Area frameArea = new Area(new Rectangle(ox, oy, ow, oh));
        frameArea.subtract(new Area(new Rectangle(ix, iy, iw, ih)));
        paintAvionicsSurface(g2, frameArea, panelW, panelH);

        // 2. Soft outer ridge transition along the top edge (subtle bend shadow, not a sharp black line)
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(12, 14, 18, 80));
        g2.drawLine(ox, oy, ox + ow - 1, oy);

        // 3. Top sloped wall (faces down, away from overhead light -> smooth cast shadow)
        Polygon topWall = new Polygon(
                new int[]{ox, ox + ow, ix + iw, ix},
                new int[]{oy, oy, iy, iy}, 4);
        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, oy), new Point2D.Float(0, iy),
                new float[]{0.0f, 0.35f, 1.0f},
                new Color[]{
                        new Color(14, 16, 20, 165),
                        new Color(18, 21, 26, 140),
                        new Color(22, 25, 30, 105)
                }));
        g2.fill(topWall);

        // 4. Left sloped wall (angled shadow, matching the first version's natural falloff)
        Polygon leftWall = new Polygon(
                new int[]{ox, ix, ix, ox},
                new int[]{oy, iy, iy + ih, oy + oh}, 4);
        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(ox, 0), new Point2D.Float(ix, 0),
                new float[]{0.0f, 0.45f, 1.0f},
                new Color[]{
                        new Color(20, 23, 28, 135),
                        new Color(24, 27, 33, 110),
                        new Color(28, 31, 37, 85)
                }));
        g2.fill(leftWall);

        // 5. Right sloped wall (catches subtle side bounce light toward the outer ridge)
        Polygon rightWall = new Polygon(
                new int[]{ix + iw, ox + ow, ox + ow, ix + iw},
                new int[]{iy, oy, oy + oh, iy + ih}, 4);
        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(ix + iw, 0), new Point2D.Float(ox + ow, 0),
                new float[]{0.0f, 0.55f, 1.0f},
                new Color[]{
                        new Color(28, 32, 38, 85),
                        new Color(44, 49, 58, 85),
                        new Color(58, 64, 74, 90)
                }));
        g2.fill(rightWall);

        // 6. Bottom sloped wall (faces up toward overhead cockpit light -> natural metallic sheen)
        Polygon bottomWall = new Polygon(
                new int[]{ix, ix + iw, ox + ow, ox},
                new int[]{iy + ih, iy + ih, oy + oh, oy + oh}, 4);
        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, iy + ih), new Point2D.Float(0, oy + oh),
                new float[]{0.0f, 0.5f, 1.0f},
                new Color[]{
                        new Color(52, 58, 67, 90),
                        new Color(70, 77, 89, 108),
                        new Color(86, 94, 108, 120)
                }));
        g2.fill(bottomWall);

        // 7. Subtle bottom ridge catch-light where bottom slope meets the flat front panel
        g2.setColor(new Color(115, 125, 140, 72));
        g2.drawLine(ox + 1, oy + oh - 1, ox + ow - 2, oy + oh - 1);

        // 8. Soft recessed depth shadow where the metal shroud meets the black screen glass
        g2.setColor(new Color(8, 10, 14, 150));
        g2.drawRect(ix, iy, iw - 1, ih - 1);
        g2.setColor(new Color(0, 0, 0, 75));
        g2.drawLine(ix + 1, iy + 1, ix + iw - 2, iy + 1);
        g2.drawLine(ix + 1, iy + 2, ix + 1, iy + ih - 2);
    }

    /**
     * Draws a realistic 3D machined chamfer along the outer perimeter of the unit.
     */
    private void drawOuterMachinedBevel(Graphics2D g2, int w, int h) {
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(110, 120, 135, 85));
        g2.drawLine(2, 1, w - 3, 1);
        g2.setColor(new Color(85, 92, 105, 38));
        g2.drawLine(2, 2, w - 3, 2);

        g2.setColor(new Color(90, 98, 112, 55));
        g2.drawLine(1, 2, 1, h - 3);

        g2.setColor(new Color(14, 16, 20, 155));
        g2.drawLine(2, h - 2, w - 2, h - 2);
        g2.drawLine(w - 2, 2, w - 2, h - 2);

        g2.setColor(new Color(18, 20, 25, 80));
        g2.drawLine(3, h - 3, w - 3, h - 3);
        g2.drawLine(w - 3, 3, w - 3, h - 3);

        g2.setColor(new Color(12, 14, 17));
        g2.drawRect(0, 0, w - 1, h - 1);
    }

    private void drawScrewCounterbore(Graphics2D g2, int x, int y, int size) {
        float cx = x + size / 2.0f;
        float cy = y + size / 2.0f;
        float r = size * 0.78f;

        RadialGradientPaint pocket = new RadialGradientPaint(
                new Point2D.Float(cx, cy), r,
                new float[]{0.0f, 0.65f, 1.0f},
                new Color[]{new Color(10, 12, 15, 95), new Color(14, 16, 20, 45), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(pocket);
        g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

        g2.setStroke(new BasicStroke(0.9f));
        g2.setColor(new Color(95, 103, 116, 55));
        g2.draw(new Arc2D.Double(x - 1.5, y - 1.5, size + 3, size + 3, 285, 115, Arc2D.OPEN));
    }


    private TexturePaint cleanPaperPaint;

    private void drawStickerHeader(Graphics2D g2, int x, int y, String text) {
        AffineTransform oldTransform = g2.getTransform();
        Composite oldComposite = g2.getComposite();
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        Font oldFont = g2.getFont();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Font font = new Font("Arial", Font.BOLD, 10);
        try {
            Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
            attributes.put(TextAttribute.TRACKING, 0.04f);
            font = font.deriveFont(attributes);
        } catch (Throwable ignored) {}
        g2.setFont(font);

        FontRenderContext frc = g2.getFontRenderContext();
        GlyphVector gv = font.createGlyphVector(frc, text);
        Rectangle2D textBounds = gv.getVisualBounds();

        float padX = 6.2f;
        float padY = 3.2f;
        float textX = x + 2.0f;
        float textY = y - 1.0f;

        float rectX = (float) (textX + textBounds.getX() - padX);
        float rectY = (float) (textY + textBounds.getY() - padY);
        float rectW = (float) (textBounds.getWidth() + padX * 2.0f);
        float rectH = (float) (textBounds.getHeight() + padY * 2.0f);
        // Rounded die-cut sticker corners
        float corner = 3.2f;

        double centerX = rectX + rectW / 2.0;
        double centerY = rectY + rectH / 2.0;
        g2.rotate(Math.toRadians(-0.45), centerX, centerY);

        // 1. Inset bottom-only shadow (inset horizontally so it never pokes out around the edges as a black line)
        g2.setColor(new Color(0, 0, 0, 38));
        g2.fill(new RoundRectangle2D.Float(rectX + 1.0f, rectY + 0.75f, rectW - 2.0f, rectH, corner, corner));

        // 2. Single-pass matte paper fill (no setClip used, preserving pure anti-aliased cream edges)
        RoundRectangle2D stickerShape = new RoundRectangle2D.Float(rectX, rectY, rectW, rectH, corner, corner);
        g2.setPaint(getCleanPaperTexture());
        g2.fill(stickerShape);

        // 3. Subtle bright paper-cut edge thickness (prevents dark anti-aliasing fringe against the dark panel)
        g2.setColor(new Color(246, 243, 235, 130));
        g2.setStroke(new BasicStroke(0.45f));
        g2.draw(new RoundRectangle2D.Float(rectX + 0.2f, rectY + 0.2f, rectW - 0.4f, rectH - 0.4f, corner, corner));

        // 4. Crisp matte charcoal ink
        g2.setColor(new Color(38, 37, 36, 238));
        g2.drawGlyphVector(gv, textX, textY);

        g2.setFont(oldFont);
        g2.setStroke(oldStroke);
        g2.setPaint(oldPaint);
        g2.setComposite(oldComposite);
        g2.setTransform(oldTransform);
    }

    // Generates a clean, uniform matte cream paper surface without blotchy lines
    private TexturePaint getCleanPaperTexture() {
        if (cleanPaperPaint != null) {
            return cleanPaperPaint;
        }

        int size = 64;
        BufferedImage tex = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Random rng = new Random(2026L);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Very subtle +-2 tonal variation for smooth matte sticker paper
                int grain = rng.nextInt(5) - 2;
                int r = 238 + grain;
                int g = 234 + grain;
                int b = 223 + grain;
                tex.setRGB(x, y, (255 << 24) | (r << 16) | (g << 8) | b);
            }
        }

        cleanPaperPaint = new TexturePaint(tex, new Rectangle2D.Float(0, 0, size, size));
        return cleanPaperPaint;
    }

    private void drawScrew(Graphics2D g2, int x, int y) {
        int size = 12;
        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;

        drawScrewCounterbore(g2, x, y, size);

        g2.setColor(new Color(0, 0, 0, 85));
        g2.fill(new Ellipse2D.Double(x - 1, y - 0.5, size + 2, size + 2));

        Point2D headCenter = new Point2D.Float(centerX - size * 0.14f, centerY - size * 0.14f);
        RadialGradientPaint headMetal = new RadialGradientPaint(
                headCenter,
                size * 0.8f,
                new float[]{0.0f, 0.6f, 1.0f},
                new Color[]{
                        new Color(62, 66, 74),
                        new Color(40, 43, 49),
                        new Color(24, 26, 30)
                }
        );
        Ellipse2D.Double screwHead = new Ellipse2D.Double(x, y, size, size);
        g2.setPaint(headMetal);
        g2.fill(screwHead);
        g2.setPaint(getNoiseTexture());
        g2.fill(screwHead);

        g2.setStroke(new BasicStroke(0.8f));
        g2.setColor(new Color(95, 98, 105, 150));
        g2.draw(new Ellipse2D.Double(x + 0.5, y + 0.5, size - 1, size - 1));

        g2.setColor(new Color(12, 13, 15));
        g2.draw(new Ellipse2D.Double(x, y, size, size));

        g2.setColor(new Color(130, 135, 145, 65));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new Arc2D.Double(x + 1.3, y + 1.3, size - 2.6, size - 2.6, 110, 70, Arc2D.OPEN));

        double cx = centerX, cy = centerY;
        double armLen = size * 0.27;
        double armHalfW = 0.7;

        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2.setColor(new Color(10, 11, 13, 195));
        g2.draw(new Line2D.Double(cx - armLen, cy + armHalfW, cx + armLen, cy + armHalfW));
        g2.draw(new Line2D.Double(cx + armHalfW, cy - armLen, cx + armHalfW, cy + armLen));

        g2.setColor(new Color(75, 78, 86, 95));
        g2.draw(new Line2D.Double(cx - armLen, cy - armHalfW, cx + armLen, cy - armHalfW));
        g2.draw(new Line2D.Double(cx - armHalfW, cy - armLen, cx - armHalfW, cy + armLen));

        g2.setColor(new Color(6, 6, 8, 160));
        g2.fill(new Ellipse2D.Double(cx - 1.0, cy - 1.0, 2.0, 2.0));
    }

    private void drawSheen(Graphics2D g, float x, float y, float w, float h, int alpha, Color c) {
        float rx = w * 0.6f, ry = h * 0.7f;
        AffineTransform t = new AffineTransform();
        t.translate(x + w * 0.35f, y - 1f);
        t.scale(rx, ry);
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(0, 0), 1f, new Point2D.Float(0, 0),
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha),
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 3),
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB, t));
        g.fill(new Rectangle2D.Float(x, y, w, h));
    }

    private void drawAnnunciator(Graphics2D g2, int x, int y, String label, boolean isLit) {
        final int width = 44, height = 15, cr = 4;

        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        float cx = x + width / 2f, cy = y + height / 2f;
        RoundRectangle2D.Float socket = new RoundRectangle2D.Float(x - 1, y - 1, width + 2, height + 2, cr + 2, cr + 2);
        RoundRectangle2D.Float lens = new RoundRectangle2D.Float(x, y, width, height, cr, cr);

        if (isLit) {
            float rx = width * 0.8f, ry = height * 1.9f;
            AffineTransform t = new AffineTransform();
            t.translate(cx, cy);
            t.scale(rx, ry);
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(0, 0), 1f, new Point2D.Float(0, 0),
                    new float[]{0f, 0.3f, 0.55f, 0.8f, 1f},
                    new Color[]{new Color(255, 150, 20, 100), new Color(255, 140, 10, 70),
                            new Color(255, 130, 0, 32), new Color(255, 120, 0, 10),
                            new Color(255, 120, 0, 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE,
                    MultipleGradientPaint.ColorSpaceType.SRGB, t));
            g.fill(new Ellipse2D.Float(cx - rx, cy - ry, rx * 2, ry * 2));
        } else {
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new RoundRectangle2D.Float(x, y + 1.5f, width, height, cr + 2, cr + 2));
        }

        g.setColor(new Color(12, 13, 16));
        g.fill(socket);
        g.setStroke(new BasicStroke(1f));
        g.setPaint(new GradientPaint(x - 1, y - 1, new Color(85, 90, 100),
                x + width, y + height, new Color(25, 27, 32)));
        g.draw(socket);
        if (isLit) {
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(255, 150, 30, 110));
            g.draw(socket);
        }

        Shape oldClip = g.getClip();
        g.clip(lens);

        if (isLit) {
            g.setPaint(new GradientPaint(x, y, new Color(255, 198, 62),
                    x, y + height, new Color(255, 142, 6)));
            g.fill(lens);

            AffineTransform t = new AffineTransform();
            t.translate(cx, cy - 0.5f);
            t.scale(width * 0.5f, height * 0.9f);
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(0, 0), 1f, new Point2D.Float(0, 0),
                    new float[]{0f, 0.45f, 1f},
                    new Color[]{new Color(255, 245, 185, 235), new Color(255, 215, 105, 110),
                            new Color(255, 190, 50, 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE,
                    MultipleGradientPaint.ColorSpaceType.SRGB, t));
            g.fill(lens);

            g.setStroke(new BasicStroke(2.4f));
            g.setColor(new Color(200, 90, 0, 55));
            g.draw(lens);

            drawSheen(g, x, y, width, height, 28, new Color(255, 250, 225));
        } else {
            g.setPaint(new GradientPaint(x, y, new Color(74, 50, 14),
                    x + width * 0.4f, y + height, new Color(38, 26, 8)));
            g.fill(lens);

            g.setColor(new Color(255, 170, 60, 16));
            g.fill(new Ellipse2D.Float(cx - 7f, cy - 3f, 14f, 6f));

            g.setStroke(new BasicStroke(1.2f));
            g.setColor(new Color(0, 0, 0, 110));
            g.draw(new Line2D.Float(x + cr, y + 0.6f, x + width - cr, y + 0.6f));
            g.setColor(new Color(255, 200, 110, 28));
            g.draw(new Line2D.Float(x + cr, y + height - 0.6f, x + width - cr, y + height - 0.6f));

            drawSheen(g, x, y, width, height, 80, new Color(255, 240, 205));

            g.setStroke(new BasicStroke(0.9f));
            g.setPaint(new LinearGradientPaint(
                    new Point2D.Float(x, 0), new Point2D.Float(x + width, 0),
                    new float[]{0f, 0.2f, 0.55f, 1f},
                    new Color[]{new Color(255, 235, 190, 0), new Color(255, 235, 190, 75),
                            new Color(255, 235, 190, 35), new Color(255, 235, 190, 0)}));
            g.draw(new Line2D.Float(x + 2, y + 1.4f, x + width - 2, y + 1.4f));
        }
        g.setClip(oldClip);

        Font font = new Font("Arial", Font.BOLD, 10);
        Map<TextAttribute, Object> attrs = new HashMap<>(font.getAttributes());
        attrs.put(TextAttribute.TRACKING, 0.06);
        font = font.deriveFont(attrs);

        GlyphVector gv = font.createGlyphVector(g.getFontRenderContext(), label);
        Rectangle2D vb = gv.getVisualBounds();
        float tx = (float) (x + (width - vb.getWidth()) / 2 - vb.getX());
        float ty = (float) (y + (height - vb.getHeight()) / 2 - vb.getY());

        g.clip(lens);
        if (isLit) {
            Shape outline = gv.getOutline(tx, ty);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 235, 150, 70));
            g.draw(outline);
            g.setColor(new Color(105, 48, 0, 240));
            g.fill(outline);
        } else {
            g.setColor(new Color(255, 190, 90, 30));
            g.drawGlyphVector(gv, tx, ty + 1f);
            g.setColor(new Color(22, 14, 4, 215));
            g.drawGlyphVector(gv, tx, ty);
        }
        g.setClip(oldClip);

        g.setStroke(new BasicStroke(0.8f));
        g.setPaint(isLit ? new Color(150, 70, 0, 110)
                : new GradientPaint(x, y, new Color(90, 70, 30, 140),
                x + width, y + height, new Color(5, 4, 2, 170)));
        g.draw(lens);

        g.dispose();
    }

    private static Color mixColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private void drawRoundLed(Graphics2D g2, int x, int y, int size, Color coreColor, Color highlightColor, boolean isLit) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        final float cx = x + size / 2.0f;
        final float cy = y + size / 2.0f;
        final float r = size / 2.0f;

        Ellipse2D.Float lens = new Ellipse2D.Float(x, y, size, size);
        Ellipse2D.Float socket = new Ellipse2D.Float(x - 1.2f, y - 1.2f, size + 2.4f, size + 2.4f);

        if (!isLit) {
            g.setColor(new Color(0, 0, 0, 80));
            g.fill(new Ellipse2D.Float(x - 1f, y + 0.4f, size + 2f, size + 2f));
        }

        if (!isLit) {
            g.setColor(new Color(12, 13, 16));
            g.fill(socket);
            g.setStroke(new BasicStroke(1f));
            g.setPaint(new GradientPaint(x - 1, y - 1, new Color(100, 105, 115),
                    x + size + 1, y + size + 1, new Color(22, 24, 29)));
            g.draw(socket);
        }

        Shape oldClip = g.getClip();
        g.clip(lens);

        if (isLit) {
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(cx, cy - size * 0.04f), r * 1.08f,
                    new float[]{0f, 0.35f, 0.75f, 1f},
                    new Color[]{mixColor(coreColor, Color.WHITE, 0.72f),
                            mixColor(coreColor, Color.WHITE, 0.28f),
                            coreColor,
                            mixColor(coreColor, Color.BLACK, 0.18f)}));
            g.fill(lens);

            AffineTransform ts = new AffineTransform();
            ts.translate(cx, y + size * 0.2f);
            ts.scale(size * 0.42f, size * 0.26f);
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(0, 0), 1f, new Point2D.Float(0, 0),
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{withAlpha(highlightColor, 110), withAlpha(highlightColor, 35),
                            withAlpha(highlightColor, 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE,
                    MultipleGradientPaint.ColorSpaceType.SRGB, ts));
            g.fill(lens);

            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(x + size * 0.34f, y + size * 0.30f), Math.max(1f, size * 0.2f),
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{withAlpha(highlightColor, 190), withAlpha(highlightColor, 60),
                            withAlpha(highlightColor, 0)}));
            g.fill(lens);

        } else {
            Color body = mixColor(coreColor, Color.BLACK, 0.70f);
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(x + size * 0.38f, y + size * 0.32f), r * 1.35f,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{mixColor(coreColor, Color.BLACK, 0.52f), body,
                            mixColor(coreColor, Color.BLACK, 0.88f)}));
            g.fill(lens);

            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(cx, cy + size * 0.04f), Math.max(1f, size * 0.26f),
                    new float[]{0f, 1f},
                    new Color[]{withAlpha(mixColor(coreColor, Color.BLACK, 0.2f), 55),
                            withAlpha(mixColor(coreColor, Color.BLACK, 0.2f), 0)}));
            g.fill(lens);

            g.setPaint(new GradientPaint(x, y + size * 0.6f,
                    withAlpha(mixColor(coreColor, Color.WHITE, 0.2f), 0),
                    x, y + size, withAlpha(mixColor(coreColor, Color.WHITE, 0.2f), 42)));
            g.fill(new Rectangle2D.Float(x, y + size * 0.6f, size, size * 0.4f));

            AffineTransform ts = new AffineTransform();
            ts.translate(cx - size * 0.05f, y + size * 0.22f);
            ts.scale(size * 0.45f, size * 0.28f);
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(0, 0), 1f, new Point2D.Float(0, 0),
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{withAlpha(highlightColor, 120), withAlpha(highlightColor, 40),
                            withAlpha(highlightColor, 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE,
                    MultipleGradientPaint.ColorSpaceType.SRGB, ts));
            g.fill(lens);

            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(x + size * 0.32f, y + size * 0.28f), Math.max(1f, size * 0.16f),
                    new float[]{0f, 1f},
                    new Color[]{withAlpha(highlightColor, 105), withAlpha(highlightColor, 0)}));
            g.fill(lens);

            g.setStroke(new BasicStroke(Math.max(1.2f, size * 0.12f)));
            g.setColor(new Color(0, 0, 0, 90));
            g.draw(lens);
        }
        g.setClip(oldClip);

        if (!isLit) {
            g.setStroke(new BasicStroke(0.7f));
            g.setPaint(new GradientPaint(x, y, new Color(70, 70, 75, 130),
                    x + size, y + size, new Color(0, 0, 0, 170)));
            g.draw(lens);
        }

        if (isLit) {
            float glowRadius = size * 1.5f;
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(cx, cy), glowRadius,
                    new float[]{0.00f, 0.30f, 0.50f, 0.72f, 1.00f},
                    new Color[]{withAlpha(coreColor, 95), withAlpha(coreColor, 70),
                            withAlpha(coreColor, 38), withAlpha(coreColor, 14),
                            withAlpha(coreColor, 0)}));
            g.fill(new Ellipse2D.Float(cx - glowRadius, cy - glowRadius,
                    glowRadius * 2f, glowRadius * 2f));

            float tight = size * 0.85f;
            g.setPaint(new RadialGradientPaint(
                    new Point2D.Float(cx, cy), tight,
                    new float[]{0f, 0.6f, 1f},
                    new Color[]{withAlpha(mixColor(coreColor, Color.WHITE, 0.35f), 60),
                            withAlpha(mixColor(coreColor, Color.WHITE, 0.2f), 40),
                            withAlpha(coreColor, 0)}));
            g.fill(new Ellipse2D.Float(cx - tight, cy - tight, tight * 2f, tight * 2f));
        }

        g.dispose();
    }
}