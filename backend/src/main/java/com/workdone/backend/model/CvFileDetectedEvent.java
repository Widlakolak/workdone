package com.workdone.backend.model;

import java.nio.file.Path;
import java.time.Instant;

public record CvFileDetectedEvent(
        Path filePath,
        Instant detectedAt,
        Instant fileModifiedAt
) {}