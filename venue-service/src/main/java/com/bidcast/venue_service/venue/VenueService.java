package com.bidcast.venue_service.venue;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bidcast.venue_service.exception.ResourceNotFoundException;
import com.bidcast.venue_service.venue.dto.CreateVenueRequest;
import com.bidcast.venue_service.venue.dto.VenueResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Transactional
    public VenueResponse createVenue(CreateVenueRequest request) {
        log.info("Creating venue: {}", request.getName());
        Venue venue = VenueMapper.fromCreateRequest(request);
        Venue saved = venueRepository.save(venue);
        log.info("Venue created with ID: {}", saved.getId());
        return VenueMapper.toResponse(saved);
    }

    public VenueResponse getVenueById(UUID id) {
        log.info("Fetching venue with ID: {}", id);
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + id));
        return VenueMapper.toResponse(venue);
    }

    public List<VenueResponse> getVenuesByOwner(UUID ownerId) {
        log.info("Fetching venues for owner: {}", ownerId);
        return venueRepository.findByOwnerId(ownerId).stream()
                .map(VenueMapper::toResponse)
                .toList();
    }

    public Venue getVenueEntityById(UUID id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + id));
    }
}
