package com.audit.controller;

import cn.hutool.core.util.IdUtil;
import com.audit.common.Result;
import com.audit.entity.PlanBatch;
import com.audit.mapper.ApprovalMapper;
import com.audit.mapper.AttachmentMapper;
import com.audit.mapper.AuditUnitMapper;
import com.audit.mapper.AuditLeaderMapper;
import com.audit.mapper.PlanBatchMapper;
import com.audit.mapper.RectifyMapper;
import com.audit.mapper.UserMapper;
import com.audit.service.PlanBatchService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/audit/v1")
public class PlanBatchController {

    private final PlanBatchService service;
    private final ApprovalMapper approvalMapper;
    private final PlanBatchMapper batchMapper;
    private final UserMapper userMapper;
    private final AttachmentMapper attachmentMapper;
    private final AuditUnitMapper unitMapper;
    private final AuditLeaderMapper leaderMapper;
    private final RectifyMapper rectifyMapper;

    public PlanBatchController(PlanBatchService service, ApprovalMapper approvalMapper,
                                PlanBatchMapper batchMapper, UserMapper userMapper,
                                AttachmentMapper attachmentMapper,
                                AuditUnitMapper unitMapper, AuditLeaderMapper leaderMapper,
                                RectifyMapper rectifyMapper) {
        this.service = service;
        this.approvalMapper = approvalMapper;
        this.batchMapper = batchMapper;
        this.userMapper = userMapper;
        this.attachmentMapper = attachmentMapper;
        this.unitMapper = unitMapper;
        this.leaderMapper = leaderMapper;
        this.rectifyMapper = rectifyMapper;
    }

    // ============ 计划批次 CRUD ============

    @GetMapping("/plan/batches")
    public Result<?> getBatches(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Integer planType,
                                 @RequestParam(required = false) Integer year,
                                 @RequestParam(required = false) Integer approvalStatus,
                                 @RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(service.getList(keyword, planType, year, approvalStatus, pageNum, pageSize));
    }

    @GetMapping("/plan/batches/{batchId}")
    public Result<PlanBatch> getBatch(@PathVariable String batchId) {
        return Result.ok(service.getByBatchId(batchId));
    }

    @PostMapping("/plan/batches")
    public Result<PlanBatch> create(@RequestBody PlanBatch batch) {
        return Result.ok(service.create(batch));
    }

    @PostMapping("/plan/batches/batch")
    public Result<Map<String, Object>> batchCreate(@RequestBody Map<String, List<PlanBatch>> body) {
        List<PlanBatch> batches = body.get("batches");
        if (batches != null) batches.forEach(service::create);
        return Result.ok(Map.of("successCount", batches != null ? batches.size() : 0, "failCount", 0));
    }

    @PutMapping("/plan/batches/{batchId}")
    public Result<Void> update(@PathVariable String batchId, @RequestBody PlanBatch batch) {
        service.update(batchId, batch);
        return Result.ok();
    }

    @DeleteMapping("/plan/batches/{batchId}")
    public Result<Void> delete(@PathVariable String batchId) {
        service.delete(batchId);
        return Result.ok();
    }

    @GetMapping("/plan/statistics/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(service.getSummary());
    }

    // ============ 审批流 ============

    @GetMapping("/plan/batches/{batchId}/approval/progress")
    public Result<Map<String, Object>> approvalProgress(@PathVariable String batchId) {
        List<Map<String, Object>> steps = approvalMapper.getSteps(batchId);

        if (steps.isEmpty()) {
            String[] stepNames = {"提交", "组长审核", "处长审批", "校领导审批", "归档"};
            for (int i = 0; i < stepNames.length; i++) {
                String status = i == 0 ? "ACTIVE" : "PENDING";
                String approver = i == 0 ? "系统自动" : null;
                String comment = i == 0 ? "计划已提交" : null;
                approvalMapper.insertStep(batchId, i + 1, stepNames[i], status, approver, comment);
            }
            steps = approvalMapper.getSteps(batchId);
        }

        int currentStep = -1;
        boolean allCompleted = true;
        for (int i = 0; i < steps.size(); i++) {
            String status = (String) steps.get(i).get("status");
            if ("ACTIVE".equals(status)) {
                currentStep = i;
                allCompleted = false;
                break;
            }
            if ("REJECTED".equals(status)) {
                currentStep = i;
                allCompleted = false;
                break;
            }
            if (!"COMPLETED".equals(status)) {
                allCompleted = false;
            }
        }
        if (allCompleted) {
            currentStep = steps.size();
        }

        return Result.ok(Map.of("steps", steps, "currentStep", currentStep));
    }

