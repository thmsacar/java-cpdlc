package gui2;

import gui2.components.CduBezel;
import gui2.components.CduButton;
import gui2.components.CduDisplay;
import gui2.controller.CduController;
import gui2.pages.CduLoginPage;
import service.CpdlcService;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;

/**
 * Full CDU Cockpit Unit with pixel-perfect alignment between LSK buttons and display text lines.
 */
public class CduPanel extends CduBezel {

    private final CduDisplay display;
    private final CduController controller;

    public CduPanel() {
        this.display = new CduDisplay();
        this.controller = new CduController(display);
        this.controller.setMsgLedConsumer(this::setMsgLed);
        this.controller.setExecLedConsumer(this::setExecLed);
        this.controller.setFailLedConsumer(this::setFailLed);
        setupUI();
        setupGlobalKeyboardListener();
        initController();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(28, 16, 24, 16));

        // Screen bezel container
        JPanel screenContainer = new JPanel(new BorderLayout());
        screenContainer.setOpaque(false);
        screenContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(25, 27, 30), 4),
                BorderFactory.createLineBorder(new Color(10, 10, 12), 2)
        ));
        screenContainer.add(display, BorderLayout.CENTER);
        add(screenContainer, BorderLayout.CENTER);

        // Left LSK Panel (Absolute positioning matching display row Y center)
        JPanel leftPanel = new JPanel(null) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(40, 300);
            }
        };
        leftPanel.setOpaque(false);

        for (int i = 0; i < 6; i++) {
            CduButton btn = new CduButton("LSK_" + (i + 1) + "L", true);
            int lskIndex = i;
            btn.addActionListener(e -> controller.onLskPressed(lskIndex, true));
            int btnY = 45 + (i * 40);
            btn.setBounds(2, btnY, 36, 22);
            leftPanel.add(btn);
        }
        add(leftPanel, BorderLayout.WEST);

        // Right LSK Panel (Absolute positioning matching display row Y center)
        JPanel rightPanel = new JPanel(null) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(40, 300);
            }
        };
        rightPanel.setOpaque(false);

        for (int i = 0; i < 6; i++) {
            CduButton btn = new CduButton("LSK_" + (i + 1) + "R", false);
            int lskIndex = i;
            btn.addActionListener(e -> controller.onLskPressed(lskIndex, false));
            int btnY = 45 + (i * 40);
            btn.setBounds(2, btnY, 36, 22);
            rightPanel.add(btn);
        }
        add(rightPanel, BorderLayout.EAST);
    }

    private void setupGlobalKeyboardListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            Window window = SwingUtilities.getWindowAncestor(CduPanel.this);
            if (window == null || !window.isFocused()) {
                return false;
            }

            if (e.getID() == KeyEvent.KEY_PRESSED) {
                boolean isShortcut = e.isControlDown() || e.isMetaDown();

                // Ctrl+V or Cmd+V: Paste from clipboard safely
                if (isShortcut && e.getKeyCode() == KeyEvent.VK_V) {
                    try {
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        Transferable contents = clipboard.getContents(null);
                        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                            String pasteText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                            if (pasteText != null && !pasteText.isEmpty()) {
                                String cleanText = pasteText.trim().replace("\n", "").replace("\r", "");
                                controller.handlePaste(cleanText);
                            }
                        }
                    } catch (Throwable t) {
                        System.err.println("Clipboard paste error: " + t.getMessage());
                    }
                    return true;
                }

                // Ctrl+C or Cmd+C: Copy scratchpad to clipboard
                if (isShortcut && e.getKeyCode() == KeyEvent.VK_C) {
                    try {
                        String scratchpad = controller.getScratchpad();
                        if (scratchpad != null && !scratchpad.isEmpty()) {
                            StringSelection selection = new StringSelection(scratchpad);
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                            controller.setStatusMessage("SCRATCHPAD COPIED");
                        }
                    } catch (Throwable t) {
                        System.err.println("Clipboard copy error: " + t.getMessage());
                    }
                    return true;
                }

                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    controller.handleKeyTyped("MENU");
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    controller.handleKeyTyped("DEL");
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    controller.handleKeyTyped("CLR");
                    return true;
                }
            } else if (e.getID() == KeyEvent.KEY_TYPED) {
                if (e.isControlDown() || e.isMetaDown()) {
                    return false;
                }

                char c = e.getKeyChar();
                if (c == '\b') {
                    controller.handleKeyTyped("CLR");
                    return true;
                } else if (c == 127) {
                    controller.handleKeyTyped("DEL");
                    return true;
                } else if (Character.isLetterOrDigit(c) || c == '/' || c == '.' || c == ' ' || c == '-' || c == '@') {
                    controller.handleKeyTyped(String.valueOf(c));
                    return true;
                }
            }
            return false;
        });
    }

    private void initController() {
        controller.showPage(new CduLoginPage());
    }

    public CduController getController() {
        return controller;
    }

    public CduDisplay getDisplay() {
        return display;
    }

    public void setService(CpdlcService service) {
        controller.setService(service);
        controller.refreshDisplay();
    }
}
