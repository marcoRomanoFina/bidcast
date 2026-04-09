package com.bidcast.advertisement_service.campaign.dto;

import java.util.UUID;

import com.bidcast.advertisement_service.campaign.Creative;

public record CreativeResponse(
        UUID id,
        String name,
        String mediaUrl,
        String clickUrl
) {
    public static CreativeResponse from(Creative creative) {
        return new CreativeResponse(
                creative.getId(),
                creative.getName(),
                creative.getMediaUrl(),
                creative.getClickUrl()
        );
    }
}
