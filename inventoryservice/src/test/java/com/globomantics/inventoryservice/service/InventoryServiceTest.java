package com.globomantics.inventoryservice.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.util.Optional;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.globomantics.inventoryservice.model.InventoryRecord;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@WireMockTest(httpPort = 9999)
@SpringBootTest
@TestPropertySource(locations = "classpath:test.properties")
public class InventoryServiceTest {
    @Autowired
    private InventoryService service;

    @Test
    void testGetInventoryRecordSuccess() {
        stubFor(get(urlEqualTo("/inventory/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("json/inventory-response.json")));

        Optional<InventoryRecord> record = service.getInventoryRecord(1);
        Assertions.assertTrue(record.isPresent(), "InventoryRecord should be present");

        // Validate the contents of the response
        Assertions.assertEquals(500, record.get().getQuantity().intValue(),
                "The quantity should be 500");
    }

    @Test
    void testGetInventoryRecordNotFound() {
        stubFor(get(urlEqualTo("/inventory/2"))
                .willReturn(aResponse().withStatus(404)));
        Optional<InventoryRecord> record = service.getInventoryRecord(2);
        Assertions.assertFalse(record.isPresent(), "InventoryRecord should not be present");
    }

    @Test
    void testPurchaseProductSuccess() {
        stubFor(post("/inventory/1/purchaseRecord")
                // Actual Header sent by the RestTemplate is: application/json;charset=UTF-8
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(containing("\"productId\":1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("json/inventory-response-after-post.json")));

        Optional<InventoryRecord> record = service.purchaseProduct(1, 5);
        Assertions.assertTrue(record.isPresent(), "InventoryRecord should be present");

        // Validate the contents of the response
        Assertions.assertEquals(495, record.get().getQuantity().intValue(),
                "The quantity should be 495");
    }
}
