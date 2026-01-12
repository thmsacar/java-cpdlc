package gui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PilotButton extends JButton {
    private Color foregroundColor;
    private Color originalColor;

    public PilotButton(String text) {
        super(text);

        this.originalColor = UIManager.getColor("Button.background");
        this.foregroundColor = UIManager.getColor("Button.foreground");


        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(true);
        setOpaque(true);

        setBackground(originalColor);
        setForeground(Color.WHITE);
        setFont(new Font("Monospaced", Font.BOLD, 14));


        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

        //Blue on click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                originalColor = getBackground();
                foregroundColor = getForeground();
                setBackground(new Color(0, 120, 215));
                setForeground(Color.WHITE);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setBackground(originalColor);
                setForeground(foregroundColor);
            }
        });
    }

    public void setCustomColor(Color bg, Color fg) {
        setBackground(bg);
        setForeground(fg);
    }



    //Remove the gradient
    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}