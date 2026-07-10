package com.audit.controller;

import com.audit.common.Result;
import com.audit.mapper.SettingsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/audit/v1/settings")
public class SettingsController {

    private final SettingsMapper mapper;
    private final ObjectMapper objectMapper;

    public SettingsController(SettingsMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        String userId = "admin";
        List<Map<String, String>> rows = mapper.findByUserId(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            result.put(row.get("setting_key"), row.get("setting_value"));
        }
        return Result.ok(result);
    }

    @PostMapping("/notify")
    public Result<Void> saveNotify(@RequestBody String body) {
        mapper.save("admin", "notify", body);
        return Result.ok();
    }

    @PostMapping("/ui")
    public Result<Void> saveUI(@RequestBody String body) {
        mapper.save("admin", "ui", body);
        return Result.ok();
    }

    @PostMapping
    public Result<Void> saveAll(@RequestBody Map<String, Object> body) {
        try {
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                String json = objectMapper.writeValueAsString(entry.getValue());
                mapper.save("admin", entry.getKey(), json);
            }
        } catch (Exception e) {
            return Result.fail(500, "保存失败: " + e.getMessage());
        }
        return Result.ok();
    }
}
