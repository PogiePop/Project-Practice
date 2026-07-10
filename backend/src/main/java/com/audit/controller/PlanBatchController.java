package com.audit.controller;

import cn.hutool.core.util.IdUtil;
import com.audit.common.Result;
import com.audit.entity.PlanBatch;
import com.audit.mapper.ApprovalMapper;
import com.audit.mapper.AttachmentMapper;
import com.audit.mapper.AuditUnitMapper;
import com.audit.mapper.AuditLeaderMapper;
import com.audit.mapper.MessageMapper;
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
    private final MessageMapper messageMapper;

    public PlanBatchController(PlanBatchService service, ApprovalMapper approvalMapper,
                                PlanBatchMapper batchMapper, UserMapper userMapper,
                                AttachmentMapper attachmentMapper,
                                AuditUnitMapper unitMapper, AuditLeaderMapper leaderMapper,
                                RectifyMapper rectifyMapper, MessageMapper messageMapper) {
        this.service = service;
        this.approvalMapper = approvalMapper;
        this.batchMapper = batchMapper;
        this.userMapper = userMapper;
        this.attachmentMapper = attachmentMapper;
        this.unitMapper = unitMapper;
        this.leaderMapper = leaderMapper;
        this.rectifyMapper = rectifyMapper;
        this.messageMapper = messageMapper;
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

    /** 导出后归档（仅进度100%的计划才会归档） */
    @PostMapping("/plan/batches/{batchId}/archive")
    public Result<Void> archive(@PathVariable String batchId) {
        PlanBatch batch = batchMapper.findByBatchId(batchId);
        if (batch != null && batch.getProgress() != null && batch.getProgress() >= 100) {
            batchMapper.updateApprovalStatus(batchId, 2); // 已归档
        }
        return Result.ok();
    }

    /** 保存审计记录（实际金额），实际>预算自动驳回+预警 */
    @PutMapping("/plan/batches/{batchId}/audit-record")
    public Result<Void> saveAuditRecord(@PathVariable String batchId, @RequestBody Map<String, Object> body) {
        PlanBatch batch = batchMapper.findByBatchId(batchId);
        if (batch == null) return Result.fail(404, "计划不存在");

        java.math.BigDecimal actualAmt = body.get("actualAmount") != null ?
                new java.math.BigDecimal(body.get("actualAmount").toString()) : java.math.BigDecimal.ZERO;
        java.math.BigDecimal budget = batch.getProjectAmount() != null ? batch.getProjectAmount() : java.math.BigDecimal.ZERO;

        // 实际使用金额超过预算 → 自动驳回 + 预警
        if (actualAmt.compareTo(java.math.BigDecimal.ZERO) > 0 && actualAmt.compareTo(budget) > 0) {
            batch.setActualAmount(actualAmt);
            batch.setApprovalStatus(4); // 已驳回
            batch.setProgress(0);
            batchMapper.updateAmounts(batch);
            batchMapper.updateApprovalStatus(batchId, 4);
            batchMapper.updateProgress(batchId, 0);
            // 创建预警消息
            messageMapper.insertAlert("MSG" + IdUtil.fastSimpleUUID().substring(0,6).toUpperCase(),
                    "预算超支预警", "项目「" + batch.getBatchName() + "」实际使用" + actualAmt + "万超过预算" + budget + "万，已自动驳回",
                    "ALERT", batchId);
            return Result.fail(400, "实际使用金额(" + actualAmt + "万)超过预算(" + budget + "万)，已自动驳回并发出预警");
        }

        batch.setActualAmount(actualAmt);
        batchMapper.updateAmounts(batch);
        // 同步审计结论到领导
        if (batch.getLeaderId() != null && batch.getAuditConclusion() != null) {
            var leader = leaderMapper.findByLeaderId(batch.getLeaderId());
            if (leader != null) {
                leader.setLatestAuditConclusion(batch.getAuditConclusion());
                leaderMapper.update(leader);
            }
        }
        return Result.ok();
    }

    /** 批量导出后归档（仅进度100%的计划才会归档） */
    @PostMapping("/plan/batches/archive-batch")
    public Result<Void> archiveBatch(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.get("batchIds");
        if (ids != null) {
            for (String id : ids) {
                PlanBatch batch = batchMapper.findByBatchId(id);
                if (batch != null && batch.getProgress() != null && batch.getProgress() >= 100) {
                    batchMapper.updateApprovalStatus(id, 2);
                }
            }
        }
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

    // 审批步骤→进度→状态映射
    // 步骤1提交→10%, 步骤2组长→30%, 步骤3处长→60% (审批中)
    // 步骤4校领导→85%, 步骤5归档→100% (已审批)
    private static final int[] STEP_PROGRESS = {0, 10, 30, 60, 85, 100};
    private static final int[] STEP_STATUS  = {3, 1, 1, 1, 0, 0}; // 草稿,审批中×3,已审批×2

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

        // 预算检查：实际使用>项目金额 → 无论通过/驳回都自动打回
        PlanBatch batch = batchMapper.findByBatchId(batchId);
        if (batch != null && batch.getActualAmount() != null && batch.getProjectAmount() != null
                && batch.getActualAmount().compareTo(batch.getProjectAmount()) > 0) {
            batch.setApprovalStatus(4);
            batch.setProgress(0);
            batchMapper.updateApprovalStatus(batchId, 4);
            batchMapper.updateProgress(batchId, 0);
            // 检查是否已有预警，避免重复
            messageMapper.insertAlert("MSG" + IdUtil.fastSimpleUUID().substring(0,6).toUpperCase(),
                    "预算超支预警",
                    "「" + batch.getBatchName() + "」实际使用" + batch.getActualAmount() + "万超过预算" + batch.getProjectAmount() + "万，审批已自动驳回",
                    "ALERT", batchId);
            return Result.fail(400, "实际使用金额超出预算，审批已自动驳回。请修正实际使用金额后重新提交。");
        }

        List<Map<String, Object>> steps = approvalMapper.getSteps(batchId);
        for (Map<String, Object> step : steps) {
            if ("ACTIVE".equals(step.get("status"))) {
                int stepOrder = ((Number) step.get("stepOrder")).intValue();

                String historyId = "APR" + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
                if ("APPROVE".equals(action)) {
                    approvalMapper.updateStep(batchId, stepOrder, "COMPLETED", approverName, comment);
                    int idx = Math.min(stepOrder, STEP_PROGRESS.length - 1);
                    batchMapper.updateProgress(batchId, STEP_PROGRESS[idx]);
                    batchMapper.updateApprovalStatus(batchId, STEP_STATUS[idx]);
                    int nextOrder = stepOrder + 1;
                    if (nextOrder <= steps.size()) {
                        approvalMapper.updateStep(batchId, nextOrder, "ACTIVE", null, null);
                    }
                    // 每个阶段完成都更新最近审计日期，最后一步+审计次数
                    updateUnitLeaderDate(batchId);
                    if (nextOrder > steps.size()) {
                        updateAuditStats(batchId);
                    }
                    approvalMapper.insertHistory(historyId, batchId, approverName, "已通过");
                } else {
                    approvalMapper.updateStep(batchId, stepOrder, "REJECTED", approverName, comment);
                    batchMapper.updateApprovalStatus(batchId, 4);
                    batchMapper.updateProgress(batchId, 0);
                    approvalMapper.insertHistory(historyId, batchId, approverName, "已驳回");
                    createRejectionRectify(batchId, approverName, comment);
                    // 驳回也算一次审计
                    updateAuditCounts(batchId);
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
        String[] changeNames = {"修改基本信息", "调整金额预算", "变更审计对象", "修改实施周期"};
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
                        if (data.containsKey("projectAmount") && data.get("projectAmount") != null)
                            current.setProjectAmount(new java.math.BigDecimal(data.get("projectAmount").toString()));
                        if (data.containsKey("fundSource")) current.setFundSource((String) data.get("fundSource"));
                    } catch (Exception ignored) {}
                }
                break;
            }
        }
        // 变更后重算关联领导资金
        batchMapper.update(current);
        if (current.getLeaderId() != null) {
            service.updateLeaderFunds(current.getLeaderId());
        }
        updateAuditCounts(batchId);
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
        for (int i = 0; i < steps.size(); i++) {
            int order = ((Number) steps.get(i).get("stepOrder")).intValue();
            String status = (order == 1) ? "ACTIVE" : "PENDING";
            approvalMapper.updateStep(batchId, order, status, null, null);
        }
        batchMapper.updateApprovalStatus(batchId, 1);
        batchMapper.updateProgress(batchId, 10);
        // 将驳回产生的整改项标为已整改
        rectifyMapper.markRejectionResolved(batchId);
        return Result.ok();
    }

    /**
     * 审计完成时更新关联单位/干部的统计信息 + 清除驳回产生的整改项
     */
    /** 驳回/审批完成时 +1 审计次数 */
    private void updateAuditCounts(String batchId) {
        PlanBatch batch = batchMapper.findByBatchId(batchId);
        if (batch == null) return;
        if (batch.getUnitId() != null) {
            unitMapper.incrementAuditCount(batch.getUnitId());
        }
        if (batch.getLeaderId() != null) {
            leaderMapper.incrementAuditCount(batch.getLeaderId());
        }
    }

    /** 每个审批阶段完成时更新单位/领导的最近审计日期 */
    private void updateUnitLeaderDate(String batchId) {
        PlanBatch batch = batchMapper.findByBatchId(batchId);
        if (batch == null) return;
        if (batch.getUnitId() != null) {
            unitMapper.updateLatestAuditDate(batch.getUnitId());
        }
        if (batch.getLeaderId() != null) {
            leaderMapper.updateLatestAuditDate(batch.getLeaderId());
        }
    }

    private void updateAuditStats(String batchId) {
        PlanBatch batch = batchMapper.findByBatchId(batchId);
        if (batch == null) return;

        // 清除因驳回产生的整改项（审批通过了）
        rectifyMapper.deleteRejectionRectifies(batchId);

        // 更新被审计单位
        if (batch.getUnitId() != null) {
            int pending = rectifyMapper.countPendingByUnitId(batch.getUnitId());
            unitMapper.incrementAuditStats(batch.getUnitId(), pending);
        }

        // 更新直接关联的领导干部
        if (batch.getLeaderId() != null) {
            var leader = leaderMapper.findByLeaderId(batch.getLeaderId());
            if (leader != null) {
                leader.setAuditCount((leader.getAuditCount() != null ? leader.getAuditCount() : 0) + 1);
                leader.setLatestAuditDate(java.time.LocalDate.now());
                leader.setFundScope(null); // 防止 fundScope 被 update 覆盖
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
        papers.put("totalCount", approvalMapper.getHistory(projectId).size());
        result.put("workingPapers", papers);

        Map<String, Object> reports = new LinkedHashMap<>();
        reports.put("draftSubmitted", prog >= 50);
        reports.put("finalSubmitted", prog >= 100);
        result.put("reports", reports);

        List<Map<String, Object>> rectList = rectifyMapper.findByBatchId(projectId);
        int totalIssues = (int) rectList.stream().filter(r -> "审批驳回".equals(r.get("issue_category"))).count();
        int rectified = (int) rectList.stream().filter(r -> "审批驳回".equals(r.get("issue_category")) && r.get("rectify_status") instanceof Number && ((Number)r.get("rectify_status")).intValue() == 2).count();
        int rectifying = (int) rectList.stream().filter(r -> "审批驳回".equals(r.get("issue_category")) && r.get("rectify_status") instanceof Number && ((Number)r.get("rectify_status")).intValue() == 1).count();
        int pending = totalIssues - rectified - rectifying;
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
