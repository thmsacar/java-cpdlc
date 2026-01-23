package gui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

public class FontManager {
    public static Font REGULAR;
    public static Font BOLD;
    public static Font LIGHT;

    public static void loadFonts() {
        REGULAR = loadFont("/resources/fonts/RobotoMono-Regular.ttf");
        BOLD = loadFont("/resources/fonts/RobotoMono-Bold.ttf");
        LIGHT = loadFont("/resources/fonts/RobotoMono-Light.ttf");
    }

    private static Font loadFont(String path) {
        try {
            InputStream is = FontManager.class.getResourceAsStream(path);
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception e) {
            System.err.println("Font couldn't be loaded: " + path);
            return new Font("Monospaced", Font.PLAIN, 12);
        }
    }
}