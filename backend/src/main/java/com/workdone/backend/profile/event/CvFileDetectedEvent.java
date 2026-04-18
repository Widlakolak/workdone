package com.workdone.backend.profile.event;

import java.nio.file.Path;
import java.time.Instant;

public record CvFileDetectedEvent(
        Path file,
        Instant detectedAt,
        Instant modifiedAt
) {}