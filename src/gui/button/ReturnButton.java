package gui.button;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ReturnButton extends PilotButton{


    public ReturnButton() {
        super("← RETURN");
        this.addColorChangerOnPress();
        this.setFont(new Font("Monospaced", Font.BOLD, 12));
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        );
        this.setBorder(border);
    }
}
