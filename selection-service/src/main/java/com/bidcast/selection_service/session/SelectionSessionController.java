package com.bidcast.selection_service.session;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/sessions")
@RequiredArgsConstructor
public class SelectionSessionController {

    private final SelectionSessionService selectionSessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void register(@RequestBody @Valid SelectionSessionRegistrationRequest request) {
        selectionSessionService.register(request);
    }
}
