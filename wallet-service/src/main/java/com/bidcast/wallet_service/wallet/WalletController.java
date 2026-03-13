package com.bidcast.wallet_service.wallet;

import com.bidcast.wallet_service.wallet.dto.WalletFreezeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/freeze")
    public ResponseEntity<Void> freeze(@RequestBody @Valid WalletFreezeRequest request) {
        walletService.freeze(
                UUID.fromString(request.advertiserId()),
                WalletOwnerType.ADVERTISER,
                request.amount(),
                request.referenceId()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unfreeze")
    public ResponseEntity<Void> unfreeze(@RequestBody @Valid WalletFreezeRequest request) {
        walletService.unfreeze(
                UUID.fromString(request.advertiserId()),
                WalletOwnerType.ADVERTISER,
                request.amount()
        );
        return ResponseEntity.ok().build();
    }
}
