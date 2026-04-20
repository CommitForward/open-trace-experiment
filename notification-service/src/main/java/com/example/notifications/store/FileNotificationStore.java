package com.example.notifications.store;

import com.example.notifications.api.NotificationRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileNotificationStore {

    private final Path logPath;
    private final ObjectMapper objectMapper;

    public FileNotificationStore(String logPath, ObjectMapper objectMapper) {
        this.logPath = Path.of(logPath);
        this.objectMapper = objectMapper;
    }

    public synchronized void append(NotificationRecord record) throws IOException {
        Path parent = logPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String line = objectMapper.writeValueAsString(record) + System.lineSeparator();
        Files.writeString(
            logPath,
            line,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND);
    }
}
