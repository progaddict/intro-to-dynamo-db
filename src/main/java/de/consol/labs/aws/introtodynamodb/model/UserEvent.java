package de.consol.labs.aws.introtodynamodb.model;

import java.util.Map;

public class UserEvent {

    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_EXPIRES_AFTER = "expiresAfter";
    public static final String FIELD_EVENT_TYPE = "eventType";
    public static final String FIELD_EVENT_DATA = "eventData";

    private Long userId;
    private Long timestamp;
    private String eventType;
    private Map<String, Object> eventData;

    public Long getUserId() {
        return userId;
    }

    public UserEvent setUserId(final Long userId) {
        this.userId = userId;
        return this;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public UserEvent setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getEventType() {
        return eventType;
    }

    public UserEvent setEventType(final String eventType) {
        this.eventType = eventType;
        return this;
    }

    public Map<String, Object> getEventData() {
        return eventData;
    }

    public UserEvent setEventData(final Map<String, Object> eventData) {
        this.eventData = eventData;
        return this;
    }

    @Override
    public String toString() {
        return "UserEvent{" +
                "userId=" + userId +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", eventData=" + eventData +
                '}';
    }
}
