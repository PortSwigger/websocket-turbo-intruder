package standalone;

import burp.api.montoya.websocket.Direction;
import data.ConnectionMessage;
import logger.Logger;
import org.json.JSONObject;
import utils.MessageUtilities;
import utils.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class StandaloneWebSocketMessageModel {
    private final Logger logger;
    private final List<ConnectionMessage> connectionMessageList;
    private final String logFilePath;

    public StandaloneWebSocketMessageModel(Logger logger, String logFilePath) {
        this.logger = logger;
        this.logFilePath = logFilePath;
        this.connectionMessageList = new ArrayList<>();
    }

    private void saveToFile(Pair<ConnectionMessage, ConnectionMessage> requestResponse) {
        String request = requestResponse.getFirst() == null ? "" :
                Base64.getUrlEncoder().encodeToString(requestResponse.getFirst().getMessageByteBuffer().array());
        ConnectionMessage message = requestResponse.getSecond();
        JSONObject json = new JSONObject();
        json.put("id", message.getId());
        json.put("hash", message.getHash());
        json.put("duration", message.getTime());
        json.put("time", message.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm:ss.SS dd LLL yyyy")));
        json.put("comment", message.getComment());
        json.put("request", request);
        json.put("response", Base64.getUrlEncoder().encodeToString(message.getMessageByteBuffer().array()));

        try (FileWriter writer = new FileWriter(logFilePath, true)) {
            writer.write(json + System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            logger.logError(e.getMessage());
        }
    }

    public void add(ConnectionMessage connectionMessage) {
        int index = connectionMessageList.size();
        connectionMessageList.add(connectionMessage);
        Pair<ConnectionMessage, ConnectionMessage> requestResponse = getPair(index);
        if (requestResponse.getSecond() != null) {
            saveToFile(requestResponse);
            logger.logOutput(MessageUtilities.messagePairToStringSimple(requestResponse));
        }
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
}
