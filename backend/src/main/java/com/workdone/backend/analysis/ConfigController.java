package com.workdone.backend.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Kontroler do dynamicznej zmiany filtrów wyszukiwania ofert.
 * Docelowo używany przez front-end w Angularze.
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class ConfigController {

    private final DynamicConfigService dynamicConfigService;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "semanticThreshold", dynamicConfigService.getSemanticThreshold(),
                "instantThreshold", dynamicConfigService.getInstantThreshold(),
                "digestThreshold", dynamicConfigService.getDigestThreshold(),
                "archiveThreshold", dynamicConfigService.getArchiveThreshold(),
                "mustHaveKeywords", dynamicConfigService.getMustHaveKeywords(),
                "preferredLocation", dynamicConfigService.getPreferredLocation(),
                "allowRemoteSearch", dynamicConfigService.isAllowRemoteSearch(),
                "preferredSeniority", dynamicConfigService.getPreferredSeniority()
        );
    }

    @PostMapping("/update")
    public String update(@RequestParam String param, @RequestParam String value) {
        return dynamicConfigService.updateConfig(param, value);
    }
}
