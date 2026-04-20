package com.example.tasks.store;

import com.example.tasks.api.Task;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InMemoryTaskStoreTest {

    @Test
    void shouldCreateAndFetchTask() {
        InMemoryTaskStore store = new InMemoryTaskStore();

        Task created = store.create("write docs", "prepare sample");
        Task loaded = store.get(created.getId());

        assertNotNull(loaded);
        assertEquals("write docs", loaded.getTitle());
        assertEquals("prepare sample", loaded.getDescription());
    }

    @Test
    void shouldDeleteTask() {
        InMemoryTaskStore store = new InMemoryTaskStore();

        Task created = store.create("write docs", "prepare sample");
        Task deleted = store.delete(created.getId());

        assertNotNull(deleted);
        assertNull(store.get(created.getId()));
    }
}
