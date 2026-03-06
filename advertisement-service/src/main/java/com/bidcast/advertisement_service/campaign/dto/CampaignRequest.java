package com.bidcast.advertisement_service.campaign.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CampaignRequest(

    @NotBlank(message = "El nombre de la campaña no puede estar vacío")
    String name,
    
    @NotNull(message = "El presupuesto es obligatorio")
    @Positive(message = "El presupuesto debe ser mayor a cero")
    BigDecimal budget
) {}