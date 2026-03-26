package com.project.fraudsystem.rag.dto;

public class RagRequestDTO {

    private double amount;
    private String merchantCategory;
    private String deviceType;
    private boolean newDevice;
    private boolean international;
    private String transactionTime;
    private int accountAgeDays;
    private int transactionsLast24h;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getMerchantCategory() {
        return merchantCategory;
    }

    public void setMerchantCategory(String merchantCategory) {
        this.merchantCategory = merchantCategory;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public boolean isNewDevice() {
        return newDevice;
    }

    public void setNewDevice(boolean newDevice) {
        this.newDevice = newDevice;
    }

    public boolean isInternational() {
        return international;
    }

    public void setInternational(boolean international) {
        this.international = international;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public int getAccountAgeDays() {
        return accountAgeDays;
    }

    public void setAccountAgeDays(int accountAgeDays) {
        this.accountAgeDays = accountAgeDays;
    }

    public int getTransactionsLast24h() {
        return transactionsLast24h;
    }

    public void setTransactionsLast24h(int transactionsLast24h) {
        this.transactionsLast24h = transactionsLast24h;
    }
}