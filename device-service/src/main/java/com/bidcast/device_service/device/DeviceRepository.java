package com.bidcast.device_service.device;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device,UUID> {

    List<Device> findByOwnerId(UUID ownerId);
    
}