    @GetMapping("/plan/batches/{batchId}/approval/history")
    public Result<Map<String, Object>> approvalHistory(@PathVariable String batchId) {
        return Result.ok(Map.of("list", approvalMapper.getHistory(batchId)));
    }

    private static final int[] STEP_PROGRESS = {0, 10, 30, 60, 85, 100};

    private String getApproverName(String authHeader) {
        if (authHeader != null && authHeader.contains("jwt-token-")) {
            String username = authHeader.replace("Bearer jwt-token-", "").trim();
            Map<String, Object> user = userMapper.findByUsername(username);
            if (user != null && user.get("realName") != null) {
                return (String) user.get("realName");
            }
        }
        return "未知用户";
    }

    @PostMapping("/plan/batches/{batchId}/approval/action")
    public Result<Void> approvalAction(@PathVariable String batchId, @RequestBody Map<String, String> body,
                                        @RequestHeader(value = "Authorization", defaultValue = "") String auth) {
        String action = body.get("action");
        String comment = body.get("comment");
        String approverName = getApproverName(auth);

        List<Map<String, Object>> steps = approvalMapper.getSteps(batchId);
        for (Map<String, Object> step : steps) {
            if ("ACTIVE".equals(step.get("status"))) {
                int stepOrder = ((Number) step.get("stepOrder")).intValue();

                String historyId = "APR" + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
                if ("APPROVE".equals(action)) {
                    approvalMapper.updateStep(batchId, stepOrder, "COMPLETED", approverName, comment);
                    int newProgress = STEP_PROGRESS[Math.min(stepOrder, STEP_PROGRESS.length - 1)];
                    batchMapper.updateProgress(batchId, newProgress);
                    int nextOrder = stepOrder + 1;
                    if (nextOrder <= steps.size()) {
                        approvalMapper.updateStep(batchId, nextOrder, "ACTIVE", null, null);
                    }
                    if (nextOrder > steps.size()) {
                        batchMapper.updateApprovalStatus(batchId, 0);
                        batchMapper.updateProgress(batchId, 100);
                        // 审计完成 → 更新关联单位和干部的审计统计
                        updateAuditStats(batchId);
                    }
                    approvalMapper.insertHistory(historyId, batchId, approverName, "已通过");
                } else {
                    approvalMapper.updateStep(batchId, stepOrder, "REJECTED", approverName, comment);
                    batchMapper.updateApprovalStatus(batchId, 4);
                    batchMapper.updateProgress(batchId, 0);
                    approvalMapper.insertHistory(historyId, batchId, approverName, "已驳回");
                    // 驳回 → 为关联单位创建整改记录
                    createRejectionRectify(batchId, approverName, comment);
                }
                break;
            }
        }
        return Result.ok();
    }

    // ============ 计划变更 ============

    @GetMapping("/plan/batches/{batchId}/changes")
    public Result<Map<String, Object>> getChanges(@PathVariable String batchId) {
        return Result.ok(Map.of("list", approvalMapper.getChanges(batchId)));
    }

    @PostMapping("/plan/batches/{batchId}/changes")
    public Result<Void> createChange(@PathVariable String batchId, @RequestBody Map<String, Object> body) {
        String changeId = "CHG" + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
        int changeType = body.get("changeType") != null ? ((Number) body.get("changeType")).intValue() : 0;
        String reason = (String) body.getOrDefault("reason", "");
        String[] changeNames = {"修改计划信息", "调减项目", "修改周期", "其他变更"};
        try {
            String snapshot = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
            approvalMapper.insertChange(changeId, batchId, changeType,
                    changeNames[changeType % changeNames.length], reason, snapshot);
        } catch (Exception e) {
            return Result.fail(500, "保存变更失败");
        }
        return Result.ok();
    }

