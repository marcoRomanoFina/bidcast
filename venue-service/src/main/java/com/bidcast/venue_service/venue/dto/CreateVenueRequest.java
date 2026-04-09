package com.bidcast.venue_service.venue.dto;

import java.util.UUID;

import com.bidcast.venue_service.venue.VenueCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVenueRequest {
    
    @NotBlank(message = "Venue name is required")
    private String name;

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    @NotNull(message = "Category is required")
    private VenueCategory category;
}
