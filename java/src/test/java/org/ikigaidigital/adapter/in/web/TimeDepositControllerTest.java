package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.model.BalanceUpdate;
import org.ikigaidigital.application.model.TimeDepositDetails;
import org.ikigaidigital.application.model.Withdrawal;
import org.ikigaidigital.application.port.TimeDepositRepository;
import org.ikigaidigital.application.service.TimeDepositService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimeDepositController.class)
@ContextConfiguration(classes = {TimeDepositController.class, TimeDepositControllerTest.TestConfig.class})
class TimeDepositControllerTest {
    @Autowired
    private InMemoryTimeDepositRepository repository;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository.deposits.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        InMemoryTimeDepositRepository repository() {
            return new InMemoryTimeDepositRepository();
        }

        @Bean
        TimeDepositService service(InMemoryTimeDepositRepository repository) {
            return new TimeDepositService(repository, new TimeDepositCalculator());
        }
    }

    @Test
    void getReturnsDepositFieldsAndWithdrawalHistoryAsJson() throws Exception {
        repository.deposits.add(new TimeDepositDetails(1, "basic", new BigDecimal("1200.123"), 31,
            List.of(new Withdrawal(7, 1, new BigDecimal("50.25"), LocalDate.of(2026, 11, 1)))));
        repository.deposits.add(new TimeDepositDetails(2, "student", new BigDecimal("100.01"), 365, List.of()));

        mvc.perform(get("/time-deposits"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                [
                  {"id":1,"planType":"basic","balance":1200.123,"days":31,
                   "withdrawals":[{"id":7,"timeDepositId":1,"amount":50.25,"date":"2026-11-01"}]},
                  {"id":2,"planType":"student","balance":100.01,"days":365,"withdrawals":[]}
                ]
                """, JsonCompareMode.STRICT));
    }

    @Test
    void getReturnsAnEmptyJsonArrayWhenNoDepositsExist() throws Exception {
        mvc.perform(get("/time-deposits"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void postReturnsNoContentAndGetShowsUpdatedBalances() throws Exception {
        repository.deposits.add(new TimeDepositDetails(1, "basic", new BigDecimal("1200.123"), 31,
            List.of(new Withdrawal(7, 1, new BigDecimal("50.25"), LocalDate.of(2026, 11, 1)))));

        mvc.perform(post("/time-deposits/update-balances"))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        mvc.perform(get("/time-deposits"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                [{"id":1,"planType":"basic","balance":1201.123,"days":31,
                  "withdrawals":[{"id":7,"timeDepositId":1,"amount":50.25,"date":"2026-11-01"}]}]
                """, JsonCompareMode.STRICT));
    }

    @Test
    void postSucceedsWithoutARequestBodyWhenNoDepositsExist() throws Exception {
        mvc.perform(post("/time-deposits/update-balances"))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
    }

    private static class InMemoryTimeDepositRepository implements TimeDepositRepository {
        private final List<TimeDepositDetails> deposits = new ArrayList<>();

        @Override
        public List<TimeDepositDetails> findAll() {
            return List.copyOf(deposits);
        }

        @Override
        public void saveBalances(List<BalanceUpdate> updates) {
            for (BalanceUpdate update : updates) {
                deposits.replaceAll(existing -> existing.id() == update.id()
                    ? new TimeDepositDetails(existing.id(), existing.planType(), update.balance(),
                        existing.days(), existing.withdrawals())
                    : existing);
            }
        }
    }
}
