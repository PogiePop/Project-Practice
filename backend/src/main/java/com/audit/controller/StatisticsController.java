package com.audit.controller;

import com.audit.common.Result;
import com.audit.entity.PlanBatch;
import com.audit.mapper.PlanBatchMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/audit/v1/statistics")
public class StatisticsController {

    private final PlanBatchMapper planBatchMapper;

    public StatisticsController(PlanBatchMapper planBatchMapper) {
        this.planBatchMapper = planBatchMapper;
    }

    /**
     * 获取年度进度总览
     * 返回: summaryCards + byPlanType 分布 + byMonth 月度趋势
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(@RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        List<PlanBatch> all = planBatchMapper.findList(null, null, year, null);

        int totalProjects = all.stream().mapToInt(b -> b.getProjectCount() != null ? b.getProjectCount() : 0).sum();
        int completed = 0, inProgress = 0, notStarted = 0, overdue = 0;
        LocalDate today = LocalDate.now();

        for (PlanBatch b : all) {
            int cnt = b.getProjectCount() != null ? b.getProjectCount() : 0;
            int prog = b.getProgress() != null ? b.getProgress() : 0;
            if (prog >= 100) {
                completed += cnt;
            } else if (prog > 0) {
                inProgress += cnt;
                if (b.getEndDate() != null && b.getEndDate().isBefore(today)) overdue += cnt;
            } else {
                notStarted += cnt;
                if (b.getEndDate() != null && b.getEndDate().isBefore(today)) overdue += cnt;
            }
        }

        Map<String, Object> summaryCards = new LinkedHashMap<>();
        summaryCards.put("totalProjects", totalProjects);
        summaryCards.put("completedProjects", completed);
        summaryCards.put("inProgressProjects", inProgress);
        summaryCards.put("notStartedProjects", notStarted);
        summaryCards.put("overdueProjects", overdue);

        // 按计划类型分布
        String[] typeNames = {"经济责任审计", "财务收支审计", "专项审计", "工程审计"};
        Map<Integer, Map<String, Integer>> typeMap = new LinkedHashMap<>();
        for (PlanBatch b : all) {
            int t = b.getPlanType() != null && b.getPlanType() < 4 ? b.getPlanType() : 0;
            typeMap.putIfAbsent(t, new LinkedHashMap<>());
            Map<String, Integer> m = typeMap.get(t);
            int cnt = b.getProjectCount() != null ? b.getProjectCount() : 0;
            int prog = b.getProgress() != null ? b.getProgress() : 0;
            m.merge("total", cnt, Integer::sum);
            if (prog >= 100) m.merge("completed", cnt, Integer::sum);
            else if (prog > 0) m.merge("inProgress", cnt, Integer::sum);
            if (b.getEndDate() != null && b.getEndDate().isBefore(today) && prog < 100)
                m.merge("overdue", cnt, Integer::sum);
        }

        List<Map<String, Object>> byPlanType = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("planType", i);
            item.put("planTypeName", typeNames[i]);
            Map<String, Integer> m = typeMap.getOrDefault(i, Collections.emptyMap());
            item.put("total", m.getOrDefault("total", 0));
            item.put("completed", m.getOrDefault("completed", 0));
            item.put("inProgress", m.getOrDefault("inProgress", 0));
            item.put("overdue", m.getOrDefault("overdue", 0));
            byPlanType.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summaryCards", summaryCards);
        data.put("byPlanType", byPlanType);
        return Result.ok(data);
    }

    /**
     * 获取甘特图数据（按计划类型分组）
     */
    @GetMapping("/gantt")
    public Result<Map<String, Object>> getGantt(@RequestParam(required = false) Integer year,
                                                 @RequestParam(defaultValue = "planType") String groupBy) {
        if (year == null) year = LocalDate.now().getYear();
        List<PlanBatch> all = planBatchMapper.findList(null, null, year, null);

        String[] typeNames = {"经济责任审计", "财务收支审计", "专项审计", "工程审计"};
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();

        for (PlanBatch b : all) {
            String key = typeNames[b.getPlanType() != null && b.getPlanType() < 4 ? b.getPlanType() : 0];
            groups.putIfAbsent(key, new ArrayList<>());

            String alertLevel = "NORMAL";
            if (b.getEndDate() != null && b.getEndDate().isBefore(LocalDate.now()) && (b.getProgress() == null || b.getProgress() < 100)) {
                alertLevel = (b.getProgress() == null || b.getProgress() == 0) ? "CRITICAL" : "WARNING";
            }

            Map<String, Object> proj = new LinkedHashMap<>();
            proj.put("projectName", b.getBatchName());
            proj.put("startDate", b.getStartDate() != null ? b.getStartDate().toString() : "");
            proj.put("endDate", b.getEndDate() != null ? b.getEndDate().toString() : "");
            proj.put("progress", b.getProgress() != null ? b.getProgress() : 0);
            proj.put("status", b.getApprovalStatus() != null ? b.getApprovalStatus() : 0);
            proj.put("alertLevel", alertLevel);
            proj.put("auditLeader", b.getAuditLeader() != null ? b.getAuditLeader() : "");
            groups.get(key).add(proj);
        }

        List<Map<String, Object>> groupList = new ArrayList<>();
        groups.forEach((name, projects) -> {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("groupName", name);
            g.put("projects", projects);
            groupList.add(g);
        });

        return Result.ok(Map.of("groups", groupList));
    }

