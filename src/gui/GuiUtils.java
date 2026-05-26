package gui;

import javax.swing.*;
import java.awt.*;

import static gui.DashboardPanel.UI_FONT;

public class GuiUtils {

    public static void styleRadioButton(JRadioButton rb) {
        rb.setOpaque(false);
        rb.setFont(new Font(UI_FONT, Font.PLAIN, 13));
        rb.setForeground(Color.LIGHT_GRAY);
        rb.setFocusPainted(false);
        rb.setIcon(new SquareIcon(false));
        rb.setSelectedIcon(new SquareIcon(true));
        rb.setRolloverIcon(new SquareIcon(false));
    }
}
