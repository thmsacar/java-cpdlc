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
import java.awt.geom.Path2D;

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
        setupWindowDragAndResize();
        setupGlobalKeyboardListener();
        initController();
    }

    private static final int RESIZE_BORDER_WIDTH = 8;
    private int resizeCursorDirection = Cursor.DEFAULT_CURSOR;
    private Point dragStartPoint;
    private Rectangle windowStartBounds;

    private void setupWindowDragAndResize() {
        java.awt.event.MouseAdapter adapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                Window window = SwingUtilities.getWindowAncestor(CduPanel.this);
                if (window == null) return;

                int x = e.getX();
                int y = e.getY();
                int w = getWidth();
                int h = getHeight();

                int cursor = Cursor.DEFAULT_CURSOR;
                if (x <= RESIZE_BORDER_WIDTH && y <= RESIZE_BORDER_WIDTH) {
                    cursor = Cursor.NW_RESIZE_CURSOR;
                } else if (x >= w - RESIZE_BORDER_WIDTH && y <= RESIZE_BORDER_WIDTH) {
                    cursor = Cursor.NE_RESIZE_CURSOR;
                } else if (x <= RESIZE_BORDER_WIDTH && y >= h - RESIZE_BORDER_WIDTH) {
                    cursor = Cursor.SW_RESIZE_CURSOR;
                } else if (x >= w - RESIZE_BORDER_WIDTH && y >= h - RESIZE_BORDER_WIDTH) {
                    cursor = Cursor.SE_RESIZE_CURSOR;
                } else if (x <= RESIZE_BORDER_WIDTH) {
                    cursor = Cursor.W_RESIZE_CURSOR;
                } else if (x >= w - RESIZE_BORDER_WIDTH) {
                    cursor = Cursor.E_RESIZE_CURSOR;
                } else if (y <= RESIZE_BORDER_WIDTH) {
                    cursor = Cursor.N_RESIZE_CURSOR;
                } else if (y >= h - RESIZE_BORDER_WIDTH) {
                    cursor = Cursor.S_RESIZE_CURSOR;
                }

                resizeCursorDirection = cursor;
                if (cursor != Cursor.DEFAULT_CURSOR) {
                    setCursor(Cursor.getPredefinedCursor(cursor));
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                Window window = SwingUtilities.getWindowAncestor(CduPanel.this);
                if (window != null) {
                    dragStartPoint = e.getLocationOnScreen();
                    windowStartBounds = window.getBounds();
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (resizeCursorDirection == Cursor.DEFAULT_CURSOR) {
                    setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                Window window = SwingUtilities.getWindowAncestor(CduPanel.this);
                if (window == null || dragStartPoint == null || windowStartBounds == null) return;

                Point currentPoint = e.getLocationOnScreen();
                int dx = currentPoint.x - dragStartPoint.x;
                int dy = currentPoint.y - dragStartPoint.y;

                Rectangle b = new Rectangle(windowStartBounds);
                Dimension minSize = window.getMinimumSize();
                if (minSize == null || minSize.width <= 0) {
                    minSize = new Dimension(380, 280);
                }

                switch (resizeCursorDirection) {
                    case Cursor.E_RESIZE_CURSOR:
                        b.width = Math.max(minSize.width, b.width + dx);
                        break;
                    case Cursor.S_RESIZE_CURSOR:
                        b.height = Math.max(minSize.height, b.height + dy);
                        break;
                    case Cursor.SE_RESIZE_CURSOR:
                        b.width = Math.max(minSize.width, b.width + dx);
                        b.height = Math.max(minSize.height, b.height + dy);
                        break;
                    case Cursor.W_RESIZE_CURSOR:
                        int newW = b.width - dx;
                        if (newW >= minSize.width) {
                            b.x += dx;
                            b.width = newW;
                        }
                        break;
                    case Cursor.N_RESIZE_CURSOR:
                        int newH = b.height - dy;
                        if (newH >= minSize.height) {
                            b.y += dy;
                            b.height = newH;
                        }
                        break;
                    case Cursor.NW_RESIZE_CURSOR:
                        int newW_nw = b.width - dx;
                        int newH_nw = b.height - dy;
                        if (newW_nw >= minSize.width) {
                            b.x += dx;
                            b.width = newW_nw;
                        }
                        if (newH_nw >= minSize.height) {
                            b.y += dy;
                            b.height = newH_nw;
                        }
                        break;
                    case Cursor.NE_RESIZE_CURSOR:
                        int newW_ne = b.width + dx;
                        int newH_ne = b.height - dy;
                        b.width = Math.max(minSize.width, newW_ne);
                        if (newH_ne >= minSize.height) {
                            b.y += dy;
                            b.height = newH_ne;
                        }
                        break;
                    case Cursor.SW_RESIZE_CURSOR:
                        int newW_sw = b.width - dx;
                        int newH_sw = b.height + dy;
                        if (newW_sw >= minSize.width) {
                            b.x += dx;
                            b.width = newW_sw;
                        }
                        b.height = Math.max(minSize.height, newH_sw);
                        break;
                    default:
                        // Move window
                        b.x += dx;
                        b.y += dy;
                        break;
                }

                window.setBounds(b);
                window.revalidate();
                window.repaint();
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private void setupUI() {
        if (closeScrewButton != null) {
            closeScrewButton.addActionListener(e -> controller.handleWindowClose());
        }
        if (minimizeScrewButton != null) {
            minimizeScrewButton.addActionListener(e -> {
                Window window = SwingUtilities.getWindowAncestor(CduPanel.this);
                if (window instanceof Frame) {
                    ((Frame) window).setState(Frame.ICONIFIED);
                }
            });
        }
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(28, 16, 24, 16));

        // Screen bezel container
        JPanel screenContainer = new JPanel(new BorderLayout());
        screenContainer.setOpaque(false);
        screenContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(75, 80, 90), 4),
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

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();

        // 6 LSK Line Pairs
        int firstButtonCenterY = 84; // 28 (top border) + 45 (btnY) + 11 (half btn height)
        int rowGap = 40;

        g2.setColor(new Color(240, 244, 248));
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i = 0; i < 6; i++) {
            int btnY = firstButtonCenterY + (i * rowGap);

            // Left LSK Line: starts at left button border (X=54), goes horizontally to X=67.2,
            // then drops 45° to (69.2, btnY + 2.0), staying strictly within the 4px gray border [66.0, 70.0].
            Path2D.Float leftPath = new Path2D.Float();
            leftPath.moveTo(54, btnY);
            leftPath.lineTo(67.2f, btnY);
            leftPath.lineTo(69.2f, btnY + 2.0f);
            g2.draw(leftPath);

            // Right LSK Line: starts at right button border (X=w-54), goes horizontally to X=w-67.2,
            // then drops 45° to (w-69.2, btnY + 2.0), staying strictly within the 4px gray border [w-70.0, w-66.0].
            Path2D.Float rightPath = new Path2D.Float();
            rightPath.moveTo(w - 54, btnY);
            rightPath.lineTo(w - 67.2f, btnY);
            rightPath.lineTo(w - 69.2f, btnY + 2.0f);
            g2.draw(rightPath);
        }

        g2.dispose();
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
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    controller.handleKeyTyped("SP");
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
                } else if (c == ' ') {
                    controller.handleKeyTyped("SP");
                    return true;
                } else if (c >= ' ' && c != 127 || c > 127) {
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
