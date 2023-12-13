package com.globomantics.inventoryservice.service;

import java.util.Optional;

import com.globomantics.inventoryservice.model.InventoryRecord;
import com.globomantics.inventoryservice.model.PurchaseRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Value("${inventorymanager.baseUrl}")
    private String baseUrl;

    // Create a RestTemplate to use to communicate with the Inventory Manager Service
    RestTemplate restTemplate = new RestTemplate();

    @Override
    public Optional<InventoryRecord> getInventoryRecord(Integer productId) {
        try {
            // Get the inventory record for the specified product ID
            return Optional.of(restTemplate.getForObject(baseUrl + "/" + productId, InventoryRecord.class));
        } catch (HttpClientErrorException e) {
            // An exception occurred, so return Optional.empty()
            return Optional.empty();
        }
    }

    @Override
    public Optional<InventoryRecord> purchaseProduct(Integer productId, Integer quantity) {
        try {
            return Optional.of(restTemplate.postForObject(baseUrl + "/" + productId + "/purchaseRecord",
                    new PurchaseRecord(productId, quantity),
                    InventoryRecord.class));
        } catch (HttpClientErrorException e) {
            return Optional.empty();
        }
    }
}
