package gui;

import javax.swing.*;
import java.awt.*;

public class SquareIcon implements Icon {
    private final boolean selected;
    public SquareIcon(boolean selected) { this.selected = selected; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dış Kare (Border)
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(x, y, 14, 14);

        // İç Dolgu (Seçiliyse PilotButton Yeşili veya senin istediğin renk)
        if (selected) {
            g2.setColor(new Color(60, 120, 60));
            g2.fillRect(x + 3, y + 3, 9, 9);
        }

        g2.dispose();
    }

    @Override public int getIconWidth() { return 18; }
    @Override public int getIconHeight() { return 18; }
}
