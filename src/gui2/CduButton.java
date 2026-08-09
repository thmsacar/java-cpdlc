package gui2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Line Select Key (LSK) r button for the CDU panel.
 */
public class CduButton extends JButton {

    private boolean isHovered = false;
    private boolean isPressed = false;

    public CduButton(String name) {
        super();
        setName(name);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(36, 22));

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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Base metallic button gradient
        Color topColor = isPressed ? new Color(25, 25, 28) : (isHovered ? new Color(65, 68, 75) : new Color(45, 48, 55));
        Color bottomColor = isPressed ? new Color(45, 48, 55) : (isHovered ? new Color(35, 38, 45) : new Color(25, 28, 32));

        GradientPaint bgGradient = new GradientPaint(0, 0, topColor, 0, height, bottomColor);
        g2.setPaint(bgGradient);
        g2.fillRoundRect(2, 2, width - 4, height - 4, 6, 6);

        // Outer border
        g2.setColor(new Color(15, 15, 18));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(2, 2, width - 4, height - 4, 6, 6);

        // Center white indicator line (hardware LSK dash)
        g2.setColor(isHovered ? Color.CYAN : new Color(220, 220, 220));
        int dashW = 12;
        int dashH = 3;
        int dashX = (width - dashW) / 2;
        int dashY = (height - dashH) / 2;
        g2.fillRect(dashX, dashY, dashW, dashH);

        g2.dispose();
    }
}
