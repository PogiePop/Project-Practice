package com.audit.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AuditLeader {
    private Long id;
    private String leaderId;
    private String leaderCode;
    private String leaderName;
    private String staffId;
    private String currentUnitName;
    private String currentPosition;
    private Integer isActive;
    private LocalDate tenureStartDate;
    private BigDecimal tenureYears;
    private BigDecimal fundScope;
    private Integer auditCount;
    private LocalDate latestAuditDate;
    private String latestAuditConclusion;
    private Integer pendingRectifyCount;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }
    public String getLeaderCode() { return leaderCode; }
    public void setLeaderCode(String leaderCode) { this.leaderCode = leaderCode; }
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }
    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }
    public String getCurrentUnitName() { return currentUnitName; }
    public void setCurrentUnitName(String currentUnitName) { this.currentUnitName = currentUnitName; }
    public String getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public LocalDate getTenureStartDate() { return tenureStartDate; }
    public void setTenureStartDate(LocalDate tenureStartDate) { this.tenureStartDate = tenureStartDate; }
    public BigDecimal getTenureYears() { return tenureYears; }
    public void setTenureYears(BigDecimal tenureYears) { this.tenureYears = tenureYears; }
    public BigDecimal getFundScope() { return fundScope; }
    public void setFundScope(BigDecimal fundScope) { this.fundScope = fundScope; }
    public Integer getAuditCount() { return auditCount; }
    public void setAuditCount(Integer auditCount) { this.auditCount = auditCount; }
    public LocalDate getLatestAuditDate() { return latestAuditDate; }
    public void setLatestAuditDate(LocalDate latestAuditDate) { this.latestAuditDate = latestAuditDate; }
    public String getLatestAuditConclusion() { return latestAuditConclusion; }
    public void setLatestAuditConclusion(String latestAuditConclusion) { this.latestAuditConclusion = latestAuditConclusion; }
    public Integer getPendingRectifyCount() { return pendingRectifyCount; }
    public void setPendingRectifyCount(Integer pendingRectifyCount) { this.pendingRectifyCount = pendingRectifyCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
