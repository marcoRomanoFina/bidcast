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
        log.info("Iniciando creación de dispositivo. OwnerID: {}, Nombre: {}", request.getOwnerId(), request.getDeviceName());
        Device device = DeviceMapper.fromCreateRequest(request);
        Device saved = deviceRepository.save(device);
        log.info("Dispositivo creado. DeviceID: {}, OwnerID: {}", saved.getId(), saved.getOwnerId());
        return DeviceMapper.toResponse(saved);
    }

    /**
     * Obtiene todos los dispositivos asociados a un owner.
     */
    public List<DeviceResponse> getDevicesByOwner(UUID ownerId){
        log.info("Buscando dispositivos por owner. OwnerID: {}", ownerId);
        List<Device> devices = deviceRepository.findByOwnerId(ownerId);
        log.info("Dispositivos encontrados para owner. OwnerID: {}, Cantidad: {}", ownerId, devices.size());
        return devices.stream()
            .map(DeviceMapper::toResponse)
            .toList();
    }

    /**
     * Busca un dispositivo por ID.
     * Lanza {@link com.bidcast.device_service.exception.DeviceNotFoundException}
     * si no existe.
     */
    public DeviceResponse getDeviceById(UUID deviceId){
        log.info("Buscando dispositivo por ID. DeviceID: {}", deviceId);
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> {
                log.warn("Dispositivo no encontrado. DeviceID: {}", deviceId);
                return new DeviceNotFoundException("Dispositivo no encontrado: " + deviceId);
            });
        log.info("Dispositivo encontrado. DeviceID: {}, OwnerID: {}", device.getId(), device.getOwnerId());
        return DeviceMapper.toResponse(device);
    }

    /**
     * Elimina un dispositivo por ID.
     * Lanza {@link com.bidcast.device_service.exception.DeviceNotFoundException}
     * si no existe.
     */
    public void deleteDevice(UUID deviceId){
        log.info("Eliminando dispositivo por ID. DeviceID: {}", deviceId);
        if (!deviceRepository.existsById(deviceId)) {
            log.warn("No se puede eliminar: dispositivo no encontrado. DeviceID: {}", deviceId);
            throw new DeviceNotFoundException("Dispositivo no encontrado: " + deviceId);
        }
        deviceRepository.deleteById(deviceId);
        log.info("Dispositivo eliminado. DeviceID: {}", deviceId);
    }

}
