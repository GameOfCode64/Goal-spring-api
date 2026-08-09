package com.tracker.backend.controller;

import com.tracker.backend.dto.TrackingTickRequest;
import com.tracker.backend.dto.TrackingTickResponse;
import com.tracker.backend.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
public class TrackingController {


    private  TrackingService trackingService;

    /**
     * Called by Electron/React Native/browser clients once per CLOSED
     * interval (app/tab switch), not on every raw poll tick.
     */
    @PostMapping("/tick")
    public ResponseEntity<TrackingTickResponse> submitTick(@Valid @RequestBody TrackingTickRequest request) {
        TrackingTickResponse response = trackingService.processTick(request);
        return ResponseEntity.ok(response);
    }
}


