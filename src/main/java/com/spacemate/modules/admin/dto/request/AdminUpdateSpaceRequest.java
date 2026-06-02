package com.spacemate.modules.admin.dto.request;

import java.math.BigDecimal;
import java.time.LocalTime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class AdminUpdateSpaceRequest {

    @Size(max = 100)
    private String name;

    private LocalTime openStartTime;

    private LocalTime openEndTime;

    @Size(max = 64)
    private String wifiSsid;

    @Size(max = 128)
    private String wifiPassword;

    private String rules;

    @DecimalMin("0")
    private BigDecimal priceHourly;

    @Min(0)
    @Max(1)
    private Integer status;

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

    public String getWifiPassword() {
        return wifiPassword;
    }

    public void setWifiPassword(String wifiPassword) {
        this.wifiPassword = wifiPassword;
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
}



