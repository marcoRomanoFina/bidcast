package com.bidcast.venue_service.device;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bidcast.venue_service.device.dto.CreateDeviceRequest;
import com.bidcast.venue_service.device.dto.DeviceResponse;
import com.bidcast.venue_service.exception.ResourceNotFoundException;
import com.bidcast.venue_service.venue.Venue;
import com.bidcast.venue_service.venue.VenueService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DeviceService {
    
    private final DeviceRepository deviceRepository;
    private final VenueService venueService;

    public DeviceService(DeviceRepository deviceRepository, VenueService venueService){
        this.deviceRepository = deviceRepository;
        this.venueService = venueService;
    }

    /**
     * Crea un dispositivo y lo asocia a un Venue.
     */
    @Transactional
    public DeviceResponse createDevice(CreateDeviceRequest request){
        log.info("Starting device creation. VenueID: {}, Name: {}", request.getVenueId(), request.getDeviceName());
        
        Venue venue = venueService.getVenueEntityById(request.getVenueId());
        
        Device device = DeviceMapper.fromCreateRequest(request);
        device.setVenue(venue);
        
        Device saved = deviceRepository.save(device);
        log.info("Device created. DeviceID: {}, VenueID: {}", saved.getId(), venue.getId());
        return DeviceMapper.toResponse(saved);
    }

    /**
     * Obtiene todos los dispositivos asociados a un Venue.
     */
    public List<DeviceResponse> getDevicesByVenue(UUID venueId){
        log.info("Searching devices by venue. VenueID: {}", venueId);
        List<Device> devices = deviceRepository.findByVenueId(venueId);
        log.info("Devices found for venue. VenueID: {}, Count: {}", venueId, devices.size());
        return devices.stream()
            .map(DeviceMapper::toResponse)
            .toList();
    }

    /**
     * Busca un dispositivo por ID.
     */
    public DeviceResponse getDeviceById(UUID deviceId){
        log.info("Searching device by ID. DeviceID: {}", deviceId);
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> {
                log.warn("Device not found. DeviceID: {}", deviceId);
                return new ResourceNotFoundException("Device not found: " + deviceId);
            });
        return DeviceMapper.toResponse(device);
    }

    /**
     * Elimina un dispositivo por ID.
     */
    @Transactional
    public void deleteDevice(UUID deviceId){
        log.info("Deleting device by ID. DeviceID: {}", deviceId);
        if (!deviceRepository.existsById(deviceId)) {
            log.warn("Cannot delete: device not found. DeviceID: {}", deviceId);
            throw new ResourceNotFoundException("Device not found: " + deviceId);
        }
        deviceRepository.deleteById(deviceId);
        log.info("Device deleted. DeviceID: {}", deviceId);
    }

}
