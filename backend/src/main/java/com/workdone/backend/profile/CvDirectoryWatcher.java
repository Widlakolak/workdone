package com.workdone.backend.profile;

import com.workdone.backend.config.WorkDoneProperties;
import com.workdone.backend.profile.event.CvFileDetectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;

import static java.nio.file.StandardWatchEventKinds.*;

@Component
public class CvDirectoryWatcher {

    private static final Logger log = LoggerFactory.getLogger(CvDirectoryWatcher.class);

    private final ApplicationEventPublisher publisher;
    private final Path folder;

    public CvDirectoryWatcher(ApplicationEventPublisher publisher,
                              WorkDoneProperties properties) {
        this.publisher = publisher;
        this.folder = Path.of(properties.profile().inputDirectory());
    }

    @PostConstruct
    public void start() {
        Thread.ofVirtual().start(this::watchLoop);
    }

    private void watchLoop() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

            if (Files.notExists(folder)) {
                Files.createDirectories(folder);
            }

            folder.register(watchService, ENTRY_CREATE);

            log.info("CV Watcher started for: {}", folder);

            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW) continue;

                    Path filename = (Path) event.context();
                    Path fullPath = folder.resolve(filename);
                    Instant detectedAt = Instant.now();
                    Instant modifiedAt = Instant.ofEpochMilli(fullPath.toFile().lastModified());

                    log.info("New CV file detected: {}", fullPath);

                    publisher.publishEvent(
                            new CvFileDetectedEvent(fullPath, detectedAt, modifiedAt)
                    );
                }

                key.reset();
            }

        } catch (IOException | InterruptedException e) {
            log.error("CV watcher crashed", e);
        }
    }
}