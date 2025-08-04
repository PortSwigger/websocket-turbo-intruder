package embedded;


import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

import static java.awt.EventQueue.invokeLater;

public class WrappedConnectionTableModel extends AbstractTableModel {
    private final List<ConnectionWrapper> connectionsList;

    public WrappedConnectionTableModel() {
        this.connectionsList = new ArrayList<>();
    }

    @Override
    public int getRowCount() {
        return connectionsList.size();
    }

    @Override
    public int getColumnCount() {
        return WrappedConnectionTableColumns.values().length;
    }

    @Override
    public String getColumnName(int column) {
        return WrappedConnectionTableColumns.headerWithIndex(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ConnectionWrapper connection = connectionsList.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> rowIndex;
            case 1 -> connection.getURL();
            case 2 -> connection.getStatus();
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return WrappedConnectionTableColumns.typeForIndex(columnIndex);
    }

    public void add(ConnectionWrapper connection) {
        invokeLater(() ->
        {
            int index = connectionsList.size();
            connectionsList.add(connection);
            fireTableRowsInserted(index, index);
        });
    }

    public ConnectionWrapper get(int rowIndex) {
        return rowIndex < 0 || rowIndex >= connectionsList.size()
                ? null
                : connectionsList.get(rowIndex);
    }

    public void clear() {
        int rows = getRowCount();

        if (rows == 0) {
            return;
        }

        connectionsList.clear();
        fireTableRowsDeleted(0, rows - 1);
    }
}
