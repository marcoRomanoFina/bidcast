package com.bidcast.wallet_service.wallet;

import com.bidcast.wallet_service.core.exception.ApiErrorResponse;
import com.bidcast.wallet_service.wallet.dto.WalletResponse;
import com.bidcast.wallet_service.wallet.dto.WalletFreezeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "HTTP operations for wallets and reserved funds")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    @Operation(
            summary = "Get authenticated advertiser wallet",
            description = "Returns the current wallet state identified by the `X-User-Id` header propagated by the gateway."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wallet found",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"error":"Wallet not found for owner 8c44f4b5-cd54-4fe6-b1a8-f617e9f0f807"}
                                    """)
                    )
            )
    })
    public ResponseEntity<WalletResponse> getMyWallet(@RequestHeader("X-User-Id") UUID ownerId) {
        Wallet wallet = walletService.getWalletByOwner(ownerId, WalletOwnerType.ADVERTISER);
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    @PostMapping("/freeze")
    @Operation(
            summary = "Freeze wallet funds",
            description = "Reserves part of the advertiser's available balance for a pending operation, such as an auction session."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funds frozen successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payload",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {"advertiserId":"Advertiser ID is required","amount":"Amount must be positive","referenceId":"Reference ID is required"}
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"error":"Wallet not found for owner 8c44f4b5-cd54-4fe6-b1a8-f617e9f0f807"}
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Insufficient balance or invalid domain rule",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"error":"Insufficient balance in wallet 8df9ae85-2e55-4c69-a3b8-dac6f8d4e4fd"}
                                    """)
                    )
            )
    })
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
    @Operation(
            summary = "Unfreeze wallet funds",
            description = "Releases previously reserved funds and returns them to the advertiser's available balance."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funds unfrozen successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payload",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {"advertiserId":"Advertiser ID is required","amount":"Amount must be positive","referenceId":"Reference ID is required"}
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wallet not found",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"error":"Wallet not found for owner 8c44f4b5-cd54-4fe6-b1a8-f617e9f0f807"}
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Insufficient frozen balance or invalid domain rule",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"error":"Insufficient frozen balance"}
                                    """)
                    )
            )
    })
    public ResponseEntity<Void> unfreeze(@RequestBody @Valid WalletFreezeRequest request) {
        walletService.unfreeze(
                UUID.fromString(request.advertiserId()),
                WalletOwnerType.ADVERTISER,
                request.amount()
        );
        return ResponseEntity.ok().build();
    }
}
