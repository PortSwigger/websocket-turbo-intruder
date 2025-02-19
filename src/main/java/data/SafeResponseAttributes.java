package data;

import org.json.JSONException;
import org.json.JSONObject;
import utils.StringUtillities;

import java.util.*;

public class SafeResponseAttributes {
    private static double TOLERANCE = 0.1;
    private Map<WebSocketAttributeType, Integer> attributes;
    private Map<WebSocketAttributeType, Set<String>> stringAttributes;

    public SafeResponseAttributes(ConnectionMessage message) {
        this.attributes = new HashMap<>();
        this.stringAttributes = new HashMap<>();
        parseAttributes(message);
    }

    private void parseAttributes(ConnectionMessage message) {
        for (WebSocketAttributeType type : WebSocketAttributeType.values()) {
            switch (type) {
                case KEY_COUNT -> parseKeyCount(message);
                case KEY_NAMES -> parseKeyNames(message);
                case LINE_COUNT -> parseLineCount(message);
                case WORD_COUNT -> parseWordCount(message);
                case CONTENT_LENGTH -> parseContentLength(message);
//                case TIME -> parseTime(message);
                default -> {
                    // Handle unknown attribute types if necessary
                }
            }
        }
    }

    private void parseKeyCount(ConnectionMessage message) {
        if (message.getCode() == FrameType.TEXT.value) {
            try {
                JSONObject json = new JSONObject(message.getMessage());
                int keyCount = json.keySet().size();
                attributes.put(WebSocketAttributeType.KEY_COUNT, keyCount);
            } catch (JSONException exception) {
                // Handle JSON parsing exception
                attributes.put(WebSocketAttributeType.KEY_COUNT, 0);
            }
        } else {
            attributes.put(WebSocketAttributeType.KEY_COUNT, 0);
        }
    }

    private void parseKeyNames(ConnectionMessage message) {
        if (message.getCode() == FrameType.TEXT.value) {
            try {
                JSONObject json = new JSONObject(message.getMessage());
                Set<String> keys = json.keySet();
                stringAttributes.put(WebSocketAttributeType.KEY_NAMES, keys);
            } catch (JSONException exception) {
                // Handle JSON parsing exception
                stringAttributes.put(WebSocketAttributeType.KEY_NAMES, Collections.emptySet());
            }
        } else {
            stringAttributes.put(WebSocketAttributeType.KEY_NAMES, Collections.emptySet());
        }
    }

    private void parseLineCount(ConnectionMessage message) {
        if (message.getCode() == FrameType.TEXT.value) {
            String msg = message.getMessage();
            int lineCount = msg.split("\r\n|\r|\n").length;
            attributes.put(WebSocketAttributeType.LINE_COUNT, lineCount);
        } else {
            attributes.put(WebSocketAttributeType.LINE_COUNT, 0);
        }
    }

    private void parseWordCount(ConnectionMessage message) {
        if (message.getCode() == FrameType.TEXT.value) {
            String msg = message.getMessage();
            int wordCount = msg.trim().isEmpty() ? 0 : StringUtillities.countWords(msg);
            attributes.put(WebSocketAttributeType.WORD_COUNT, wordCount);
        } else {
            attributes.put(WebSocketAttributeType.WORD_COUNT, 0);
        }
    }

    private void parseContentLength(ConnectionMessage message) {
        attributes.put(WebSocketAttributeType.CONTENT_LENGTH, message.getMessageLength());
    }

    protected void parseTime(ConnectionMessage message) {
        attributes.put(WebSocketAttributeType.TIME, (int) message.getTime());
    }

    // Getter methods for attributes
    public Map<WebSocketAttributeType, Integer> getAttributes() {
        return attributes;
    }

    // Getter methods for attributes
    public Integer getAttribute(WebSocketAttributeType type) {
        return attributes.get(type);
    }

    public Set<String> getStringAttribute(WebSocketAttributeType type) {
        return stringAttributes.get(type);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;  // Same reference
        if (obj == null || getClass() != obj.getClass()) return false;  // Check null and class type

        SafeResponseAttributes that = (SafeResponseAttributes) obj;

        if (!Objects.equals(stringAttributes, that.stringAttributes)) {
            return false;
        }

        return attributes.entrySet().stream().allMatch(entry -> {
            WebSocketAttributeType key = entry.getKey();
            Integer thisValue = entry.getValue();
            Integer thatValue = that.attributes.get(key);

            if (thatValue == null) return false; // Ensure both maps contain the key

            if (thisValue == 0 && thatValue == 0) return true;

//            if (key == WebSocketAttributeType.TIME) {
//                if (thisValue == 0 || thatValue == 0) return true;
//                return Math.abs((double) (thatValue - thisValue) / thisValue) < 0.5;
//            }

            if (thisValue == 0 || thatValue == 0) {
                return Math.abs(thatValue - thisValue) <= 1; // Allow a minor absolute difference (e.g., ±1)
            }

            double relativeDifference = Math.abs((double) (thatValue - thisValue) / thisValue);
            return relativeDifference <= TOLERANCE;
        });
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, stringAttributes);
    }

    @Override
    public String toString() {
        return "SafeResponseAttributes{" +
                "attributes=" + attributes +
                ", stringAttributes=" + stringAttributes +
                '}';
    }
}