    /** 确认变更 — 应用变更数据到计划 */
    @PostMapping("/plan/batches/{batchId}/changes/{changeId}/approve")
    public Result<Void> approveChange(@PathVariable String batchId, @PathVariable String changeId) {
        PlanBatch current = batchMapper.findByBatchId(batchId);
        if (current == null) return Result.fail(404, "计划不存在");

        // 标记变更已确认
        approvalMapper.approveChange(changeId);

        // 读取变更快照并应用
        var changes = approvalMapper.getChanges(batchId);
        for (var c : changes) {
            if (changeId.equals(c.get("change_id"))) {
                String raw = (String) c.get("change_data");
                if (raw != null) {
                    try {
                        var data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(raw, Map.class);
                        if (data.containsKey("batchName")) current.setBatchName((String) data.get("batchName"));
                        if (data.containsKey("planType")) current.setPlanType(((Number) data.get("planType")).intValue());
                        if (data.get("startDate") != null && !data.get("startDate").toString().isEmpty())
                            current.setStartDate(java.time.LocalDate.parse(data.get("startDate").toString()));
                        if (data.get("endDate") != null && !data.get("endDate").toString().isEmpty())
                            current.setEndDate(java.time.LocalDate.parse(data.get("endDate").toString()));
                        if (data.containsKey("auditLeader")) current.setAuditLeader((String) data.get("auditLeader"));
                        if (data.containsKey("unitId")) current.setUnitId((String) data.get("unitId"));
                        if (data.containsKey("leaderId")) current.setLeaderId((String) data.get("leaderId"));
                        if (data.containsKey("remark")) current.setRemark((String) data.get("remark"));
                    } catch (Exception ignored) {}
                }
                break;
            }
        }
        batchMapper.update(current);
        return Result.ok();
    }

    /**
     * 驳回时创建整改记录
     */
    private void createRejectionRectify(String batchId, String approverName, String comment) {
        PlanBatch batch = batchMapper.findByBatchId(batchId);
        if (batch == null || batch.getUnitId() == null) return;
        String rectifyId = "RCT" + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
        rectifyMapper.insertRejectionRectify(rectifyId, batchId, batch.getUnitId(),
                "审批驳回: " + (comment != null ? comment : ""),
                approverName);
    }

    /**
     * 重新提交被驳回的计划 — 重置审批步骤为第一步
     */
    @PostMapping("/plan/batches/{batchId}/approval/resubmit")
    public Result<Void> resubmit(@PathVariable String batchId) {
        List<Map<String, Object>> steps = approvalMapper.getSteps(batchId);
        // 所有步骤重置：第一步 ACTIVE，其余 PENDING
        for (int i = 0; i < steps.size(); i++) {
            int order = ((Number) steps.get(i).get("stepOrder")).intValue();
            String status = (order == 1) ? "ACTIVE" : "PENDING";
            approvalMapper.updateStep(batchId, order, status, null, null);
        }
        batchMapper.updateApprovalStatus(batchId, 1); // 审批中
        batchMapper.updateProgress(batchId, 10);
        return Result.ok();
    }

