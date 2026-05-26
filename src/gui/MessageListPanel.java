package gui;

import hoppie.AcarsMessage;
import service.CpdlcListener;
import service.CpdlcService;

import javax.swing.*;
import java.awt.*;
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
        messageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onMessageSelected.accept(messageList.getSelectedValue());
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
        private final JPanel rendererPanel = new JPanel(new BorderLayout(10, 0));
        private final JLabel arrowLabel = new JLabel();
        private final JLabel textLabel = new JLabel();

        public MessageListCellRenderer(String callsign) {
            this.callsign = callsign;
            rendererPanel.add(arrowLabel, BorderLayout.WEST);
            rendererPanel.add(textLabel, BorderLayout.CENTER);
            arrowLabel.setFont(new Font("Monospaced", Font.BOLD, 20));
            textLabel.setFont(FontManager.REGULAR.deriveFont(14f));
            rendererPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (value instanceof AcarsMessage) {
                AcarsMessage msg = (AcarsMessage) value;
                java.util.Map<String, String> format = msg.getListFormat(callsign);
                arrowLabel.setText(format.get("symbol"));
                textLabel.setText(format.get("entry"));
            }

            if (isSelected) {
                rendererPanel.setOpaque(true);
                rendererPanel.setBackground(new Color(45, 45, 45));
            } else {
                rendererPanel.setOpaque(false);
                rendererPanel.setBackground(list.getBackground());
            }
            return rendererPanel;
        }
    }
}
