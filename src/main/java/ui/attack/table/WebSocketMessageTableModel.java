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

public class WebSocketMessageTableModel extends AbstractTableModel {
    private final List<ConnectionMessage> connectionMessageList;

    public WebSocketMessageTableModel() {
        this.connectionMessageList = new ArrayList<>();
    }

    @Override
    public int getRowCount() {
        return connectionMessageList.size();
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
        ConnectionMessage webSocketConnectionMessage = connectionMessageList.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> rowIndex;
            case 1 -> webSocketConnectionMessage.getHash();
            case 2 -> webSocketConnectionMessage.getDirection();
            case 3 -> webSocketConnectionMessage.getMessageLength();
            case 4 ->
                    webSocketConnectionMessage.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS dd LLL yyyy"));
            case 5 ->
                    webSocketConnectionMessage.getTime();
            case 6 -> webSocketConnectionMessage.getComment();
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
            int index = connectionMessageList.size();
            connectionMessageList.add(connectionMessage);
            fireTableRowsInserted(index, index);
        });
    }

    public ConnectionMessage get(int rowIndex) {
        return rowIndex < 0 || rowIndex >= connectionMessageList.size()
                ? null
                : connectionMessageList.get(rowIndex);
    }

    public Pair<ConnectionMessage, ConnectionMessage> getPair(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= connectionMessageList.size()) return null;
        ConnectionMessage selected = connectionMessageList.get(rowIndex);
        if (selected.getDirection() == Direction.CLIENT_TO_SERVER) {
            Optional<ConnectionMessage> response = connectionMessageList
                    .stream()
                    .filter(connectionMessage -> connectionMessage.getDirection() == Direction.SERVER_TO_CLIENT)
                    .filter(connectionMessage -> connectionMessage.getHash() == selected.getHash())
                    .findFirst();
            return response.map(connectionMessage -> new Pair<>(selected, connectionMessage)).orElseGet(() -> new Pair<>(selected, null));
        } else {
            Optional<ConnectionMessage> request = connectionMessageList
                    .stream()
                    .filter(connectionMessage -> connectionMessage.getDirection() == Direction.CLIENT_TO_SERVER)
                    .filter(connectionMessage -> connectionMessage.getHash() == selected.getHash())
                    .findFirst();
            return request.map(connectionMessage -> new Pair<>(connectionMessage, selected)).orElseGet(() -> new Pair<>(null, selected));
        }
    }

    public void clear() {
        int rows = getRowCount();

        if (rows == 0) {
            return;
        }

        connectionMessageList.clear();
        fireTableRowsDeleted(0, rows - 1);
    }
}
