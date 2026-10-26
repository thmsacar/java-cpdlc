package gui2.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * Line Select Key (LSK) hardware button for the CDU panel.
 * Modeled from a natural near-top-down cockpit viewing angle (~80 deg):
 * subtle 4-sided 3D bevels, slightly thicker lower skirt, and short-travel
 * tactile movement that stays proud of the panel when pressed.
 */
public class CduButton extends JButton {

    private boolean isHovered = false;
    private boolean isPressed = false;
    private final boolean isLeft;
    private static TexturePaint polymerTexture;

    public CduButton(String name, boolean isLeft) {
        super();
        this.isLeft = isLeft;
        setName(name);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(38, 22));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    /**
     * Subtle matte elastomer/polymer micro-grain for the keycap surface.
     */
    private static TexturePaint getPolymerTexture() {
        if (polymerTexture == null) {
            final int size = 32;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            java.util.Random rnd = new java.util.Random(90737);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int alpha = 4 + rnd.nextInt(7);
                    int v = rnd.nextBoolean() ? 255 : 0;
                    img.setRGB(x, y, (alpha << 24) | (v << 16) | (v << 8) | v);
                }
            }
            polymerTexture = new TexturePaint(img, new Rectangle(0, 0, size, size));
        }
        return polymerTexture;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();
        int h = getHeight();

        // Subtle, short-travel tactile displacement (0.8px) so the key stays elevated when pressed
        float pressY = isPressed ? 0.8f : 0.0f;

        // Near-bird's-eye 3D geometry:
        // Top bevel = 1.1px, Left/Right bevel = 1.3px, Bottom 3D skirt = 2.3px (1.5px when pressed)
        float kx = 1.8f;
        float kw = w - 3.6f;
        float ky = 1.5f + pressY;
        float skirtBottom = h - 1.8f; // 20.2f
        float kh = skirtBottom - ky;
        float kr = 4.4f;

        float fx = kx + 1.3f;
        float fw = kw - 2.6f;
        float fy = ky + 1.1f;
        float fh = 15.3f; // Constant crown height: leaves 2.3px bottom skirt unpressed, 1.5px pressed
        float fr = 3.0f;

        // ====================================================================
        // 1. TIGHT SOCKET COLLAR & SUBTLE DROP SHADOW
        // ====================================================================
        // Subtle shadow just beneath the bottom edge (not exaggerated)
        float shadowBottom = isPressed ? (h - 0.8f) : (h - 0.3f);
        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, skirtBottom - 1.0f),
                new Point2D.Float(0, shadowBottom),
                new float[]{0.0f, 1.0f},
                new Color[]{
                        new Color(0, 0, 0, isPressed ? 130 : 160),
                        new Color(0, 0, 0, 0)
                }
        ));
        g2.fill(new RoundRectangle2D.Float(kx - 0.2f, skirtBottom - 1.0f, kw + 0.4f, shadowBottom - (skirtBottom - 1.0f), 5.0f, 5.0f));

        // Recessed socket gap around the keycap perimeter
        RoundRectangle2D.Float socketCavity = new RoundRectangle2D.Float(1.0f, 1.0f, w - 2.0f, h - 2.2f, 5.2f, 5.2f);
        g2.setColor(new Color(9, 10, 13, 225));
        g2.fill(socketCavity);
        g2.setStroke(new BasicStroke(0.8f));
        g2.setColor(new Color(16, 18, 23, 180));
        g2.draw(socketCavity);

        // ====================================================================
        // 2. 3D TAPERED KEYCAP SKIRT
        // ====================================================================
        RoundRectangle2D.Float keySkirt = new RoundRectangle2D.Float(kx, ky, kw, kh, kr, kr);

        float knuckleRatio = Math.min(0.88f, Math.max(0.5f, (fy + fh - ky) / kh));
        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, ky),
                new Point2D.Float(0, skirtBottom),
                new float[]{0.0f, 0.10f, knuckleRatio, 1.0f},
                new Color[]{
                        isHovered ? new Color(110, 118, 132) : new Color(96, 103, 116), // Lit top chamfer
                        isHovered ? new Color(62, 67, 77) : new Color(53, 58, 67),      // Side chamfers
                        isHovered ? new Color(36, 40, 47) : new Color(30, 34, 40),      // Lower knuckle
                        new Color(15, 17, 21)                                           // Bottom skirt base
                }
        ));
        g2.fill(keySkirt);

        // Soft left & right chamfer shading so all 4 sides read as 3D from above
        Shape oldClip = g2.getClip();
        g2.clip(keySkirt);

        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(kx, 0), new Point2D.Float(kx + 2.0f, 0),
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(120, 130, 145, 55), new Color(120, 130, 145, 0)}
        ));
        g2.fill(new Rectangle2D.Float(kx, ky, 2.0f, kh));

        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(kx + kw - 2.0f, 0), new Point2D.Float(kx + kw, 0),
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(20, 22, 27, 0), new Color(20, 22, 27, 65)}
        ));
        g2.fill(new Rectangle2D.Float(kx + kw - 2.0f, ky, 2.0f, kh));

        // Faint corner bevel lines at the bottom corners
        g2.setStroke(new BasicStroke(0.8f));
        g2.setColor(new Color(65, 72, 84, 55));
        g2.draw(new Line2D.Float(fx + 0.5f, fy + fh - 0.4f, kx + 0.9f, skirtBottom - 0.6f));
        g2.draw(new Line2D.Float(fx + fw - 0.5f, fy + fh - 0.4f, kx + kw - 0.9f, skirtBottom - 0.6f));

        // ====================================================================
        // 3. TOP CROWN FACE (Üst basma yüzeyi)
        // ====================================================================
        RoundRectangle2D.Float keyFace = new RoundRectangle2D.Float(fx, fy, fw, fh, fr, fr);

        Color faceTop = isHovered ? new Color(60, 66, 76) : new Color(51, 56, 65);
        Color faceMid = isHovered ? new Color(50, 55, 64) : new Color(43, 47, 55);
        Color faceBot = isHovered ? new Color(43, 47, 55) : new Color(37, 41, 48);

        if (isPressed) {
            faceTop = new Color(48, 53, 61);
            faceMid = new Color(41, 45, 52);
            faceBot = new Color(35, 39, 46);
        }

        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, fy), new Point2D.Float(0, fy + fh),
                new float[]{0.0f, 0.5f, 1.0f},
                new Color[]{faceTop, faceMid, faceBot}
        ));
        g2.fill(keyFace);

        // Subtle overhead light sheen on the top face
        g2.setPaint(new RadialGradientPaint(
                new Point2D.Float(fx + fw * 0.5f, fy + fh * 0.25f),
                fw * 0.65f,
                new float[]{0.0f, 1.0f},
                new Color[]{
                        new Color(210, 224, 242, isHovered ? 24 : 15),
                        new Color(210, 224, 242, 0)
                }
        ));
        g2.fill(keyFace);

        // Matte polymer micro-grain across the keycap
        g2.setPaint(getPolymerTexture());
        g2.fill(keySkirt);
        g2.setClip(oldClip);

        // ====================================================================
        // 4. SUBTLE 3D CHAMFER HIGHLIGHTS
        // ====================================================================
        // Top chamfer catch-light
        int topRidgeAlpha = isHovered ? 145 : (isPressed ? 105 : 125);
        g2.setStroke(new BasicStroke(0.9f));
        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(fx, 0), new Point2D.Float(fx + fw, 0),
                new float[]{0.0f, 0.16f, 0.5f, 0.84f, 1.0f},
                new Color[]{
                        new Color(185, 195, 210, 0),
                        new Color(185, 195, 210, topRidgeAlpha),
                        new Color(205, 215, 230, topRidgeAlpha + 20),
                        new Color(185, 195, 210, topRidgeAlpha),
                        new Color(185, 195, 210, 0)
                }
        ));
        g2.draw(new Line2D.Float(fx + 1.0f, ky + 0.8f, fx + fw - 1.0f, ky + 0.8f));

        // Soft bottom edge transition where the crown meets the short lower skirt
        float knuckleY = fy + fh;
        g2.setStroke(new BasicStroke(0.8f));
        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(fx, 0), new Point2D.Float(fx + fw, 0),
                new float[]{0.0f, 0.18f, 0.5f, 0.82f, 1.0f},
                new Color[]{
                        new Color(85, 94, 108, 0),
                        new Color(85, 94, 108, 65),
                        new Color(100, 110, 125, 80),
                        new Color(85, 94, 108, 65),
                        new Color(85, 94, 108, 0)
                }
        ));
        g2.draw(new Line2D.Float(fx + 1.2f, knuckleY - 0.2f, fx + fw - 1.2f, knuckleY - 0.2f));

        g2.setColor(new Color(14, 16, 20, 135));
        g2.draw(new Line2D.Float(fx + 1.2f, knuckleY + 0.5f, fx + fw - 1.2f, knuckleY + 0.5f));

        // Crisp outer contour
        g2.setStroke(new BasicStroke(0.8f));
        g2.setColor(new Color(9, 11, 14, 210));
        g2.draw(keySkirt);

        // ====================================================================
        // 5. INLAID BACKLIT INDICATOR BAR (Merkeze hizalı gömme akrilik bar)
        // ====================================================================
        float barW = 13.0f;
        float barH = 2.4f;
        float barX = (w - barW) / 2.0f;
        float barY = fy + (fh - barH) / 2.0f;

        RoundRectangle2D.Float barGroove = new RoundRectangle2D.Float(
                barX - 0.6f, barY - 0.6f, barW + 1.2f, barH + 1.2f, 2.2f, 2.2f);
        RoundRectangle2D.Float barInlay = new RoundRectangle2D.Float(
                barX, barY, barW, barH, 1.6f, 1.6f);

        if (isHovered) {
            float cx = barX + barW / 2.0f;
            float cy = barY + barH / 2.0f;
            AffineTransform glowTx = new AffineTransform();
            glowTx.translate(cx, cy);
            glowTx.scale(barW * 0.75f, barH * 2.2f);
            g2.setPaint(new RadialGradientPaint(
                    new Point2D.Float(0, 0), 1.0f, new Point2D.Float(0, 0),
                    new float[]{0.0f, 0.45f, 1.0f},
                    new Color[]{
                            new Color(220, 238, 255, 70),
                            new Color(190, 220, 255, 25),
                            new Color(190, 220, 255, 0)
                    },
                    MultipleGradientPaint.CycleMethod.NO_CYCLE,
                    MultipleGradientPaint.ColorSpaceType.SRGB, glowTx
            ));
            g2.fill(new Ellipse2D.Float(cx - barW, cy - barH * 3, barW * 2, barH * 6));
        }

        // Recessed micro-seam around the white acrylic bar
        g2.setColor(new Color(14, 16, 20, 210));
        g2.fill(barGroove);

        Color barTop = isHovered ? new Color(250, 253, 255) : new Color(230, 235, 242);
        Color barBottom = isHovered ? new Color(215, 226, 240) : new Color(184, 190, 200);

        g2.setPaint(new LinearGradientPaint(
                new Point2D.Float(0, barY), new Point2D.Float(0, barY + barH),
                new float[]{0.0f, 1.0f},
                new Color[]{barTop, barBottom}
        ));
        g2.fill(barInlay);

        g2.dispose();
    }
}