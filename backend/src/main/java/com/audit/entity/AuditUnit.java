package com.audit.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AuditUnit {
    private Long id;
    private String unitId;
    private String unitCode;
    private String unitName;
    private Integer category;
    private String categoryName;
    private Integer establishmentCount;
    private BigDecimal fundScale;
    private String leaderInCharge;
    private String financeContact;
    private String financeContactPhone;
    private String address;
    private LocalDate setupDate;
    private Integer totalAuditCount;
    private LocalDate latestAuditDate;
    private Integer pendingRectifyCount;
    private String leaderNames; // 关联领导干部姓名（非DB字段，由controller填充）
    private LocalDateTime createTime;

    public String getLeaderNames() { return leaderNames; }
    public void setLeaderNames(String leaderNames) { this.leaderNames = leaderNames; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getEstablishmentCount() { return establishmentCount; }
    public void setEstablishmentCount(Integer establishmentCount) { this.establishmentCount = establishmentCount; }
    public BigDecimal getFundScale() { return fundScale; }
    public void setFundScale(BigDecimal fundScale) { this.fundScale = fundScale; }
    public String getLeaderInCharge() { return leaderInCharge; }
    public void setLeaderInCharge(String leaderInCharge) { this.leaderInCharge = leaderInCharge; }
    public String getFinanceContact() { return financeContact; }
    public void setFinanceContact(String financeContact) { this.financeContact = financeContact; }
    public String getFinanceContactPhone() { return financeContactPhone; }
    public void setFinanceContactPhone(String financeContactPhone) { this.financeContactPhone = financeContactPhone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDate getSetupDate() { return setupDate; }
    public void setSetupDate(LocalDate setupDate) { this.setupDate = setupDate; }
    public Integer getTotalAuditCount() { return totalAuditCount; }
    public void setTotalAuditCount(Integer totalAuditCount) { this.totalAuditCount = totalAuditCount; }
    public LocalDate getLatestAuditDate() { return latestAuditDate; }
    public void setLatestAuditDate(LocalDate latestAuditDate) { this.latestAuditDate = latestAuditDate; }
    public Integer getPendingRectifyCount() { return pendingRectifyCount; }
    public void setPendingRectifyCount(Integer pendingRectifyCount) { this.pendingRectifyCount = pendingRectifyCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