    /**
     * 审计完成时更新关联单位/干部的统计信息 + 清除驳回产生的整改项
     */
    private void updateAuditStats(String batchId) {
        PlanBatch batch = batchMapper.findByBatchId(batchId);
        if (batch == null) return;

        // 清除因驳回产生的整改项（审批通过了）
        rectifyMapper.deleteRejectionRectifies(batchId);

        // 更新被审计单位
        if (batch.getUnitId() != null) {
            var unit = unitMapper.findByUnitId(batch.getUnitId());
            if (unit != null) {
                unit.setTotalAuditCount((unit.getTotalAuditCount() != null ? unit.getTotalAuditCount() : 0) + 1);
                unit.setLatestAuditDate(java.time.LocalDate.now());
                int pending = rectifyMapper.countPendingByUnitId(batch.getUnitId());
                unit.setPendingRectifyCount(pending);
                unitMapper.update(unit);
                // 级联：更新该单位的所有在职领导干部
                var leaders = leaderMapper.findList(null, 1);
                for (var ldr : leaders) {
                    if (unit.getUnitName().equals(ldr.getCurrentUnitName())) {
                        ldr.setPendingRectifyCount(pending);
                        ldr.setLatestAuditDate(java.time.LocalDate.now());
                        leaderMapper.update(ldr);
                    }
                }
            }
        }

        // 更新直接关联的领导干部
        if (batch.getLeaderId() != null) {
            var leader = leaderMapper.findByLeaderId(batch.getLeaderId());
            if (leader != null) {
                leader.setAuditCount((leader.getAuditCount() != null ? leader.getAuditCount() : 0) + 1);
                leader.setLatestAuditDate(java.time.LocalDate.now());
                leaderMapper.update(leader);
            }
        }
    }

    // ============ 穿透查询 ============

    @GetMapping("/plan/projects/{projectId}/penetrate")
    public Result<Map<String, Object>> penetrate(@PathVariable String projectId) {
        PlanBatch batch = service.getByBatchId(projectId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", projectId);
        result.put("projectName", batch != null ? batch.getBatchName() : "未知");

        Map<String, Object> batchInfo = new LinkedHashMap<>();
        batchInfo.put("batchId", projectId);
        batchInfo.put("batchName", batch != null ? batch.getBatchName() : "");
        batchInfo.put("planType", batch != null ? batch.getPlanType() : 0);
        result.put("batchInfo", batchInfo);

        List<Map<String, Object>> attachments = attachmentMapper.findByBatchId(projectId);
        List<Map<String, Object>> guidingFiles = new ArrayList<>();
        for (Map<String, Object> att : attachments) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("fileName", att.getOrDefault("file_name", ""));
            f.put("attachId", att.getOrDefault("attach_id", ""));
            f.put("previewUrl", "/api/audit/v1/files/" + att.get("attach_id") + "/preview");
            guidingFiles.add(f);
        }
        result.put("guidingFiles", guidingFiles);

        int prog = batch != null && batch.getProgress() != null ? batch.getProgress() : 0;
        String phase = prog >= 100 ? "已完成" : prog > 70 ? "报告阶段" : prog > 30 ? "实施阶段" : "准备阶段";
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("projectStatus", batch != null ? batch.getApprovalStatus() : 0);
        progress.put("progressPercent", prog);
        progress.put("currentPhase", phase);
        result.put("progress", progress);

        Map<String, Object> papers = new LinkedHashMap<>();
        papers.put("totalCount", Math.max(prog / 4, 0));
        papers.put("completedCount", Math.max(prog / 5, 0));
        result.put("workingPapers", papers);

        Map<String, Object> reports = new LinkedHashMap<>();
        reports.put("draftSubmitted", prog >= 50);
        reports.put("finalSubmitted", prog >= 100);
        result.put("reports", reports);

        List<Map<String, Object>> rectList = rectifyMapper.findByBatchId(projectId);
        int totalIssues = rectList.size();
        int rectified = 0, rectifying = 0, pending = 0;
        for (Map<String, Object> r : rectList) {
            Object st = r.get("rectify_status");
            int status = st instanceof Number ? ((Number) st).intValue() : 0;
            if (status == 2) rectified++; else if (status == 1) rectifying++; else pending++;
        }
        Map<String, Object> rectify = new LinkedHashMap<>();
        rectify.put("totalIssues", totalIssues);
        rectify.put("rectifiedCount", rectified);
        rectify.put("rectifyingCount", rectifying);
        rectify.put("pendingCount", pending);
        rectify.put("list", rectList);
        result.put("rectifyLedger", rectify);

        return Result.ok(result);
    }
}
