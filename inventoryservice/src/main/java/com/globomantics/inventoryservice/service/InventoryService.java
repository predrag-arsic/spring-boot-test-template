package com.globomantics.inventoryservice.service;

import java.util.Optional;

import com.globomantics.inventoryservice.model.InventoryRecord;

public interface InventoryService {
    Optional<InventoryRecord> getInventoryRecord(Integer productId);
    Optional<InventoryRecord> purchaseProduct(Integer productId, Integer quantity);
}
