package com.bidcast.advertisement_service.campaign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;


public record CampaignRequest(
    @NotBlank(message = "El nombre no puede estar vacío") 
    String name,

    @NotNull @Positive BigDecimal budget,
    @NotNull @Positive BigDecimal bidCpm 
) {}