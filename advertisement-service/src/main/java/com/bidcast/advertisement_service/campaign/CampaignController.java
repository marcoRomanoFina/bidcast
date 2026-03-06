package com.bidcast.advertisement_service.campaign;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bidcast.advertisement_service.campaign.dto.CampaignRequest;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Slf4j
public class CampaignController {

    private final CampaignService campaignService;

    
    @PostMapping
    public ResponseEntity<Campaign> create(@Valid @RequestBody CampaignRequest body, @RequestHeader("X-Advertiser-Id") UUID advertiserId) {
        log.info("Solicitud de creacion de campana recibida: advertiserId={}, name={}", advertiserId, body.name());
        Campaign savedCampaign = campaignService.createCampaign(advertiserId,body);
        return new ResponseEntity<>(savedCampaign, HttpStatus.CREATED);
    }
}
