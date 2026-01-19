package gui;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.net.URL;

public class Client {

    protected JFrame frame;


    public Client() {

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        UIManager.put("control", new java.awt.Color(45,45,45));
        UIManager.put("info", new java.awt.Color(45,45,45));
        UIManager.put("nimbusBase", new java.awt.Color(18,30,49));
        UIManager.put("nimbusAlertYellow", new java.awt.Color(248,187,0));
        UIManager.put("nimbusDisabledText", new java.awt.Color(128,128,128));
        UIManager.put("nimbusFocus", new java.awt.Color(115,164,209));
        UIManager.put("nimbusGreen", new java.awt.Color(176,179,50));
        UIManager.put("nimbusInfoBlue", new java.awt.Color(66,139,221));
        UIManager.put("nimbusLightBackground", new java.awt.Color(30,30,30));
        UIManager.put("nimbusOrange", new java.awt.Color(191,98,4));
        UIManager.put("nimbusRed", new java.awt.Color(169,46,34));
        UIManager.put("nimbusSelectedText", new java.awt.Color(255,255,255));
        UIManager.put("nimbusSelectionBackground", new java.awt.Color(60,63,65));
        UIManager.put("text", new java.awt.Color(230,230,230));
        UIManager.put("DefaultLookup.setUIDefault", Boolean.TRUE);
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("TextField.focus", new Color(0, 0, 0, 0));
        UIManager.put("Component.focusColor", new Color(0, 0, 0, 0));

        FontManager.loadFonts();

        frame = new JFrame("CPDLC Client");
        setAppIcon(frame);
        frame.setSize(650, 400);
        frame.setMinimumSize(new Dimension(650, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setContentPane(new LoginPanel(this));
        frame.setVisible(true);

    }

    public void setAppIcon(JFrame frame) {
        String iconPath = "/resources/images/icon.png";
        URL iconURL = getClass().getResource(iconPath);

        if (iconURL != null) {
            Image img = new ImageIcon(iconURL).getImage();
            frame.setIconImage(img);

            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                try {
                    Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
                    Method getTaskbar = taskbarClass.getDeclaredMethod("getTaskbar");
                    Object taskbar = getTaskbar.invoke(null);
                    Method setIconImage = taskbarClass.getDeclaredMethod("setIconImage", Image.class);
                    setIconImage.invoke(taskbar, img);
//                    System.out.println("Mac ikon: Taskbar API kullanıldı.");
                } catch (Exception e1) {
                    try {
                        Class<?> appClass = Class.forName("com.apple.eawt.Application");
                        Object application = appClass.getMethod("getApplication").invoke(null);
                        appClass.getMethod("setDockIconImage", Image.class).invoke(application, img);
//                        System.out.println("Mac ikon: Apple EAWT kullanıldı.");
                    } catch (Exception e2) {
//                        System.err.println("Mac ikon set edilemedi.");
                    }
                }
            }
        }
    }

    public void showDashboard(String callsign, String hoppieID) {
        frame.setContentPane(new DashboardPanel(callsign, hoppieID, this));
        frame.revalidate();
        frame.repaint();
    }

    public void showLogin() {
        frame.setContentPane(new LoginPanel(this));
        frame.revalidate();
        frame.repaint();
    }

    public static void main(String[] args) {
        new Client();
    }

}
