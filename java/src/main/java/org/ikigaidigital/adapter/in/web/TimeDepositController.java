package org.ikigaidigital.adapter.in.web;

import lombok.RequiredArgsConstructor;

import org.ikigaidigital.application.model.TimeDepositDetails;
import org.ikigaidigital.application.service.TimeDepositService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HTTP adapter for the two operations defined in the OpenAPI contract. */
@RestController
@RequestMapping("/time-deposits")
@RequiredArgsConstructor
public class TimeDepositController {
    private final TimeDepositService service;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TimeDepositDetails> getTimeDeposits() {
        return service.getTimeDeposits();
    }

    @PostMapping("/update-balances")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBalances() {
        service.updateBalances();
    }
}
