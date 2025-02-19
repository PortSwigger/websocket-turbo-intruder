package turbo;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class EngineIO {

    private static final Map<Integer, String> PACKET_TYPES = new HashMap<>();

    static {
        PACKET_TYPES.put(0, "open");
        PACKET_TYPES.put(1, "close");
        PACKET_TYPES.put(2, "ping");
        PACKET_TYPES.put(3, "pong");
        PACKET_TYPES.put(4, "message");
        PACKET_TYPES.put(5, "upgrade");
        PACKET_TYPES.put(6, "noop");
    }

    public static EngineIoMessage parseMessage(String input) throws IllegalArgumentException {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty");
        }

        // The first character indicates the packet type ID
        char typeChar = input.charAt(0);
        if (!Character.isDigit(typeChar)) {
            throw new IllegalArgumentException("Invalid packet type ID: " + typeChar);
        }

        int typeId = Character.getNumericValue(typeChar);
        String type = PACKET_TYPES.get(typeId);

        if (type == null) {
            throw new IllegalArgumentException("Unknown packet type ID: " + typeId);
        }

        // Extract the payload (everything after the first character)
        String payload = input.length() > 1 ? input.substring(1) : "";
        // If the type is "open" (handshake), parse the JSON payload
        if (typeId == 0) {
            try {
                JSONObject json = new JSONObject(payload);
                return new EngineIoMessage(type, json.getString("sid"), json.getInt("pingInterval"), json.getInt("pingTimeout"), json.getInt("maxPayload"));
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse handshake data: " + e.getMessage());
            }
        }
        throw new IllegalArgumentException("Failed to parse handshake data!");
    }

    public static class EngineIoMessage {
        private final String type;
        private String sid;
        private int pingInterval;
        private int pingTimeout;
        private int maxPayload;

        public EngineIoMessage(String type, String sid, int pingInterval, int pingTimeout, int maxPayload) {
            this.type = type;
            this.sid = sid;
            this.pingInterval = pingInterval;
            this.pingTimeout = pingTimeout;
            this.maxPayload = maxPayload;
        }

        public String getType() {
            return type;
        }

        public String getSid() {
            return sid;
        }

        public void setSid(String sid) {
            this.sid = sid;
        }

        public int getPingInterval() {
            return pingInterval;
        }

        public void setPingInterval(int pingInterval) {
            this.pingInterval = pingInterval;
        }

        public int getPingTimeout() {
            return pingTimeout;
        }

        public void setPingTimeout(int pingTimeout) {
            this.pingTimeout = pingTimeout;
        }

        public int getMaxPayload() {
            return maxPayload;
        }

        public void setMaxPayload(int maxPayload) {
            this.maxPayload = maxPayload;
        }
    }

}
