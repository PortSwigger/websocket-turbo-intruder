package ui.attack.table;

import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.websocket.Direction;
import data.ConnectionMessage;
import ui.editor.IconTextField;
import utils.IconFactory;
import utils.ToggleIcon;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;

import static java.awt.EventQueue.invokeLater;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingConstants.LEFT;
import static utils.IconType.LEFT_ARROW;
import static utils.IconType.RIGHT_ARROW;

public class WebSocketMessageTable extends JPanel {
    public static final String TOOL_TIP_TEXT = "WebSocket Logger saves up to 1,000 messages";

    public WebSocketMessageTable(
            UserInterface userInterface,
            WebSocketMessageTableModel webSocketMessageTableModel,
            IconFactory iconFactory,
            Consumer<ConnectionMessage> messageConsumer
    ) {
        super(new BorderLayout());

        JTable webSocketMessageTable = new PercentageBasedColumnWidthTable(AttackTableColumns.columnWidthPercentages());
        webSocketMessageTable.setModel(webSocketMessageTableModel);
        webSocketMessageTable.setAutoCreateRowSorter(true);
        TableRowSorter<WebSocketMessageTableModel> sorter = new TableRowSorter<>(webSocketMessageTableModel);
        webSocketMessageTable.setRowSorter(sorter);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        IconTextField searchField = new IconTextField("Search");
//        JTextField searchField = new JTextField("Search", 20);
//        searchField.setToolTipText("Filter");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String text = searchField.getText().trim().toLowerCase();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(new RowFilter<>() {
                        @Override
                        public boolean include(Entry<? extends WebSocketMessageTableModel, ? extends Integer> entry) {
                            // entry.getIdentifier() is the model‐row index
                            int modelRow = entry.getIdentifier();
                            WebSocketMessageTableModel model = entry.getModel();
                            ConnectionMessage msg = model.get(modelRow);
                            if (msg == null) return false;
                            if (msg.getMessage() != null) {
                                return msg.getMessage().toLowerCase().contains(text);
                            }
                            if (msg.getComment() != null) {
                                return msg.getComment().toLowerCase().contains(text);
                            }
                            return false;
                        }
                    });
                }
            }

            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });

        webSocketMessageTable.setSelectionMode(SINGLE_SELECTION);

        webSocketMessageTable.setDefaultRenderer(Direction.class, new DirectionCellRenderer(iconFactory));
        webSocketMessageTable.setDefaultRenderer(String.class, new LeftAlignedCellRender());
        webSocketMessageTable.setDefaultRenderer(Integer.class, new LeftAlignedCellRender());

        userInterface.applyThemeToComponent(webSocketMessageTable);

        JTableHeader tableHeader = webSocketMessageTable.getTableHeader();
        tableHeader.setDefaultRenderer(new LeftAlignedTableHeaderDecorator(tableHeader.getDefaultRenderer()));

        ListSelectionModel selectionModel = webSocketMessageTable.getSelectionModel();
        selectionModel.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            // 1) get the selected row in the VIEW
            int viewRow = webSocketMessageTable.getSelectedRow();
            if (viewRow < 0) {
                return; // nothing selected
            }

            // 2) convert to the MODEL index
            int modelRow = webSocketMessageTable.convertRowIndexToModel(viewRow);

            // 3) now fetch the correct ConnectionMessage
            ConnectionMessage msg = webSocketMessageTableModel.get(modelRow);
            if (msg != null) {
                messageConsumer.accept(msg);
            }
        });

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem clearTableMenuItem = new JMenuItem("Clear table");
        clearTableMenuItem.addActionListener(l -> invokeLater(webSocketMessageTableModel::clear));

        popupMenu.add(clearTableMenuItem);
        webSocketMessageTable.setComponentPopupMenu(popupMenu);

        JScrollPane scrollPane = new JScrollPane(webSocketMessageTable);

        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        ToggleIcon switchIcon = new ToggleIcon(35, 18);
        JToggleButton loggingToggle = new JToggleButton();
        loggingToggle.setToolTipText(TOOL_TIP_TEXT);
        loggingToggle.setSelected(true);

        loggingToggle.setText("Logging On");
        loggingToggle.setIcon(switchIcon);
        loggingToggle.setSelectedIcon(switchIcon);
        loggingToggle.setHorizontalTextPosition(SwingConstants.RIGHT);
        loggingToggle.setBorderPainted(false);
        loggingToggle.setContentAreaFilled(false);

        loggingToggle.addItemListener(e -> {
            boolean on = (e.getStateChange() == ItemEvent.SELECTED);
            webSocketMessageTableModel.setLoggingEnabled(on);
            loggingToggle.setText(on ? "Logging On" : "Logging Off");
        });
        loggingToggle.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI());
        loggingToggle.setFont(userInterface.currentDisplayFont());
        userInterface.applyThemeToComponent(loggingToggle);

        searchPanel.add(searchField);
        togglePanel.add(loggingToggle);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        topPanel.add(togglePanel, BorderLayout.WEST);
        topPanel.add(searchPanel, BorderLayout.EAST);

        this.add(topPanel, BorderLayout.NORTH);
        this.add(scrollPane, BorderLayout.CENTER);

        userInterface.applyThemeToComponent(topPanel);
        userInterface.applyThemeToComponent(scrollPane);
    }

    private static class DirectionCellRenderer extends DefaultTableCellRenderer {
        private final IconFactory iconFactory;

        public DirectionCellRenderer(IconFactory iconFactory) {
            this.iconFactory = iconFactory;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (component instanceof JLabel label && value instanceof Direction direction) {
                String text = switch (direction) {
                    case SERVER_TO_CLIENT -> "To client";
                    case CLIENT_TO_SERVER -> "To server";
                };

                Icon icon = switch (direction) {
                    case SERVER_TO_CLIENT -> iconFactory.scaledIconFor(LEFT_ARROW, label.getFont().getSize());
                    case CLIENT_TO_SERVER -> iconFactory.scaledIconFor(RIGHT_ARROW, label.getFont().getSize());
                };

                label.setIcon(icon);
                label.setText(text);

                label.setHorizontalAlignment(LEFT);
            }

            return component;
        }
    }

    private static class LeftAlignedCellRender extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (component instanceof JLabel label) {
                label.setHorizontalAlignment(LEFT);
            }

            return component;
        }
    }

    private static class LeftAlignedTableHeaderDecorator implements TableCellRenderer {
        private final TableCellRenderer tableCellRenderer;

        public LeftAlignedTableHeaderDecorator(TableCellRenderer tableCellRenderer) {
            this.tableCellRenderer = tableCellRenderer;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (component instanceof JLabel label) {
                label.setHorizontalAlignment(LEFT);
            }

            return component;
        }
    }
}
