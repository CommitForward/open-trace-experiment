package com.example.tasks.api;

import com.example.tasks.notifications.NotificationServiceClient;
import com.example.tasks.store.InMemoryTaskStore;
import java.net.URI;
import java.util.UUID;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

    private final InMemoryTaskStore taskStore;
    private final NotificationServiceClient notificationServiceClient;

    public TaskResource(InMemoryTaskStore taskStore, NotificationServiceClient notificationServiceClient) {
        this.taskStore = taskStore;
        this.notificationServiceClient = notificationServiceClient;
    }

    @POST
    public Response createTask(@Valid CreateTaskRequest request) {
        Task task = taskStore.create(request.getTitle(), request.getDescription());
        try {
            notificationServiceClient.sendTaskCreatedNotification(task);
        } catch (RuntimeException exception) {
            taskStore.delete(task.getId());
            throw exception;
        }
        return Response.created(URI.create("/tasks/" + task.getId()))
            .entity(task)
            .build();
    }

    @GET
    @Path("/{id}")
    public Task getTask(@PathParam("id") UUID id) {
        Task task = taskStore.get(id);
        if (task == null) {
            throw new NotFoundException("Task not found for id " + id);
        }
        return task;
    }
}
