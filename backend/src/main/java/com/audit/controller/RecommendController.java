package com.audit.controller;

import com.audit.common.Result;
import com.audit.entity.AuditLeader;
import com.audit.entity.AuditUnit;
import com.audit.entity.PlanBatch;
import com.audit.mapper.AuditLeaderMapper;
import com.audit.mapper.AuditUnitMapper;
import com.audit.mapper.PlanBatchMapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/audit/v1")
public class RecommendController {

    private final AuditUnitMapper unitMapper;
    private final AuditLeaderMapper leaderMapper;
    private final PlanBatchMapper batchMapper;

    public RecommendController(AuditUnitMapper unitMapper, AuditLeaderMapper leaderMapper,
                                PlanBatchMapper batchMapper) {
        this.unitMapper = unitMapper;
        this.leaderMapper = leaderMapper;
        this.batchMapper = batchMapper;
    }

    @GetMapping("/plan/recommend/objects")
    public Result<Map<String, Object>> getRecommendObjects(@RequestParam(required = false) Integer planType) {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 推荐被审计单位（最近3年未审计的）
        List<AuditUnit> units = unitMapper.findList(null, null);
        for (AuditUnit u : units) {
            if (u.getLatestAuditDate() != null && ChronoUnit.YEARS.between(u.getLatestAuditDate(), today) < 3) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("objectId", u.getUnitId());
            item.put("objectName", u.getUnitName());
            item.put("categoryName", u.getCategoryName() != null ? u.getCategoryName() : "未知");
            item.put("recommendReason", u.getLatestAuditDate() == null ? "从未审计" :
                    "距上轮审计已超" + ChronoUnit.YEARS.between(u.getLatestAuditDate(), today) + "年");
            item.put("lastAuditDate", u.getLatestAuditDate());
            int rectCount = u.getPendingRectifyCount() != null ? u.getPendingRectifyCount() : 0;
            item.put("riskLevel", rectCount > 2 ? "HIGH" : rectCount > 0 ? "MEDIUM" : "LOW");
            item.put("recommendScore", u.getFundScale() != null ? u.getFundScale().intValue() / 1000 + 50 : 60);
            list.add(item);
        }

        // 推荐领导干部（任职满3年未审计的）
        List<AuditLeader> leaders = leaderMapper.findList(null, 1);
        for (AuditLeader l : leaders) {
            if (l.getLatestAuditDate() != null && ChronoUnit.YEARS.between(l.getLatestAuditDate(), today) < 3) continue;
            BigDecimal tenure = l.getTenureYears() != null ? l.getTenureYears() : BigDecimal.ZERO;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("objectId", l.getLeaderId());
            item.put("objectName", l.getLeaderName());
            item.put("categoryName", "领导干部");
            item.put("recommendReason", l.getLatestAuditDate() == null ?
                    "任职" + tenure + "年尚未审计" :
                    "距上轮审计已超" + ChronoUnit.YEARS.between(l.getLatestAuditDate(), today) + "年");
            item.put("lastAuditDate", l.getLatestAuditDate());
            item.put("riskLevel", tenure.compareTo(new BigDecimal("5")) >= 0 ? "HIGH" : "MEDIUM");
            item.put("recommendScore", tenure.intValue() * 15 + 30);
            list.add(item);
        }

        // 按推荐分降序
        list.sort((a, b) -> Integer.compare((int) b.get("recommendScore"), (int) a.get("recommendScore")));
        return Result.ok(Map.of("list", list));
    }

    @PostMapping("/plan/batches/{batchId}/projects/recommend-import")
    public Result<Void> importToBatch(@PathVariable String batchId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> objectIds = (List<String>) body.get("objectIds");
        PlanBatch batch = batchMapper.findByBatchId(batchId);
        if (batch != null && objectIds != null) {
            batch.setProjectCount((batch.getProjectCount() != null ? batch.getProjectCount() : 0) + objectIds.size());
            batchMapper.updateProjectCount(batchId, batch.getProjectCount());
        }
        return Result.ok();
    }
}
