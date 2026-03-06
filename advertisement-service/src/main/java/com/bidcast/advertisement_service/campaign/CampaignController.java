package com.bidcast.advertisement_service.campaign;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bidcast.advertisement_service.campaign.dto.CampaignRequest;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    
    @PostMapping
    public ResponseEntity<Campaign> create(@Valid @RequestBody CampaignRequest body, @RequestHeader("X-Advertiser-Id") UUID advertiserId) {
        Campaign savedCampaign = campaignService.createCampaign(advertiserId,body);
        return new ResponseEntity<>(savedCampaign, HttpStatus.CREATED);
    }
}