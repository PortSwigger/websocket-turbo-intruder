package ui.attack.table;

import burp.api.montoya.websocket.Direction;
import data.ConnectionMessage;
import utils.Pair;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.awt.EventQueue.invokeLater;

public class TurboWebSocketMessageTableModel extends AbstractTableModel {
    private final List<ConnectionMessage> connectionMessageList;
    private List<ConnectionMessage> displayConnectionMessageList;

    public TurboWebSocketMessageTableModel() {
        this.connectionMessageList = new ArrayList<>();
        this.displayConnectionMessageList = new ArrayList<>();
    }

    @Override
    public int getRowCount() {
        return displayConnectionMessageList.size();
    }

    @Override
    public int getColumnCount() {
        return AttackTableColumns.values().length;
    }

    @Override
    public String getColumnName(int column) {
        return AttackTableColumns.headerWithIndex(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ConnectionMessage webSocketConnectionMessage = displayConnectionMessageList.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> rowIndex;
            case 1 -> webSocketConnectionMessage.getDirection();
            case 2 -> webSocketConnectionMessage.getMessageLength();
            case 3 ->
                    webSocketConnectionMessage.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SS dd LLL yyyy"));
            case 4 -> webSocketConnectionMessage.getComment();
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return AttackTableColumns.typeForIndex(columnIndex);
    }

    public void add(ConnectionMessage connectionMessage) {
        invokeLater(() ->
        {
            if (connectionMessage.getDirection() == Direction.CLIENT_TO_SERVER) {
                connectionMessageList.add(connectionMessage);
            } else {
                int index = displayConnectionMessageList.size();
                displayConnectionMessageList.add(connectionMessage);
                fireTableRowsInserted(index, index);
            }
        });
    }

    public ConnectionMessage get(int rowIndex) {
        return rowIndex < 0 || rowIndex >= displayConnectionMessageList.size()
                ? null
                : displayConnectionMessageList.get(rowIndex);
    }

    public Pair<ConnectionMessage, ConnectionMessage> getPair(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= displayConnectionMessageList.size()) return null;
        ConnectionMessage selected = displayConnectionMessageList.get(rowIndex);
        Optional<ConnectionMessage> request = connectionMessageList
                .stream()
                .filter(connectionMessage -> connectionMessage.getDirection() == Direction.CLIENT_TO_SERVER)
                .filter(connectionMessage -> connectionMessage.getHash() == selected.getHash())
                .findFirst();
        return request.map(connectionMessage -> new Pair<>(connectionMessage, selected)).orElseGet(() -> new Pair<>(null, selected));

    }

    public void clear() {
        int rows = getRowCount();

        connectionMessageList.clear();

        if (rows == 0) {
            return;
        }
        displayConnectionMessageList.clear();

        fireTableRowsDeleted(0, rows - 1);
    }
}
