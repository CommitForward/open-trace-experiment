package com.example.notifications.api;

import com.example.notifications.store.FileNotificationStore;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    private static final long RESPONSE_DELAY_MILLIS = 10_000L;
    private static final int DELAY_PERCENTAGE = 20;

    private final FileNotificationStore notificationStore;

    public NotificationResource(FileNotificationStore notificationStore) {
        this.notificationStore = notificationStore;
    }

    @POST
    @Path("/send")
    public Response sendNotification(@Valid SendNotificationRequest request) {
        NotificationRecord record = new NotificationRecord(
            UUID.randomUUID(),
            request.getRecipient(),
            request.getChannel(),
            request.getMessage(),
            Instant.now());

        try {
            notificationStore.append(record);
        } catch (IOException exception) {
            throw new ServerErrorException("Unable to record notification request", Response.Status.INTERNAL_SERVER_ERROR, exception);
        }

        if (shouldDelayResponse()) {
            try {
                Thread.sleep(RESPONSE_DELAY_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ServerErrorException("Notification response delay was interrupted", Response.Status.INTERNAL_SERVER_ERROR, exception);
            }
        }

        SendNotificationResponse response = new SendNotificationResponse(record.getId(), "RECORDED");
        return Response.accepted(response)
            .location(URI.create("/notifications/" + record.getId()))
            .build();
    }

    private boolean shouldDelayResponse() {
        return ThreadLocalRandom.current().nextInt(100) < DELAY_PERCENTAGE;
    }
}
