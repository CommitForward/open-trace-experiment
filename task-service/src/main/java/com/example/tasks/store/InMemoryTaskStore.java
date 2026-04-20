package com.example.tasks.store;

import com.example.tasks.api.Task;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryTaskStore {

    private final ConcurrentMap<UUID, Task> tasks = new ConcurrentHashMap<>();

    public Task create(String title, String description) {
        Task task = new Task(UUID.randomUUID(), title, description, Instant.now());
        tasks.put(task.getId(), task);
        return task;
    }

    public Task get(UUID id) {
        return tasks.get(id);
    }

    public Task delete(UUID id) {
        return tasks.remove(id);
    }
}
