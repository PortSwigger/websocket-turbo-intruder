package utils;

import burp.api.montoya.websocket.Direction;
import data.ConnectionMessage;
import data.Progress;
import ui.attack.table.AttackTableColumns;

import java.time.format.DateTimeFormatter;

public class MessageUtilities {
    private static final int DEFAULT_MESSAGE_LENGTH = 32;

    public static String messageToString(ConnectionMessage webSocketConnectionMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("( ");
        for (AttackTableColumns column: AttackTableColumns.values()) {
            sb.append(switch (column.ordinal()) {
                case 0 -> String.valueOf(webSocketConnectionMessage.getId());
                case 1 -> String.valueOf(webSocketConnectionMessage.getHash());
                case 2 -> webSocketConnectionMessage.getDirection() == Direction.CLIENT_TO_SERVER ?
                    "to server" : "to client";
                case 3 -> String.valueOf(webSocketConnectionMessage.getMessageLength());
                case 4 ->
                        webSocketConnectionMessage.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS dd LLL yyyy"));
                case 5 -> String.valueOf(webSocketConnectionMessage.getTime());
                case 6 -> webSocketConnectionMessage.getComment();
                default -> "";
            });
            sb.append(" ");
        }
        sb.append(" )");
        return sb.toString();
    }
    private static String formatFixedLength(String input, int length) {
        if (input.length() >= length) {
            return input.substring(0, length) + "...";
        } else {
            return String.format("%-" + length + "s", input);
        }
    }

    public static String messagePairToString(Pair<ConnectionMessage, ConnectionMessage> requestResponse) {
        StringBuilder stringBuilder = new StringBuilder();
        ConnectionMessage response = requestResponse.getSecond();
        long in = 0;
        if (requestResponse.getFirst() != null) {
            in = requestResponse.getFirst().getId();
            String request = requestResponse.getFirst().getMessage();
            stringBuilder.append(formatFixedLength(request, DEFAULT_MESSAGE_LENGTH)).append('\t');
        }
        Progress progress = new Progress(
                in,
                response.getId(),
                response.getHash(),
                response.getMessageLength(),
                response.getWordCount(),
                response.getTime(),
                response.getDateTime());
        stringBuilder.append(progress);
        return stringBuilder.toString();
    }
}
