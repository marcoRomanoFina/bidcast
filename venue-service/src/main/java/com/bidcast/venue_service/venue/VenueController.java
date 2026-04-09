package com.bidcast.venue_service.venue;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bidcast.venue_service.venue.dto.CreateVenueRequest;
import com.bidcast.venue_service.venue.dto.VenueResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenueResponse createVenue(@Valid @RequestBody CreateVenueRequest request) {
        return venueService.createVenue(request);
    }

    @GetMapping("/{id}")
    public VenueResponse getVenueById(@PathVariable UUID id) {
        return venueService.getVenueById(id);
    }

    @GetMapping("/owner/{ownerId}")
    public List<VenueResponse> getVenuesByOwner(@PathVariable UUID ownerId) {
        return venueService.getVenuesByOwner(ownerId);
    }
}
