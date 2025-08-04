package embedded;

import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import ui.attack.table.PercentageBasedColumnWidthTable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.function.Consumer;

import static java.awt.EventQueue.invokeLater;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static javax.swing.SwingConstants.LEFT;


public class WrappedConnectionTable extends JPanel {
    public WrappedConnectionTable(
            UserInterface userInterface,
            WrappedConnectionTableModel tableModel,
            Consumer<ConnectionWrapper> selectedConnection
    ) {
        super(new BorderLayout());
        JTable connectionTable = new PercentageBasedColumnWidthTable(WrappedConnectionTableColumns.columnWidthPercentages());
        connectionTable.setModel(tableModel);
        connectionTable.setSelectionMode(SINGLE_SELECTION);
        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.INSERT) {
                if (tableModel.getRowCount() > 0) {
                    SwingUtilities.invokeLater(() ->
                            connectionTable.setRowSelectionInterval(0, 0)
                    );
                }
            }
        });

        connectionTable.setDefaultRenderer(ExtensionWebSocketCreationStatus.class, new StatusCellRenderer());
        connectionTable.setDefaultRenderer(String.class, new LeftAlignedCellRender());
        connectionTable.setDefaultRenderer(Integer.class, new LeftAlignedCellRender());

        userInterface.applyThemeToComponent(connectionTable);
        JTableHeader tableHeader = connectionTable.getTableHeader();
        tableHeader.setDefaultRenderer(new LeftAlignedTableHeaderDecorator(tableHeader.getDefaultRenderer()));
        ListSelectionModel selectionModel = connectionTable.getSelectionModel();
        selectionModel.addListSelectionListener(e ->
        {
            if (e.getValueIsAdjusting()) {
                return;
            }

            ConnectionWrapper connection = tableModel.get(selectionModel.getMaxSelectionIndex());

            if (connection != null) {
                selectedConnection.accept(connection);
            }
        });

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem clearTableMenuItem = new JMenuItem("Clear table");
        clearTableMenuItem.addActionListener(l -> invokeLater(tableModel::clear));

        popupMenu.add(clearTableMenuItem);
        connectionTable.setComponentPopupMenu(popupMenu);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(11, 8, 11, 8));
        JLabel messagesLabel = new JLabel("Connections");
        messagesLabel.setFont(messagesLabel.getFont().deriveFont(Font.BOLD, messagesLabel.getFont().getSize()));
        topPanel.add(messagesLabel, BorderLayout.WEST);
        userInterface.applyThemeToComponent(topPanel);

        JScrollPane scrollPane = new JScrollPane(connectionTable);

        // Create panel that contains both top panel and scroll pane
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(topPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        this.add(tablePanel, BorderLayout.CENTER);

//        JScrollPane scrollPane = new JScrollPane(connectionTable);
//
//        this.add(scrollPane, BorderLayout.CENTER);
    }

    private static class StatusCellRenderer extends DefaultTableCellRenderer {

        public StatusCellRenderer() {
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (component instanceof JLabel label && value instanceof ExtensionWebSocketCreationStatus status) {
                String text = switch (status) {
                    case SUCCESS -> "Success";
                    case INVALID_HOST -> "Invalid host";
                    case INVALID_PORT -> "Invalid port";
                    case STREAMING_RESPONSE -> "Streaming response";
                    case UNKNOWN_HOST -> "Unknown host";
                    case CONNECTION_FAILED -> "Connection failed";
                    case INVALID_REQUEST -> "Invalid request";
                    case NON_UPGRADE_RESPONSE -> "None upgrade response";
                    default -> "";
                };

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
