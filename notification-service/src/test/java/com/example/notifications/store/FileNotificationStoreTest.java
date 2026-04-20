package com.example.notifications.store;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.notifications.api.NotificationChannel;
import com.example.notifications.api.NotificationRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileNotificationStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void appendsNotificationAsJsonLine() throws Exception {
        Path logFile = tempDir.resolve("notifications.log");
        FileNotificationStore store = new FileNotificationStore(logFile.toString(), new ObjectMapper().findAndRegisterModules());

        NotificationRecord record = new NotificationRecord(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "alice@example.com",
            NotificationChannel.EMAIL,
            "hello",
            Instant.parse("2026-04-18T10:15:30Z"));

        store.append(record);

        String logContents = Files.readString(logFile);
        assertTrue(logContents.contains("\"recipient\":\"alice@example.com\""));
        assertTrue(logContents.contains("\"status\"") == false);
        assertTrue(logContents.contains("\"channel\":\"EMAIL\""));
    }
}
