package embedded;


import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import java.util.ArrayList;
import java.util.List;

import static java.awt.EventQueue.invokeLater;

public class TurboTableModel extends AbstractTableModel {
    private final TurboTableItemList items;

    public TurboTableModel(TurboTableItemList items) {
        this.items = items;
        items.addChangeListener(evt ->
                SwingUtilities.invokeLater(this::fireTableDataChanged)
        );
    }

    @Override
    public int getRowCount() {
        return items.getItems().size();
    }

    @Override
    public int getColumnCount() {
        return WrappedConnectionTableColumns.values().length;
    }

    @Override
    public String getColumnName(int column) {
        return TurboTableColumns.headerWithIndex(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TurboTableItem connection = items.getItems().get(rowIndex);

        return switch (columnIndex) {
            case 0 -> rowIndex;
            case 1 -> connection.getURL();
            case 2 -> connection.getComment();
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return TurboTableColumns.typeForIndex(columnIndex);
    }

    public void add(TurboTableItem item) {
        invokeLater(() ->
        {
            int index = items.getItems().size();
            items.getItems().add(item);
            fireTableRowsInserted(index, index);
        });
    }

    public void removeRow(int rowIndex) {
        items.removeItem(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    public TurboTableItem get(int rowIndex) {
        return rowIndex < 0 || rowIndex >= items.getItems().size()
                ? null
                : items.getItems().get(rowIndex);
    }

    public List<TurboTableItem> getRows(int[] rowIndices) {
        List<TurboTableItem> result = new ArrayList<>();

        for (int rowIndex : rowIndices) {
            TurboTableItem item = get(rowIndex);
            if (item != null) {
                result.add(item);
            }
        }

        return result;
    }

    public void clear() {
        int rows = getRowCount();

        if (rows == 0) {
            return;
        }

        items.getItems().clear();
        fireTableRowsDeleted(0, rows - 1);
    }
}
