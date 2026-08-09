package gui2.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Line Select Key (LSK) hardware button for the CDU panel.
 */
public class CduButton extends JButton {

    private boolean isHovered = false;
    private boolean isPressed = false;
    private final boolean isLeft;

    public CduButton(String name, boolean isLeft) {
        super();
        this.isLeft = isLeft;
        setName(name);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
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

        // 1. Recessed dark panel socket cutout (the hole in metal faceplate)
        g2.setColor(new Color(18, 20, 24));
        g2.fillRoundRect(1, 1, width - 2, height - 2, 6, 6);
        g2.setColor(new Color(10, 12, 14));
        g2.drawRoundRect(1, 1, width - 2, height - 2, 6, 6);

        // 2. Physical 3D keycap displacement when pressed
        int offX = isPressed ? 1 : 0;
        int offY = isPressed ? 1 : 0;
        int kw = width - 4;
        int kh = height - 4;
        int kx = 2 + offX;
        int ky = 2 + offY;

        // Keycap face color
        Color keyFace = isPressed ? new Color(34, 37, 44) : (isHovered ? new Color(64, 68, 76) : new Color(48, 52, 60));
        g2.setColor(keyFace);
        g2.fillRoundRect(kx, ky, kw, kh, 4, 4);

        if (!isPressed) {
            // Top bevel highlight (specular ridge from overhead lighting)
            g2.setColor(new Color(110, 115, 125));
            g2.drawLine(kx + 2, ky + 1, kx + kw - 3, ky + 1);
            g2.setColor(new Color(85, 90, 100));
            g2.drawLine(kx + 1, ky + 2, kx + 1, ky + kh - 3);

            // Bottom edge drop shadow on keycap
            g2.setColor(new Color(20, 22, 26));
            g2.drawLine(kx + 2, ky + kh - 1, kx + kw - 2, ky + kh - 1);
        } else {
            // Depressed inner shadow
            g2.setColor(new Color(15, 17, 20));
            g2.drawRoundRect(kx, ky, kw, kh, 4, 4);
        }

        // Center indicator bar
        g2.setColor(isHovered ? Color.CYAN : (isPressed ? new Color(180, 185, 195) : new Color(220, 225, 230)));
        int dashW = 12;
        int dashH = 3;
        int dashX = (width - dashW) / 2 + offX;
        int dashY = (height - dashH) / 2 + offY;
        g2.fillRect(dashX, dashY, dashW, dashH);

        // Line extending towards screen
        g2.setColor(new Color(90, 95, 105));
        g2.setStroke(new BasicStroke(1.0f));
        if (isLeft) {
            g2.drawLine(width - 2, height / 2 + offY, width + 4, height / 2 + offY);
        } else {
            g2.drawLine(0, height / 2 + offY, -4, height / 2 + offY);
        }

        g2.dispose();
    }
}
