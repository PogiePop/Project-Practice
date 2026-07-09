package com.audit.controller;

import cn.hutool.core.util.IdUtil;
import com.audit.common.Result;
import com.audit.entity.AuditLeader;
import com.audit.entity.AuditUnit;
import com.audit.mapper.CareerHistoryMapper;
import com.audit.service.AuditLeaderService;
import com.audit.service.AuditUnitService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/audit/v1")
public class AuditObjectController {

    private final AuditUnitService unitService;
    private final AuditLeaderService leaderService;
    private final CareerHistoryMapper careerHistoryMapper;
    private final com.audit.mapper.RectifyMapper rectifyMapper;
    private final com.audit.mapper.PlanBatchMapper batchMapper;

    public AuditObjectController(AuditUnitService unitService, AuditLeaderService leaderService,
                                  CareerHistoryMapper careerHistoryMapper,
                                  com.audit.mapper.RectifyMapper rectifyMapper,
                                  com.audit.mapper.PlanBatchMapper batchMapper) {
        this.unitService = unitService;
        this.leaderService = leaderService;
        this.careerHistoryMapper = careerHistoryMapper;
        this.rectifyMapper = rectifyMapper;
        this.batchMapper = batchMapper;
    }

    // ============ 被审计单位 ============

    @GetMapping("/objects/units")
    public Result<?> getUnits(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer category,
                               @RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "20") int pageSize) {
        var page = unitService.getList(keyword, category, pageNum, pageSize);
        for (AuditUnit u : page.getList()) {
            List<String> leaders = unitService.getLeadersByUnitName(u.getUnitName());
            u.setLeaderNames(leaders.isEmpty() ? "" : String.join("、", leaders));
        }
        return Result.ok(page);
    }

    @GetMapping("/objects/units/{unitId}")
    public Result<AuditUnit> getUnit(@PathVariable String unitId) {
        AuditUnit unit = unitService.getByUnitId(unitId);
        if (unit != null) {
            // 动态查询领导电话
            if (unit.getLeaderInCharge() != null) {
                var leaders = leaderService.getList(null, 1, 1, 100);
                for (var l : leaders.getList()) {
                    if (unit.getLeaderInCharge().equals(l.getLeaderName())) {
                        unit.setLeaderInChargePhone(l.getPhone());
                        break;
                    }
                }
            }
            // 动态查询财务联系人电话
            if (unit.getFinanceContact() != null) {
                var leaders = leaderService.getList(null, 1, 1, 100);
                for (var l : leaders.getList()) {
                    if (unit.getFinanceContact().equals(l.getLeaderName())) {
                        unit.setFinanceContactPhone(l.getPhone());
                        break;
                    }
                }
            }
        }
        return Result.ok(unit);
    }

    @PostMapping("/objects/units")
    public Result<AuditUnit> createUnit(@RequestBody AuditUnit unit) {
        return Result.ok(unitService.create(unit));
    }

    @PutMapping("/objects/units/{unitId}")
    public Result<Void> updateUnit(@PathVariable String unitId, @RequestBody AuditUnit unit) {
        unitService.update(unitId, unit);
        return Result.ok();
    }

    @DeleteMapping("/objects/units/{unitId}")
    public Result<Void> deleteUnit(@PathVariable String unitId) {
        unitService.delete(unitId);
        return Result.ok();
    }

    @GetMapping("/objects/units/{unitId}/audit-records")
    public Result<Map<String, Object>> getUnitAuditRecords(@PathVariable String unitId) {
        java.util.List<Map<String, Object>> records = new java.util.ArrayList<>();
        for (var b : batchMapper.findList(null, null, null, null)) {
            if (unitId.equals(b.getUnitId())) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("projectName", b.getBatchName());
                r.put("date", b.getEndDate() != null ? b.getEndDate().toString() : "");
                r.put("type", new String[]{"经济责任审计","财务收支审计","专项审计","工程审计"}[b.getPlanType() != null && b.getPlanType() < 4 ? b.getPlanType() : 0]);
                r.put("issueCount", rectifyMapper.countPendingByUnitId(unitId));
                r.put("status", b.getProgress() != null && b.getProgress() >= 100 ? "已完成" : "进行中");
                records.add(r);
            }
        }
        return Result.ok(Map.of("list", records));
    }

    @GetMapping("/objects/units/{unitId}/rectify-ledger")
    public Result<Map<String, Object>> getUnitRectifyLedger(@PathVariable String unitId) {
        return Result.ok(Map.of("list", rectifyMapper.findByUnitId(unitId)));
    }

    @GetMapping("/objects/units/{unitId}/special-funds")
    public Result<Map<String, Object>> getUnitSpecialFunds(@PathVariable String unitId) {
        return Result.ok(Map.of("list", List.of()));
    }

    // ============ 经责领导干部 ============

    @GetMapping("/objects/leaders")
    public Result<?> getLeaders(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Integer isActive,
                                 @RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(leaderService.getList(keyword, isActive, pageNum, pageSize));
    }

    @GetMapping("/objects/leaders/recommend-for-audit")
    public Result<Map<String, Object>> recommendLeaders() {
        List<AuditLeader> leaders = leaderService.getRecommendList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (AuditLeader l : leaders) {
            Map<String, Object> item = new HashMap<>();
            item.put("leaderId", l.getLeaderId());
            item.put("leaderName", l.getLeaderName());
            item.put("unitName", l.getCurrentUnitName());
            item.put("position", l.getCurrentPosition());
            item.put("tenureYears", l.getTenureYears());
            item.put("lastAuditDate", l.getLatestAuditDate());
            item.put("reason", "任职" + l.getTenureYears() + "年，建议纳入审计计划");
            item.put("priority", l.getTenureYears().compareTo(new BigDecimal("5")) >= 0 ? "HIGH" : "MEDIUM");
            list.add(item);
        }
        return Result.ok(Map.of("list", list));
    }

    @GetMapping("/objects/leaders/{leaderId}")
    public Result<AuditLeader> getLeader(@PathVariable String leaderId) {
        return Result.ok(leaderService.getByLeaderId(leaderId));
    }

    @PostMapping("/objects/leaders")
    public Result<AuditLeader> createLeader(@RequestBody AuditLeader leader) {
        return Result.ok(leaderService.create(leader));
    }

    @PutMapping("/objects/leaders/{leaderId}")
    public Result<Void> updateLeader(@PathVariable String leaderId, @RequestBody AuditLeader leader) {
        leaderService.update(leaderId, leader);
        return Result.ok();
    }

    @DeleteMapping("/objects/leaders/{leaderId}")
    public Result<Void> deleteLeader(@PathVariable String leaderId) {
        leaderService.delete(leaderId);
        return Result.ok();
    }

    // ============ 任职履历 CRUD ============

    @GetMapping("/objects/leaders/{leaderId}/career-history")
    public Result<Map<String, Object>> getCareerHistory(@PathVariable String leaderId) {
        return Result.ok(Map.of("list", careerHistoryMapper.findByLeaderId(leaderId)));
    }

    private String nullIfEmpty(String s) { return s == null || s.isEmpty() ? null : s; }

    @PostMapping("/objects/leaders/{leaderId}/career-history")
    public Result<Map<String, Object>> addCareerRecord(@PathVariable String leaderId, @RequestBody Map<String, Object> body) {
        String recordId = "CH" + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
        careerHistoryMapper.insert(recordId, leaderId,
                (String) body.getOrDefault("unitName", ""),
                (String) body.getOrDefault("position", ""),
                nullIfEmpty((String) body.get("startDate")),
                nullIfEmpty((String) body.get("endDate")),
                (String) body.getOrDefault("dutyDescription", ""),
                body.get("fundScope") != null ? new BigDecimal(body.get("fundScope").toString()) : BigDecimal.ZERO);
        return Result.ok(Map.of("recordId", recordId));
    }

    @PutMapping("/objects/leaders/{leaderId}/career-history/{recordId}")
    public Result<Void> updateCareerRecord(@PathVariable String leaderId, @PathVariable String recordId, @RequestBody Map<String, Object> body) {
        careerHistoryMapper.update(recordId, leaderId,
                (String) body.getOrDefault("unitName", ""),
                (String) body.getOrDefault("position", ""),
                nullIfEmpty((String) body.get("startDate")),
                nullIfEmpty((String) body.get("endDate")),
                (String) body.getOrDefault("dutyDescription", ""),
                body.get("fundScope") != null ? new BigDecimal(body.get("fundScope").toString()) : BigDecimal.ZERO);
        return Result.ok();
    }

    @DeleteMapping("/objects/leaders/{leaderId}/career-history/{recordId}")
    public Result<Void> deleteCareerRecord(@PathVariable String leaderId, @PathVariable String recordId) {
        careerHistoryMapper.delete(recordId, leaderId);
        return Result.ok();
    }

    @GetMapping("/objects/leaders/{leaderId}/audit-projects")
    public Result<Map<String, Object>> getLeaderAuditProjects(@PathVariable String leaderId) {
        AuditLeader leader = leaderService.getByLeaderId(leaderId);
        String unitName = leader != null ? leader.getCurrentUnitName() : null;
        return Result.ok(Map.of("list", batchMapper.findProjectsByLeader(leaderId, unitName)));
    }
}
