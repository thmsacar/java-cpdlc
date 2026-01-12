package gui;

import javax.sound.sampled.*;

import java.net.URL;
public class SoundManager {
    public static void playNotification() {
        try {
            // Use class loader for Java 8
            URL soundFile = SoundManager.class.getResource("/resources/sounds/msg_alert.wav");

            if (soundFile == null) {
                System.err.println("Sound file couldnt find! Check the path Class.SoundManager");
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
