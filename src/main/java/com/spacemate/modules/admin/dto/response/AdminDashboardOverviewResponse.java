package com.spacemate.modules.admin.dto.response;

public class AdminDashboardOverviewResponse {

    private String date;
    private Long totalBookings;
    private Long pendingBookings;
    private Long usedBookings;
    private Long cancelledBookings;
    private Double occupancyRate;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(Long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public Long getPendingBookings() {
        return pendingBookings;
    }

    public void setPendingBookings(Long pendingBookings) {
        this.pendingBookings = pendingBookings;
    }

    public Long getUsedBookings() {
        return usedBookings;
    }

    public void setUsedBookings(Long usedBookings) {
        this.usedBookings = usedBookings;
    }

    public Long getCancelledBookings() {
        return cancelledBookings;
    }

    public void setCancelledBookings(Long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
    }

    public Double getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(Double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }
}


