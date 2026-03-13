package com.bidcast.auction_service.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, String> {
}
