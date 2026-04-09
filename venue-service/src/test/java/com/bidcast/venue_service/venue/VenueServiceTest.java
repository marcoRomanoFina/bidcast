package com.bidcast.venue_service.venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidcast.venue_service.exception.ResourceNotFoundException;
import com.bidcast.venue_service.venue.dto.CreateVenueRequest;
import com.bidcast.venue_service.venue.dto.VenueResponse;

class VenueServiceTest {

    private VenueRepository venueRepository;
    private VenueService venueService;

    @BeforeEach
    void setUp() {
        venueRepository = mock(VenueRepository.class);
        venueService = new VenueService(venueRepository);
    }

    @Test
    void createVenue_savesAndReturns() {
        UUID ownerId = UUID.randomUUID();
        VenueCategory category = VenueCategory.GYM;
        CreateVenueRequest request = CreateVenueRequest.builder()
                .name("Smart Fit")
                .ownerId(ownerId)
                .category(category)
                .build();

        Venue saved = Venue.builder()
                .id(UUID.randomUUID())
                .name("Smart Fit")
                .ownerId(ownerId)
                .category(category)
                .build();

        when(venueRepository.save(any(Venue.class))).thenReturn(saved);

        VenueResponse result = venueService.createVenue(request);

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getName()).isEqualTo("Smart Fit");
        assertThat(result.getCategory()).isEqualTo(VenueCategory.GYM);
        verify(venueRepository).save(any(Venue.class));
    }

    @Test
    void getVenueById_returnsVenueWhenFound() {
        UUID id = UUID.randomUUID();
        Venue venue = Venue.builder()
                .id(id)
                .name("Gym X")
                .ownerId(UUID.randomUUID())
                .category(VenueCategory.GYM)
                .build();

        when(venueRepository.findById(id)).thenReturn(Optional.of(venue));

        VenueResponse result = venueService.getVenueById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Gym X");
    }

    @Test
    void getVenueById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(venueRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.getVenueById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getVenuesByOwner_returnsList() {
        UUID ownerId = UUID.randomUUID();
        Venue v1 = Venue.builder().id(UUID.randomUUID()).ownerId(ownerId).name("V1").build();
        Venue v2 = Venue.builder().id(UUID.randomUUID()).ownerId(ownerId).name("V2").build();

        when(venueRepository.findByOwnerId(ownerId)).thenReturn(List.of(v1, v2));

        List<VenueResponse> result = venueService.getVenuesByOwner(ownerId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(VenueResponse::getName).containsExactly("V1", "V2");
    }
}
