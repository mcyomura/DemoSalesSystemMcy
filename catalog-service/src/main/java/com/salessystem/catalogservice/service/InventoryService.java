package com.salessystem.catalogservice.service;

import com.salessystem.catalogservice.dto.OrderEventDTO;
import com.salessystem.catalogservice.model.Inventory;
import com.salessystem.catalogservice.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    // Manual dependency injection through constructor
    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    // Process the stock subtraction inside a safe database transaction, commits at the end
    @Transactional
    public void deductStock(OrderEventDTO event) {
        // Loop through each item present in the Kafka event payload
        for (OrderEventDTO.OrderItemEventDTO item : event.getItems()) {

            // Fetch the current stock for the product or throw an exception if not found
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Stock registry not found for Product ID: " + item.getProductId()
                    ));

            // Business rule: Verify if there is enough items available in stock
            if (inventory.getQuantity() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for Product ID: " + item.getProductId() +
                                ". Available: " + inventory.getQuantity() + ", Requested: " + item.getQuantity()
                );
            }

            // Calculate the new remaining quantity
            int updatedQuantity = inventory.getQuantity() - item.getQuantity();
            inventory.setQuantity(updatedQuantity);

            // Save the updated entity state back to the database
            inventoryRepository.save(inventory);
        }
    }

    // Process the stock subtraction inside a safe database transaction, commits at the end
    @Transactional
    public void returnStock(OrderEventDTO event) {
        // Loop through each item present in the Kafka event payload
        for (OrderEventDTO.OrderItemEventDTO item : event.getItems()) {

            // Fetch the current stock for the product or throw an exception if not found
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Stock registry not found for Product ID: " + item.getProductId()
                    ));

            // Calculate the new remaining quantity
            int updatedQuantity = inventory.getQuantity() + item.getQuantity();
            inventory.setQuantity(updatedQuantity);

            // Save the updated entity state back to the database
            inventoryRepository.save(inventory);
        }
    }


}