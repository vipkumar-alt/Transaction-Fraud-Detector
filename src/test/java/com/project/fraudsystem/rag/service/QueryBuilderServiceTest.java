package com.project.fraudsystem.rag.service;

import com.project.fraudsystem.rag.dto.RagRequestDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryBuilderServiceTest {

    private final QueryBuilderService queryBuilderService = new QueryBuilderService();

    @Test
    void buildQueryUsesFullSentenceNarrativeForEveningTransaction() {
        RagRequestDTO request = new RagRequestDTO();
        request.setAmount(220);
        request.setMerchantCategory("misc_net");
        request.setDeviceType("web");
        request.setNewDevice(true);
        request.setInternational(false);
        request.setTransactionTime("19:45");
        request.setAccountAgeDays(60);
        request.setTransactionsLast24h(6);

        String query = queryBuilderService.buildQuery(request);

        assertThat(query).isEqualTo(
                "Transaction amount is 220. "
                        + "Merchant category is misc_net. "
                        + "Device type is web. "
                        + "The transaction is from a new device. "
                        + "The transaction is domestic, not international. "
                        + "Transaction time is 19:45 in the evening. "
                        + "Account age is 60 days. "
                        + "The account made 6 transactions in the last 24 hours."
        );
    }

    @Test
    void buildQueryUsesNightNarrativeForInternationalTransaction() {
        RagRequestDTO request = new RagRequestDTO();
        request.setAmount(8500);
        request.setMerchantCategory("shopping_net");
        request.setDeviceType("mobile");
        request.setNewDevice(true);
        request.setInternational(true);
        request.setTransactionTime("02:15");
        request.setAccountAgeDays(10);
        request.setTransactionsLast24h(12);

        String query = queryBuilderService.buildQuery(request);

        assertThat(query).isEqualTo(
                "Transaction amount is 8500. "
                        + "Merchant category is shopping_net. "
                        + "Device type is mobile. "
                        + "The transaction is from a new device. "
                        + "The transaction is international. "
                        + "Transaction time is 02:15 at night. "
                        + "Account age is 10 days. "
                        + "The account made 12 transactions in the last 24 hours."
        );
    }
}
