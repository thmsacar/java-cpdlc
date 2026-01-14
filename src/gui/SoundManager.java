package gui;

import javax.sound.sampled.*;

import java.net.URL;
public class SoundManager {
    public static void playNotification() {
        playSound("msg_notification.wav");
    }

    public static void playWarning() {
        playSound("msg_warning.wav");
    }

    public static void playSound(String fileName) {
        try {
            URL soundFile = SoundManager.class.getResource("/resources/sounds/"+fileName);

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
