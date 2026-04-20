package com.example.notifications.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class NotificationRecord {

    private UUID id;
    private String recipient;
    private NotificationChannel channel;
    private String message;
    private Instant receivedAt;

    public NotificationRecord() {
    }

    public NotificationRecord(UUID id, String recipient, NotificationChannel channel, String message, Instant receivedAt) {
        this.id = id;
        this.recipient = recipient;
        this.channel = channel;
        this.message = message;
        this.receivedAt = receivedAt;
    }

    @JsonProperty
    public UUID getId() {
        return id;
    }

    @JsonProperty
    public void setId(UUID id) {
        this.id = id;
    }

    @JsonProperty
    public String getRecipient() {
        return recipient;
    }

    @JsonProperty
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    @JsonProperty
    public NotificationChannel getChannel() {
        return channel;
    }

    @JsonProperty
    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    @JsonProperty
    public String getMessage() {
        return message;
    }

    @JsonProperty
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty
    public Instant getReceivedAt() {
        return receivedAt;
    }

    @JsonProperty
    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}
