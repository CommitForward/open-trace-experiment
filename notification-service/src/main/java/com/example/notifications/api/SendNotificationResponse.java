package com.example.notifications.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class SendNotificationResponse {

    private UUID notificationId;
    private String status;

    public SendNotificationResponse() {
    }

    public SendNotificationResponse(UUID notificationId, String status) {
        this.notificationId = notificationId;
        this.status = status;
    }

    @JsonProperty
    public UUID getNotificationId() {
        return notificationId;
    }

    @JsonProperty
    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    @JsonProperty
    public String getStatus() {
        return status;
    }

    @JsonProperty
    public void setStatus(String status) {
        this.status = status;
    }
}
