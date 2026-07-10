package com.audit.controller;

import com.audit.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit/v1")
public class DictController {

    @GetMapping("/dict/all")
    public Result<Map<String, Object>> getAllDict() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("planType", List.of(
                Map.of("value", 0, "label", "经济责任审计"),
                Map.of("value", 1, "label", "财务收支审计"),
                Map.of("value", 2, "label", "专项审计"),
                Map.of("value", 3, "label", "工程审计")));
        dict.put("approvalStatus", List.of(
                Map.of("value", 0, "label", "已审批"),
                Map.of("value", 1, "label", "审批中"),
                Map.of("value", 2, "label", "已归档"),
                Map.of("value", 3, "label", "草稿")));
        dict.put("unitCategory", List.of(
                Map.of("value", 0, "label", "校内职能部门"),
                Map.of("value", 1, "label", "二级学院"),
                Map.of("value", 2, "label", "直属后勤单位"),
                Map.of("value", 3, "label", "校办企业"),
                Map.of("value", 4, "label", "基建项目部"),
                Map.of("value", 5, "label", "附属医院")));
        return Result.ok(dict);
    }

    @GetMapping("/users/auditors")
    public Result<List<Map<String, Object>>> getAuditors() {
        return Result.ok(List.of(
                Map.of("userId", "U001", "userName", "张三", "role", "AUDIT_LEADER", "availableCapacity", 2),
                Map.of("userId", "U002", "userName", "赵六", "role", "AUDIT_LEADER", "availableCapacity", 1),
                Map.of("userId", "U003", "userName", "李四", "role", "AUDIT_LEADER", "availableCapacity", 3),
                Map.of("userId", "U004", "userName", "王五", "role", "AUDIT_LEADER", "availableCapacity", 4)
        ));
    }
}
