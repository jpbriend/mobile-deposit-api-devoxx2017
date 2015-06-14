package com.cloudbees.example.mobile.deposit.api;

import com.cloudbees.example.mobile.deposit.api.model.Deposit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;

/**
 * Created by kmadel on 6/13/15.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MobileDepositApplication.class)
@IntegrationTest({"server.port=0","version=0.0.1"})
@WebAppConfiguration
public class DepositEndpointTests {

    @Value("${local.server.port}")
    private int port;

    private RestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void testGetDepositAccount() {
        ResponseEntity<Deposit> entity = this.restTemplate.getForEntity(
                "http://localhost:" + this.port + "/account/deposit", Deposit.class);
        assertEquals(HttpStatus.OK, entity.getStatusCode());


        Deposit deposit = entity.getBody();
        assertEquals(1234567890L, deposit.getAccountNumber());

    }

}