    /**
     * 获取审计人员负载分布
     */
    @GetMapping("/workload")
    public Result<Map<String, Object>> getWorkload(@RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        List<PlanBatch> all = planBatchMapper.findList(null, null, year, null);

        // 按组长聚合
        Map<String, Map<String, Object>> leaderMap = new LinkedHashMap<>();
        for (PlanBatch b : all) {
            String leader = b.getAuditLeader() != null && !b.getAuditLeader().isEmpty() ? b.getAuditLeader() : "未分配";
            leaderMap.putIfAbsent(leader, new LinkedHashMap<>());
            Map<String, Object> m = leaderMap.get(leader);
            m.putIfAbsent("userName", leader);
            m.putIfAbsent("role", "审计组长");
            m.putIfAbsent("currentProjects", 0);
            m.put("currentProjects", (int) m.get("currentProjects") + 1);
        }

        List<Map<String, Object>> auditLeaders = new ArrayList<>();
        int idx = 1;
        for (Map.Entry<String, Map<String, Object>> e : leaderMap.entrySet()) {
            Map<String, Object> item = e.getValue();
            item.put("userId", "U" + String.format("%03d", idx++));
            item.put("maxCapacity", 6);
            int cp = (int) item.get("currentProjects");
            item.put("workloadPercent", Math.round(cp * 100.0 / 6));
            item.put("overload", cp > 6);
            auditLeaders.add(item);
        }

        return Result.ok(Map.of("auditLeaders", auditLeaders));
    }

    /**
     * 获取负载历史趋势
     */
    @GetMapping("/workload-trend")
    public Result<Map<String, Object>> getWorkloadTrend(@RequestParam String userId,
                                                         @RequestParam(defaultValue = "12") int months) {
        return Result.ok(Map.of("userId", userId, "trend", List.of()));
    }

    /**
     * 获取进度预警项目列表
     */
    @GetMapping("/alerts")
    public Result<Map<String, Object>> getAlerts(@RequestParam(required = false) Integer year,
                                                  @RequestParam(required = false) Integer alertType) {
        if (year == null) year = LocalDate.now().getYear();
        List<PlanBatch> all = planBatchMapper.findList(null, null, year, null);
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> list = new ArrayList<>();
        for (PlanBatch b : all) {
            int prog = b.getProgress() != null ? b.getProgress() : 0;
            if (prog >= 100) continue; // 已完成的跳过

            if (b.getEndDate() != null && b.getEndDate().isBefore(today)) {
                long days = ChronoUnit.DAYS.between(b.getEndDate(), today);
                int at = prog == 0 ? 0 : 1; // 0=超期未启动, 1=实施滞后
                if (alertType != null && at != alertType) continue;

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("projectId", b.getBatchId());
                item.put("projectName", b.getBatchName());
                item.put("batchName", b.getBatchName());
                item.put("alertType", at);
                item.put("alertTypeName", at == 0 ? "超期未启动" : "实施滞后");
                item.put("daysOverdue", (int) days);
                item.put("auditLeader", b.getAuditLeader() != null ? b.getAuditLeader() : "未分配");
                item.put("notified", false);
                list.add(item);
            }
        }

        return Result.ok(Map.of("list", list));
    }

    /**
     * 手动推送预警消息
     */
    @PostMapping("/alerts/push")
    public Result<Void> pushAlerts(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> projectIds = (List<String>) body.get("projectIds");
        // 实际推送逻辑可在此扩展（记录通知日志、WebSocket推送等）
        System.out.println("预警推送 -> projectIds: " + projectIds);
        return Result.ok();
    }
}
