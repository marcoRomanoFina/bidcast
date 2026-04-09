package com.bidcast.venue_service.venue;

import com.bidcast.venue_service.venue.dto.CreateVenueRequest;
import com.bidcast.venue_service.venue.dto.VenueResponse;

public class VenueMapper {

    public static Venue fromCreateRequest(CreateVenueRequest request) {
        return Venue.builder()
                .name(request.getName())
                .ownerId(request.getOwnerId())
                .category(request.getCategory())
                .build();
    }

    public static VenueResponse toResponse(Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .ownerId(venue.getOwnerId())
                .category(venue.getCategory())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();
    }
}
