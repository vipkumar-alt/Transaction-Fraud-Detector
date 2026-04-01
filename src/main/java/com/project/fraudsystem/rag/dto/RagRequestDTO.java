package com.project.fraudsystem.rag.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class RagRequestDTO {

    @JsonAlias("amt")
    private double amount;
    @JsonAlias("category")
    private String merchantCategory;
    private String deviceType;
    private boolean newDevice;
    private boolean international;
    private String transactionTime;
    private int accountAgeDays;
    private int transactionsLast24h;
    private String merchant;
    @JsonAlias({"transDateTransTime", "trans_date_trans_time"})
    private String transactionTimestamp;
    private String gender;
    private String city;
    private String state;
    private String job;
    private String zip;
    private String dob;
    @JsonAlias("cc_num")
    private String ccNum;
    @JsonAlias("city_pop")
    private Long cityPopulation;
    @JsonAlias("lat")
    private Double latitude;
    @JsonAlias("long")
    private Double longitude;
    @JsonAlias("merch_lat")
    private Double merchantLatitude;
    @JsonAlias("merch_long")
    private Double merchantLongitude;

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

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(String transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getCcNum() {
        return ccNum;
    }

    public void setCcNum(String ccNum) {
        this.ccNum = ccNum;
    }

    public Long getCityPopulation() {
        return cityPopulation;
    }

    public void setCityPopulation(Long cityPopulation) {
        this.cityPopulation = cityPopulation;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getMerchantLatitude() {
        return merchantLatitude;
    }

    public void setMerchantLatitude(Double merchantLatitude) {
        this.merchantLatitude = merchantLatitude;
    }

    public Double getMerchantLongitude() {
        return merchantLongitude;
    }

    public void setMerchantLongitude(Double merchantLongitude) {
        this.merchantLongitude = merchantLongitude;
    }
}
