package com.audit.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PlanBatch {
    private Long id;
    private String batchId;
    private String batchName;
    private Integer planType;
    private Integer year;
    private Integer projectCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer approvalStatus;
    private Integer progress;
    private Integer isOutsource;
    private String auditLeader;
    private String auditLeaderId;
    private String remark;
    private LocalDateTime createTime;
    private String unitId;
    private String leaderId;
    private String auditConclusion;
    private String unitName;  // JOIN 查询得到的单位名称
    private LocalDateTime updateTime;

    // Getters
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }
    public String getAuditConclusion() { return auditConclusion; }
    public void setAuditConclusion(String auditConclusion) { this.auditConclusion = auditConclusion; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    // Other Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public Integer getPlanType() { return planType; }
    public void setPlanType(Integer planType) { this.planType = planType; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(Integer approvalStatus) { this.approvalStatus = approvalStatus; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public Integer getIsOutsource() { return isOutsource; }
    public void setIsOutsource(Integer isOutsource) { this.isOutsource = isOutsource; }
    public String getAuditLeader() { return auditLeader; }
    public void setAuditLeader(String auditLeader) { this.auditLeader = auditLeader; }
    public String getAuditLeaderId() { return auditLeaderId; }
    public void setAuditLeaderId(String auditLeaderId) { this.auditLeaderId = auditLeaderId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
