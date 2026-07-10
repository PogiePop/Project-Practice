package com.audit.controller;

import com.audit.common.Result;
import com.audit.mapper.TemplateMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit/v1")
public class TemplateController {

    private final TemplateMapper mapper;

    public TemplateController(TemplateMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/templates")
    public Result<Map<String, Object>> list(@RequestParam(required = false) Integer planType,
                                             @RequestParam(required = false) String keyword) {
        return Result.ok(Map.of("list", mapper.findList(planType, keyword)));
    }

    @GetMapping("/templates/{templateId}/content")
    public Result<Map<String, Object>> getContent(@PathVariable String templateId) {
        return Result.ok(mapper.findById(templateId));
    }

    @GetMapping("/plan/batches/{batchId}/templates")
    public Result<Map<String, Object>> getBatchTemplates(@PathVariable String batchId) {
        return Result.ok(Map.of("list", mapper.findByBatchId(batchId)));
    }

    @PostMapping("/plan/batches/{batchId}/templates")
    public Result<Void> bindTemplates(@PathVariable String batchId, @RequestBody Map<String, List<String>> body) {
        List<String> templateIds = body.get("templateIds");
        if (templateIds != null) {
            for (String tid : templateIds) {
                mapper.bindToBatch(batchId, tid);
            }
        }
        return Result.ok();
    }

    @DeleteMapping("/plan/batches/{batchId}/templates/{templateId}")
    public Result<Void> unbindTemplate(@PathVariable String batchId, @PathVariable String templateId) {
        mapper.unbindFromBatch(batchId, templateId);
        return Result.ok();
    }
}
