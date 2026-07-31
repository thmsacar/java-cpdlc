package gui;

import hoppie.AcarsMessage;
import service.CpdlcListener;
import service.CpdlcService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

public class MessageListPanel extends JPanel implements CpdlcListener {

    private final CpdlcService service;
    private final DefaultListModel<AcarsMessage> messageModel;
    private final JList<AcarsMessage> messageList;
    private final Consumer<AcarsMessage> onMessageSelected;

    public MessageListPanel(CpdlcService service, Consumer<AcarsMessage> onMessageSelected) {
        this.service = service;
        this.onMessageSelected = onMessageSelected;
        this.messageModel = new DefaultListModel<>();
        this.messageList = new JList<>(messageModel);
        
        setupUI();
        service.addListener(this);
        
        // Load initial messages
        onMessagesUpdated(service.getMessages());
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        
        messageList.setCellRenderer(new MessageListCellRenderer(service.getCallsign()));
        messageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    int index = messageList.locationToIndex(e.getPoint());
                    if (index >= 0 && index < messageModel.getSize()) {
                        Rectangle bounds = messageList.getCellBounds(index, index);
                        if (bounds != null && bounds.contains(e.getPoint())) {
                            AcarsMessage msg = messageModel.getElementAt(index);
                            if (msg != null) {
                                msg.setRead(true);
                                messageModel.set(index, msg);
                                onMessageSelected.accept(msg);
                            }
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(messageList);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void clearSelection() {
        messageList.clearSelection();
    }

    @Override
    public void onMessageReceived(AcarsMessage message) {
        // Handled by onMessagesUpdated or we could optimize
    }

    @Override
    public void onMessagesUpdated(List<AcarsMessage> messages) {
        SwingUtilities.invokeLater(() -> {
            messageModel.clear();
            for (AcarsMessage msg : messages) {
                messageModel.addElement(msg);
            }
        });
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {}

    @Override
    public void onAtsUnitChanged(String atsUnit) {}

    @Override
    public void onError(String message) {}

    private static class MessageListCellRenderer extends DefaultListCellRenderer {
        private final String callsign;
        private final JPanel rendererPanel = new JPanel(new BorderLayout(8, 0));
        private final JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        private final JLabel unreadDotLabel = new JLabel();
        private final JLabel arrowLabel = new JLabel();
        private final JLabel textLabel = new JLabel();

        public MessageListCellRenderer(String callsign) {
            this.callsign = callsign;
            leftBox.setOpaque(false);
            leftBox.add(unreadDotLabel);
            leftBox.add(arrowLabel);

            rendererPanel.add(leftBox, BorderLayout.WEST);
            rendererPanel.add(textLabel, BorderLayout.CENTER);

            arrowLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
            unreadDotLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
            textLabel.setFont(FontManager.REGULAR.deriveFont(14f));
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (value instanceof AcarsMessage) {
                AcarsMessage msg = (AcarsMessage) value;
                java.util.Map<String, String> format = msg.getListFormat(callsign);
                arrowLabel.setText(format.get("symbol"));
                textLabel.setText(format.get("entry"));

                boolean isOutgoing = msg.getFrom().equalsIgnoreCase(callsign);
                boolean isUnread = !msg.isRead() && !isOutgoing;

                Color indicatorColor;
                if ("telex".equalsIgnoreCase(msg.getType())) {
                    indicatorColor = new Color(245, 200, 100); // Soft warm gold/amber
                } else if ("system".equalsIgnoreCase(msg.getType())) {
                    indicatorColor = new Color(245, 130, 130); // Soft coral
                } else {
                    indicatorColor = Color.CYAN; // CPDLC cyan
                }

                if (isUnread) {
                    unreadDotLabel.setText("●");
                    unreadDotLabel.setForeground(indicatorColor);
                    arrowLabel.setForeground(indicatorColor);
                    textLabel.setForeground(Color.WHITE);
                    textLabel.setFont(FontManager.BOLD.deriveFont(14f));


                } else {
                    unreadDotLabel.setText(" ");
                    arrowLabel.setForeground(new Color(128, 128, 128));
                    textLabel.setForeground(new Color(128, 128, 128));
                    textLabel.setFont(FontManager.REGULAR.deriveFont(14f));

                }

                rendererPanel.setOpaque(true);
                rendererPanel.setBackground(isSelected ? new Color(60, 60, 60) : list.getBackground());
                rendererPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 40)),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));

            }
            return rendererPanel;
        }
    }
}
