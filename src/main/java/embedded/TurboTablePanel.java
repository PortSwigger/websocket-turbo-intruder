package embedded;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.organizer.Organizer;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import ui.attack.table.PercentageBasedColumnWidthTable;

import ui.editor.IconTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.Comparator;

import static java.awt.EventQueue.invokeLater;
import static javax.swing.SwingConstants.LEFT;
import static utils.Utilities.createLabeledComponent;


public class TurboTablePanel extends JPanel {
    private final UserInterface userInterface;
    private final Organizer organizer;
    private HttpRequestEditor httpRequestEditor;

    public TurboTablePanel(
            Organizer organizer,
            UserInterface userInterface,
            TurboTableModel tableModel
    ) {
        super(new BorderLayout());
        this.userInterface = userInterface;
        this.organizer = organizer;
        JTable connectionTable = new PercentageBasedColumnWidthTable(TurboTableColumns.columnWidthPercentages());
        connectionTable.setModel(tableModel);
        connectionTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                if (tableModel.getRowCount() > 0) {
                    SwingUtilities.invokeLater(() ->
                            connectionTable.setRowSelectionInterval(0, 0)
                    );
                }
            }
        });

        // Set up table row sorter for header click sorting
        TableRowSorter<TurboTableModel> sorter = new TableRowSorter<>(tableModel);
        setupColumnComparators(sorter);
        connectionTable.setRowSorter(sorter);

        connectionTable.setDefaultRenderer(String.class, new LeftAlignedCellRender());
        connectionTable.setDefaultRenderer(Integer.class, new LeftAlignedCellRender());

        userInterface.applyThemeToComponent(connectionTable);
        JTableHeader tableHeader = connectionTable.getTableHeader();
        tableHeader.setDefaultRenderer(new LeftAlignedTableHeaderDecorator(tableHeader.getDefaultRenderer()));
        tableHeader.setToolTipText("Click column headers to sort");
        ListSelectionModel selectionModel = connectionTable.getSelectionModel();
        selectionModel.addListSelectionListener(e ->
        {
            if (e.getValueIsAdjusting()) {
                return;
            }

            int selectedRow = selectionModel.getMaxSelectionIndex();
            if (selectedRow >= 0) {
                // Convert view row index to model row index because of sorting
                int modelRow = connectionTable.convertRowIndexToModel(selectedRow);
                TurboTableItem item = tableModel.get(modelRow);

                if (item != null) {
                    SwingUtilities.invokeLater(() ->
                            httpRequestEditor.setRequest(item.getHttpRequest().isPresent() ? item.getHttpRequest().get() :
                                    HttpRequest.httpRequest(item.getContent()))
                    );
                }
            }
        });

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem clearTableMenuItem = new JMenuItem("Clear table");
        clearTableMenuItem.addActionListener(l -> invokeLater(tableModel::clear));
        JMenuItem organizerTableMenuItem = new JMenuItem("Send to Organizer");

        organizerTableMenuItem.addActionListener(e -> {
            int[] selectedRows = selectionModel.getSelectedIndices();

            if (selectedRows.length > 0) {
                List<TurboTableItem> items = tableModel.getRows(selectedRows);

                for (TurboTableItem item : items) {
                    if (item.getHttpRequest().isPresent()) {
                        organizer.sendToOrganizer(item.getHttpRequest().get());
                    }
                }
            }
        });
        popupMenu.add(organizerTableMenuItem);
        popupMenu.add(clearTableMenuItem);
        connectionTable.setComponentPopupMenu(popupMenu);

        // Create top panel with Messages label and search input
        JPanel topPanel = createTopPanel(sorter);

        JScrollPane scrollPane = new JScrollPane(connectionTable);

        // Create panel that contains both top panel and scroll pane
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(topPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        JSplitPane view = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                tablePanel,
                createLabeledComponent("Request", getHttpRequestEditor())
        );
        view.setResizeWeight(0.5);
        view.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (getWidth() > 0) {
                    view.setDividerLocation(0.5);
                    removeComponentListener(this);
                }
            }
        });
        this.add(view, BorderLayout.CENTER);
    }

    private JPanel createTopPanel(TableRowSorter<TurboTableModel> sorter) {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Messages label on the left
        JLabel messagesLabel = new JLabel("Messages");
        messagesLabel.setFont(messagesLabel.getFont().deriveFont(Font.BOLD, messagesLabel.getFont().getSize()));

        // Search input on the right
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        IconTextField searchField = new IconTextField("Search");

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFilter();
            }

            private void updateFilter() {
                String text = searchField.getText().trim().toLowerCase();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(new RowFilter<TurboTableModel, Object>() {
                        @Override
                        public boolean include(Entry<? extends TurboTableModel, ? extends Object> entry) {
                            for (int i = 0; i < entry.getValueCount(); i++) {
                                Object value = entry.getValue(i);
                                if (value != null && value.toString().toLowerCase().contains(text)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                }
            }
        });
        // Add document listener for real-time filtering
//        searchField.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void insertUpdate(DocumentEvent e) {
//                updateFilter();
//            }
//
//            @Override
//            public void removeUpdate(DocumentEvent e) {
//                updateFilter();
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent e) {
//                updateFilter();
//            }
//
//            private void updateFilter() {
//                String text = searchField.getText().trim().toLowerCase();
//                if (text.isEmpty()) {
//                    sorter.setRowFilter(null);
//                } else {
//                    // Create a filter that searches across all columns using substring match
//                    sorter.setRowFilter(new RowFilter<TurboTableModel, Object>() {
//                        @Override
//                        public boolean include(Entry<? extends TurboTableModel, ? extends Object> entry) {
//                            // Check all columns for the search text
//                            for (int i = 0; i < entry.getValueCount(); i++) {
//                                Object value = entry.getValue(i);
//                                if (value != null && value.toString().toLowerCase().contains(text)) {
//                                    return true;
//                                }
//                            }
//                            return false;
//                        }
//                    });
//                }
//            }
//        });

        searchPanel.add(searchField);

        topPanel.add(messagesLabel, BorderLayout.WEST);
        topPanel.add(searchPanel, BorderLayout.EAST);

        // Apply theme to components
        userInterface.applyThemeToComponent(topPanel);

        return topPanel;
    }

    private void setupColumnComparators(TableRowSorter<TurboTableModel> sorter) {
        sorter.setComparator(0, Comparator.comparing(o -> (Integer) o));

        sorter.setComparator(1, Comparator.comparing(o -> ((String) o).toLowerCase()));

        sorter.setComparator(2, Comparator.comparing(o -> {
            String comment = (String) o;
            return comment != null ? comment.toLowerCase() : "";
        }));
    }

    private Component getHttpRequestEditor() {
        httpRequestEditor = userInterface.createHttpRequestEditor(EditorOptions.READ_ONLY);
        httpRequestEditor.setRequest(null);
        return httpRequestEditor.uiComponent();
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
