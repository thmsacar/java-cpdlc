package gui2.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Interactive hardware screw button placed in the top-left corner of the CDU bezel.
 * Supports CLOSE (crimson metallic, 45-degree X cross slot) and MINIMIZE (smaller yellow metallic, single horizontal line slot).
 */
public class CduScrewButton extends JButton {

    public enum Mode {
        CLOSE,
        MINIMIZE
    }

    private final Mode mode;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public CduScrewButton() {
        this(Mode.CLOSE);
    }

    public CduScrewButton(Mode mode) {
        super();
        this.mode = mode != null ? mode : Mode.CLOSE;
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(this.mode == Mode.CLOSE ? "Close JavaCPDLC" : "Minimize JavaCPDLC");
        setPreferredSize(this.mode == Mode.CLOSE ? new Dimension(22, 22) : new Dimension(18, 18));

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
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (mode == Mode.MINIMIZE) {
            int size = 14;
            int off = isPressed ? 1 : 0;
            int x = 2 + off;
            int y = 2 + off;

            float centerX = x + size / 2.0f;
            float centerY = y + size / 2.0f;

            // 1. Recessed countersink shadow
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fill(new Ellipse2D.Double(x - 1, y - 0.5, size + 2, size + 2));

            // 2. Screw head base metal (rich bright yellow/brass metallic finish)
            Point2D headCenter = new Point2D.Float(centerX - size * 0.12f, centerY - size * 0.12f);
            Color metalTop = isHovered ? new Color(255, 220, 70) : new Color(225, 180, 45);
            Color metalMid = isHovered ? new Color(215, 165, 35) : new Color(180, 135, 25);
            Color metalBot = isHovered ? new Color(155, 115, 18) : new Color(125, 90, 12);

            RadialGradientPaint headMetal = new RadialGradientPaint(
                    headCenter,
                    size * 0.8f,
                    new float[]{0.0f, 0.6f, 1.0f},
                    new Color[]{metalTop, metalMid, metalBot}
            );
            Ellipse2D.Double screwHead = new Ellipse2D.Double(x, y, size, size);
            g2.setPaint(headMetal);
            g2.fill(screwHead);

            // 3. Machined rim ring with bright yellow accent
            g2.setStroke(new BasicStroke(0.9f));
            g2.setColor(isHovered ? new Color(255, 235, 120, 240) : new Color(240, 195, 60, 200));
            g2.draw(new Ellipse2D.Double(x + 0.5, y + 0.5, size - 1, size - 1));

            // 4. Outer contact edge
            g2.setColor(new Color(20, 15, 5));
            g2.draw(new Ellipse2D.Double(x, y, size, size));

            // 5. Top-left highlight sliver
            g2.setColor(isHovered ? new Color(255, 245, 180, 160) : new Color(255, 220, 120, 100));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new Arc2D.Double(x + 1.3, y + 1.3, size - 2.6, size - 2.6, 110, 70, Arc2D.OPEN));

            // 6. Single horizontal slot line (-) for minimize
            double cx = centerX, cy = centerY;
            double armLen = size * 0.32;

            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(25, 18, 5, 230));
            g2.draw(new Line2D.Double(cx - armLen, cy + 0.7, cx + armLen, cy + 0.7));

            Color slotHighlight = isHovered ? new Color(60, 45, 10) : new Color(40, 30, 5);
            g2.setColor(slotHighlight);
            g2.draw(new Line2D.Double(cx - armLen, cy - 0.7, cx + armLen, cy - 0.7));

            g2.dispose();
            return;
        }

        // Mode.CLOSE: 18px size, dark crimson metallic finish, 45-degree turned X slot
        int size = 18;
        int off = isPressed ? 1 : 0;
        int x = 2 + off;
        int y = 2 + off;

        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;

        // 1. Recessed countersink shadow
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fill(new Ellipse2D.Double(x - 1, y - 0.5, size + 2, size + 2));

        // 2. Screw head base metal
        Point2D headCenter = new Point2D.Float(centerX - size * 0.12f, centerY - size * 0.12f);
        Color metalTop = isHovered ? new Color(110, 60, 65) : new Color(82, 46, 50);
        Color metalMid = isHovered ? new Color(80, 38, 42) : new Color(58, 29, 32);
        Color metalBot = isHovered ? new Color(55, 24, 27) : new Color(38, 18, 20);

        RadialGradientPaint headMetal = new RadialGradientPaint(
                headCenter,
                size * 0.8f,
                new float[]{0.0f, 0.6f, 1.0f},
                new Color[]{metalTop, metalMid, metalBot}
        );
        Ellipse2D.Double screwHead = new Ellipse2D.Double(x, y, size, size);
        g2.setPaint(headMetal);
        g2.fill(screwHead);

        // 3. Machined rim ring
        g2.setStroke(new BasicStroke(0.9f));
        g2.setColor(isHovered ? new Color(220, 90, 95, 230) : new Color(175, 75, 80, 180));
        g2.draw(new Ellipse2D.Double(x + 0.5, y + 0.5, size - 1, size - 1));

        // 4. Outer contact edge
        g2.setColor(new Color(15, 10, 12));
        g2.draw(new Ellipse2D.Double(x, y, size, size));

        // 5. Top-left highlight sliver
        g2.setColor(isHovered ? new Color(255, 180, 185, 120) : new Color(200, 140, 145, 70));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new Arc2D.Double(x + 1.3, y + 1.3, size - 2.6, size - 2.6, 110, 70, Arc2D.OPEN));

        // 6. 45-degree turned Phillips slot forming a distinct 'X' close cross
        double cx = centerX, cy = centerY;
        double armLen = size * 0.28;
        double armHalfW = 0.7;

        Graphics2D sg = (Graphics2D) g2.create();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        sg.rotate(Math.toRadians(45), cx, cy);

        sg.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        sg.setColor(new Color(12, 8, 10, 210));
        sg.draw(new Line2D.Double(cx - armLen, cy + armHalfW, cx + armLen, cy + armHalfW));
        sg.draw(new Line2D.Double(cx + armHalfW, cy - armLen, cx + armHalfW, cy + armLen));

        Color slotHighlight = isHovered ? new Color(255, 120, 120) : new Color(210, 75, 75);
        sg.setColor(slotHighlight);
        sg.draw(new Line2D.Double(cx - armLen, cy - armHalfW, cx + armLen, cy - armHalfW));
        sg.draw(new Line2D.Double(cx - armHalfW, cy - armLen, cx - armHalfW, cy + armLen));

        // 7. Center pit
        sg.setColor(isHovered ? new Color(255, 140, 140) : new Color(180, 50, 50));
        sg.fill(new Ellipse2D.Double(cx - 1.2, cy - 1.2, 2.4, 2.4));

        sg.dispose();
        g2.dispose();
    }
}
