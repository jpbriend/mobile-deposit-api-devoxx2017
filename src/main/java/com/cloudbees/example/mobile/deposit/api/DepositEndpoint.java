package com.cloudbees.example.mobile.deposit.api;

/**
 * Created by kmadel on 6/12/15.
 */

import com.cloudbees.example.mobile.deposit.api.model.Deposit;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.math.BigDecimal;


@Component
@Path("/account/deposit")
public class DepositEndpoint {

    @GET
    public Deposit getDepositAccount() {

        Deposit depositAccount = new Deposit();
        depositAccount.setAccountNumber(1234567890L);
        depositAccount.setBalance(new BigDecimal(8760.85));
        depositAccount.setVersion(0.1F);

        return depositAccount;
    }

}