package com.workdone.backend.profile;

import com.workdone.backend.profile.service.CvAggregationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final CvAggregationService aggregationService;

    public ProfileController(CvAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @PostMapping("/rebuild")
    public String rebuildProfile() {
        return aggregationService.buildMergedProfile();
    }
}