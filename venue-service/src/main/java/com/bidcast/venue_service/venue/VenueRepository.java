package com.bidcast.venue_service.venue;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {
    List<Venue> findByOwnerId(UUID ownerId);
}
