package com.bidcast.venue_service.venue.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.bidcast.venue_service.venue.VenueCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueResponse {
    private UUID id;
    private String name;
    private UUID ownerId;
    private VenueCategory category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
