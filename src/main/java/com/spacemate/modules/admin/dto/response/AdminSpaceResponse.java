package com.spacemate.modules.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AdminSpaceResponse {

    private Long id;
    private String code;
    private String name;
    private LocalTime openStartTime;
    private LocalTime openEndTime;
    private String wifiSsid;
    private String rules;
    private BigDecimal priceHourly;
    private Integer status;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalTime getOpenStartTime() {
        return openStartTime;
    }

    public void setOpenStartTime(LocalTime openStartTime) {
        this.openStartTime = openStartTime;
    }

    public LocalTime getOpenEndTime() {
        return openEndTime;
    }

    public void setOpenEndTime(LocalTime openEndTime) {
        this.openEndTime = openEndTime;
    }

    public String getWifiSsid() {
        return wifiSsid;
    }

    public void setWifiSsid(String wifiSsid) {
        this.wifiSsid = wifiSsid;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public BigDecimal getPriceHourly() {
        return priceHourly;
    }

    public void setPriceHourly(BigDecimal priceHourly) {
        this.priceHourly = priceHourly;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


