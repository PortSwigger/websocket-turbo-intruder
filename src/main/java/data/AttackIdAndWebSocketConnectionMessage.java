package data;

public record AttackIdAndWebSocketConnectionMessage(int attackId,
                                                    ConnectionMessage connectionMessage) implements AttackMessage {
}