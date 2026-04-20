package com.example.notifications.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SendNotificationRequest {

    @NotBlank
    private String recipient;

    @NotNull
    private NotificationChannel channel;

    @NotBlank
    private String message;

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
}
