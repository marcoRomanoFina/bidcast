package com.bidcast.device_service.device;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bidcast.device_service.device.dto.CreateDeviceRequest;
import com.bidcast.device_service.device.dto.DeviceResponse;
import com.bidcast.device_service.exception.DeviceNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DeviceService {
    
    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository){
        this.deviceRepository = deviceRepository;
    }

    /**
     * Crea un dispositivo y lo persiste en la base de datos.
     */
    public DeviceResponse createDevice(CreateDeviceRequest request){
        log.info("Starting device creation. OwnerID: {}, Name: {}", request.getOwnerId(), request.getDeviceName());
        Device device = DeviceMapper.fromCreateRequest(request);
        Device saved = deviceRepository.save(device);
        log.info("Device created. DeviceID: {}, OwnerID: {}", saved.getId(), saved.getOwnerId());
        return DeviceMapper.toResponse(saved);
    }

    /**
     * Obtiene todos los dispositivos asociados a un owner.
     */
    public List<DeviceResponse> getDevicesByOwner(UUID ownerId){
        log.info("Searching devices by owner. OwnerID: {}", ownerId);
        List<Device> devices = deviceRepository.findByOwnerId(ownerId);
        log.info("Devices found for owner. OwnerID: {}, Count: {}", ownerId, devices.size());
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
                return new DeviceNotFoundException("Device not found: " + deviceId);
            });
        log.info("Device found. DeviceID: {}, OwnerID: {}", device.getId(), device.getOwnerId());
        return DeviceMapper.toResponse(device);
    }

    /**
     * Elimina un dispositivo por ID.
     * Lanza {@link com.bidcast.device_service.exception.DeviceNotFoundException}
     * si no existe.
     */
    public void deleteDevice(UUID deviceId){
        log.info("Deleting device by ID. DeviceID: {}", deviceId);
        if (!deviceRepository.existsById(deviceId)) {
            log.warn("Cannot delete: device not found. DeviceID: {}", deviceId);
            throw new DeviceNotFoundException("Device not found: " + deviceId);
        }
        deviceRepository.deleteById(deviceId);
        log.info("Device deleted. DeviceID: {}", deviceId);
    }

}
