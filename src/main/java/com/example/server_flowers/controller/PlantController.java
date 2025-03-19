package com.example.server_flowers.controller;

import com.example.server_flowers.PlantWebSocketHandler;
import com.example.server_flowers.model.Flower;
import com.example.server_flowers.service.PlantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/plants")
public class PlantController {

    private static final Logger logger = LoggerFactory.getLogger(PlantController.class);

    private final PlantService plantService;
    private final PlantWebSocketHandler plantWebSocketHandler;

    @Autowired
    public PlantController(PlantService plantService, PlantWebSocketHandler plantWebSocketHandler) {
        this.plantService = plantService;
        this.plantWebSocketHandler = plantWebSocketHandler;
    }

    @GetMapping
    public ResponseEntity<List<Flower>> getAllPlants() {
        logger.info("Fetching all plants...");
        List<Flower> plants = plantService.getAllPlants();
        logger.info("Successfully fetched {} plants.", plants.size());
        return ResponseEntity.status(HttpStatus.OK).body(plants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Flower> getPlantById(@PathVariable String id) {
        logger.info("Fetching plant with ID: {}", id);
        Optional<Flower> plant = plantService.getPlantById(id);
        if (plant.isPresent()) {
            logger.info("Successfully fetched plant with ID: {}", id);
            return ResponseEntity.ok(plant.get());
        } else {
            logger.error("Plant with ID: {} not found.", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Flower> addPlant(@RequestBody Flower plant, @RequestHeader("session-id") String sessionId) {
        logger.info("Adding new plant with name: {} (Session ID: {})", plant.getName(), sessionId);
        Flower savedPlant = plantService.addPlant(plant);
        logger.info("Successfully added plant with ID: {}", savedPlant.getId());
        sendMessage("CREATE", savedPlant, sessionId);
        return ResponseEntity.status(HttpStatus.OK).body(savedPlant);
    }

    @PutMapping
    public ResponseEntity<Flower> updatePlant(@RequestBody Flower plant, @RequestHeader("session-id") String sessionId) {
        logger.info("Updating plant with ID: {} (Session ID: {})", plant.getId(), sessionId);
        try {
            Flower updatedPlant = plantService.updatePlant(plant);
            logger.info("Successfully updated plant with ID: {}", updatedPlant.getId());
            sendMessage("UPDATE", updatedPlant, sessionId);
            return ResponseEntity.ok(updatedPlant);
        } catch (RuntimeException e) {
            logger.error("Failed to update plant with ID: {}. Error: {}", plant.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlant(@PathVariable String id, @RequestHeader("session-id") String sessionId) {
        logger.info("Deleting plant with ID: {} (Session ID: {})", id, sessionId);
        try {
            plantService.deletePlant(id);
            logger.info("Successfully deleted plant with ID: {}", id);
            sendMessage("DELETE", id, sessionId);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (RuntimeException e) {
            logger.error("Failed to delete plant with ID: {}. Error: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private void sendMessage(String type, Object data, String sessionId) {
        String message = createMessage(type, data, sessionId);
        logger.info("Broadcasting message: {}", message);
        plantWebSocketHandler.broadcast(message);
    }

    private String createMessage(String type, Object data, String sessionId) {
        String message = String.format("{\"type\":\"%s\",\"data\":\"%s\",\"senderSessionId\":\"%s\"}", type, data, sessionId);
        logger.debug("Created WebSocket message: {}", message);
        return message;
    }
}
