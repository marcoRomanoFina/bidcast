package com.bidcast.device_service.device;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bidcast.device_service.device.dto.CreateDeviceRequest;
import com.bidcast.device_service.device.dto.DeviceResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/devices")
public class DeviceController {
    
    
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService){
        this.deviceService = deviceService;
    }

    @PostMapping()
    @ResponseStatus(value = HttpStatus.CREATED)
    public DeviceResponse createDevice(@Valid @RequestBody CreateDeviceRequest request){
    
        return deviceService.createDevice(request);
    }

    @
    GetMapping("/{id}")
    public DeviceResponse getDeviceById(@PathVariable UUID id){
        return deviceService.getDeviceById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeviceById(@PathVariable UUID id){
        deviceService.deleteDevice(id);
    }

    @GetMapping("/owner/{ownerId}")
    public List<DeviceResponse> getDevicesByOwner(@PathVariable UUID ownerId) {
        return deviceService.getDevicesByOwner(ownerId);
    }

}
