package com.cloudbees.example.mobile.deposit.api.model;

import java.math.BigDecimal;

/**
 * Created by kmadel on 6/12/15.
 */
public class Deposit {

    private long accountNumber;

    private String version;

    private BigDecimal balance;

    public long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
