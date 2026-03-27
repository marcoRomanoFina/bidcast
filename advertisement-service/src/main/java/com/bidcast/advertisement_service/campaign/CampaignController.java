package com.bidcast.advertisement_service.campaign;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bidcast.advertisement_service.campaign.dto.CampaignRequest;
import com.bidcast.advertisement_service.campaign.dto.CampaignResponse;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Slf4j
public class CampaignController {

    private final CampaignService campaignService;

    
    @PostMapping
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CampaignRequest body, @RequestHeader("X-User-Id") UUID advertiserId) {
        log.info("Campaign creation request received: advertiserId={}, name={}", advertiserId, body.name());
        Campaign savedCampaign = campaignService.createCampaign(advertiserId,body);
        return new ResponseEntity<>(CampaignResponse.from(savedCampaign), HttpStatus.CREATED);
    }
}
