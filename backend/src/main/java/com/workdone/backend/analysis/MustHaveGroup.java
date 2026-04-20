package com.workdone.backend.analysis;

import java.util.List;

public record MustHaveGroup(
        String name,
        List<String> keywords,
        boolean required
) {}