package ui.attack.table;

import burp.api.montoya.websocket.Direction;
import data.ConnectionMessage;
import utils.Pair;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.awt.EventQueue.invokeLater;

public class WebSocketMessageTableModel extends AbstractTableModel {
    private static final int MAX_MESSAGES = 1000;
    private final boolean limitEnabled;
    private final List<ConnectionMessage> messages;
    private volatile boolean loggingEnabled = true;

    public WebSocketMessageTableModel() {
        this.limitEnabled = false;
        this.messages = Collections.synchronizedList(new ArrayList<>());
    }

    public WebSocketMessageTableModel(boolean limitEnabled) {
        this.limitEnabled = limitEnabled;
        this.messages = Collections.synchronizedList(new ArrayList<>(MAX_MESSAGES));
    }

    @Override
    public int getRowCount() {
        synchronized (messages) {
            return messages.size();
        }
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
    public Class<?> getColumnClass(int columnIndex) {
        return AttackTableColumns.typeForIndex(columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ConnectionMessage msg;
        synchronized (messages) {
            msg = messages.get(rowIndex);
        }
        return switch (columnIndex) {
            case 0 -> rowIndex;
            case 1 -> msg.getHash();
            case 2 -> msg.getDirection();
            case 3 -> msg.getMessageLength();
            case 4 -> msg.getDateTime()
                    .format(DateTimeFormatter.ofPattern(
                            "HH:mm:ss.SSS dd LLL yyyy"));
            case 5 -> msg.getTime();
            case 6 -> msg.getComment();
            default -> "";
        };
    }

    public void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
    }

    public void add(ConnectionMessage connectionMessage) {
        if (!loggingEnabled) return;
        if (limitEnabled) {
            invokeLater(() -> {
                synchronized (messages) {
                    boolean evicted = false;
                    if (messages.size() >= MAX_MESSAGES) {
                        messages.remove(0);
                        evicted = true;
                    }
                    messages.add(connectionMessage);
                    int newIndex = messages.size() - 1;
                    if (evicted) {
                        fireTableRowsDeleted(0, 0);
                    }
                    fireTableRowsInserted(newIndex, newIndex);
                }
            });
        } else {
            invokeLater(() ->
            {
                int index = messages.size();
                messages.add(connectionMessage);
                fireTableRowsInserted(index, index);
            });
        }
    }

    public void clear() {
        invokeLater(() -> {
            synchronized (messages) {
                int size = messages.size();
                if (size > 0) {
                    messages.clear();
                    fireTableRowsDeleted(0, size - 1);
                }
            }
        });
    }

    public ConnectionMessage get(int rowIndex) {
        synchronized (messages) {
            if (rowIndex < 0 || rowIndex >= messages.size()) {
                return null;
            }
            return messages.get(rowIndex);
        }
    }

    public Pair<ConnectionMessage, ConnectionMessage> getPair(int rowIndex) {
        synchronized (messages) {
            if (rowIndex < 0 || rowIndex >= messages.size()) {
                return null;
            }
            ConnectionMessage selected = messages.get(rowIndex);
            long hash = selected.getHash();
            if (selected.getDirection() == Direction.CLIENT_TO_SERVER) {
                Optional<ConnectionMessage> response = messages.stream()
                        .filter(m -> m.getDirection() == Direction.SERVER_TO_CLIENT)
                        .filter(m -> m.getHash() == hash)
                        .findFirst();
                return new Pair<>(selected, response.orElse(null));
            } else {
                Optional<ConnectionMessage> request = messages.stream()
                        .filter(m -> m.getDirection() == Direction.CLIENT_TO_SERVER)
                        .filter(m -> m.getHash() == hash)
                        .findFirst();
                return new Pair<>(request.orElse(null), selected);
            }
        }
    }
}
