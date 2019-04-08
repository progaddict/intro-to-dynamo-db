package de.consol.labs.aws.introtodynamodb;

import de.consol.labs.aws.introtodynamodb.model.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static de.consol.labs.aws.introtodynamodb.model.UserEvent.*;

@RestController
@RequestMapping("user-event")
public class UserEventResource {

    private static final Logger LOG = LoggerFactory.getLogger(UserEventResource.class);

    @Value("DYNAMO_DB_TABLE_NAME")
    private String dynamoDbTable;

    private final DynamoDbClient client = DynamoDbClient.create();

    @PutMapping
    public void saveUserEvents(@RequestBody final List<UserEvent> events) {
        if (events == null || events.isEmpty()) {
            LOG.debug("received no events");
            return;
        }
        LOG.debug("received {} events", events.size());
        final List<WriteRequest> items = events
                .stream()
                .map(UserEventResource::getWriteRequest)
                .collect(Collectors.toList());
        BatchWriteItemRequest request = BatchWriteItemRequest
                .builder()
                .requestItems(Collections.singletonMap(dynamoDbTable, items))
                .build();
        LOG.debug("writing events to DB");
        client.batchWriteItem(request);
        LOG.debug("events have been written");
    }

    @GetMapping
    public List<UserEvent> getUserEvents() {
        final ScanRequest request = ScanRequest.builder().build();
        LOG.debug("performing scan");
        final ScanResponse response = client.scan(request);
        LOG.debug("scan successful, response = {}", response);
        final List<UserEvent> events = response
                .items()
                .stream()
                .map(UserEventResource::getUserEvent)
                .collect(Collectors.toList());
        LOG.debug("returning {} events", events.size());
        return events;
    }

    @GetMapping("last")
    public List<UserEvent> getLastEvents(
            @RequestParam("userId") final int userId,
            @RequestParam(value = "n", required = false, defaultValue = "10") final Integer n
    ) {
        final Map<String, AttributeValue> attributeValues = Collections.singletonMap(
                ":userId",
                AttributeValue.builder().n(String.valueOf(userId)).build()
        );
        final QueryRequest query = QueryRequest.builder()
                .keyConditionExpression("userId = :userId")
                .expressionAttributeValues(attributeValues)
                .limit(n)
                .build();
        LOG.debug("performing query");
        final QueryResponse response = client.query(query);
        LOG.debug("query successful, response = {}", response);
        final List<UserEvent> events = response
                .items()
                .stream()
                .map(UserEventResource::getUserEvent)
                .collect(Collectors.toList());
        LOG.debug("returning {} events", events.size());
        return events;
    }

    private static WriteRequest getWriteRequest(final UserEvent event) {
        final PutRequest putRequest = PutRequest
                .builder()
                .item(getItem(event))
                .build();
        return WriteRequest
                .builder()
                .putRequest(putRequest)
                .build();
    }

    private static Map<String, AttributeValue> getItem(final UserEvent event) {
        final long userId = Objects.requireNonNull(event.getUserId());
        final long timestamp = Objects.requireNonNull(event.getTimestamp());
        final long expiresAfter = Instant.now().plus(Duration.ofMinutes(3)).toEpochMilli();
        final String eventType = Objects.requireNonNull(event.getEventType());
        Map<String, AttributeValue> result = new HashMap<>();
        result.put(
                FIELD_USER_ID,
                AttributeValue.builder().n(String.valueOf(userId)).build()
        );
        result.put(
                FIELD_TIMESTAMP,
                AttributeValue.builder().n(String.valueOf(timestamp)).build()
        );
        result.put(
                FIELD_EXPIRES_AFTER,
                AttributeValue.builder().n(String.valueOf(expiresAfter)).build()
        );
        result.put(
                FIELD_EVENT_TYPE,
                AttributeValue.builder().s(eventType).build()
        );
        if (event.getEventData() != null && !event.getEventData().isEmpty()) {
            result.put(
                    FIELD_EVENT_DATA,
                    AttributeValue.builder().m(getItemForMap(event.getEventData())).build()
            );
        }
        return result;
    }

    private static Map<String, AttributeValue> getItemForMap(final Map<?, ?> data) {
        Map<String, AttributeValue> result = new HashMap<>();
        for (final Map.Entry<?, ?> entry : data.entrySet()) {
            result.put(String.valueOf(entry.getKey()), getAttributeValue(entry.getValue()));
        }
        return result;
    }

    private static AttributeValue getAttributeValue(final Object v) {
        if (v == null) {
            return AttributeValue.builder().nul(true).build();
        }
        if (v instanceof String) {
            return AttributeValue.builder().s((String) v).build();
        } else if (v instanceof Number) {
            final Number n = (Number) v;
            return AttributeValue.builder().n(String.valueOf(n)).build();
        } else if (v instanceof Boolean) {
            return AttributeValue.builder().bool((Boolean) v).build();
        } else if (v instanceof Map) {
            return AttributeValue.builder().m(getItemForMap((Map) v)).build();
        }
        return AttributeValue.builder().s(String.valueOf(v)).build();
    }

    private static UserEvent getUserEvent(final Map<String, AttributeValue> item) {
        final UserEvent result = new UserEvent()
                .setUserId(Long.parseLong(item.get(FIELD_USER_ID).n()))
                .setTimestamp(Long.parseLong(item.get(FIELD_TIMESTAMP).n()))
                .setEventType(item.get(FIELD_EVENT_TYPE).s());
        if (item.containsKey(FIELD_EVENT_DATA)
                && item.get(FIELD_EVENT_DATA).m() != null
                && !item.get(FIELD_EVENT_DATA).m().isEmpty()) {
            result.setEventData(getData(item.get(FIELD_EVENT_DATA).m()));
        }
        return result;
    }

    private static Map<String, Object> getData(final Map<String, AttributeValue> item) {
        Map<String, Object> result = new HashMap<>();
        for (final Map.Entry<String, AttributeValue> entry : item.entrySet()) {
            result.put(String.valueOf(entry.getKey()), getValue(entry.getValue()));
        }
        return result;
    }

    private static Object getValue(final AttributeValue v) {
        if (v.s() != null) {
            return v.s();
        } else if (v.n() != null) {
            return Long.parseLong(v.n());
        } else if (v.bool() != null) {
            return v.bool();
        } else if (v.m() != null && !v.m().isEmpty()) {
            return getData(v.m());
        }
        return null;
    }
}
