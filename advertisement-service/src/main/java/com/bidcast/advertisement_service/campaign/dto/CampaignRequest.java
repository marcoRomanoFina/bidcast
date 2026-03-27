package com.bidcast.advertisement_service.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;


public record CampaignRequest(
    @NotBlank(message = "Name must not be blank") 
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotNull @Positive BigDecimal budget,
    @NotNull @Positive BigDecimal bidCpm 
) {}
