package com.bidcast.billing_service.payment;

import com.bidcast.billing_service.payment.dto.PaymentPreferenceRequest;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentPreferenceService {

    private final PaymentRepository paymentRepository;
    private final PreferenceClient preferenceClient;

    @Value("${mercadopago.back-urls.success}")
    private String successUrl;

    @Value("${mercadopago.back-urls.failure}")
    private String failureUrl;

    @Value("${mercadopago.back-urls.pending}")
    private String pendingUrl;

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    public String createPaymentPreference(PaymentPreferenceRequest request) {
        log.info("Creating payment preference for advertiser: {}", request.advertiserId());

        // 1. Guardamos el registro de pago pendiente en nuestra DB
        // Nota: save() de JpaRepository ya es transaccional por defecto.
        Payment payment = Payment.builder()
                .advertiserId(request.advertiserId())
                .amount(request.amount())
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        try {
            
            // 3. Creamos el item (la recarga de saldo)
            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(payment.getId().toString()) // Referencia a nuestra DB
                    .title(request.description() != null ? request.description() : "Bidcast balance top-up")
                    .quantity(1)
                    .unitPrice(request.amount())
                    .currencyId("ARS")
                    .build();
            items.add(item);

            // 4. Configuramos URLs de retorno (Dashboard del anunciante)
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(successUrl)
                    .failure(failureUrl)
                    .pending(pendingUrl)
                    .build();

            // 5. Armamos la solicitud de preferencia
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(backUrls)
                    .externalReference(payment.getId().toString()) // Clave para el Webhook
                    .notificationUrl(notificationUrl) // URL pública para recibir el webhook
                    .build();

            // 6. Ejecutamos la creación en Mercado Pago
            Preference preference = preferenceClient.create(preferenceRequest);

            // 7. Actualizamos nuestro registro con el ID de preferencia de MP
            payment.setMpPreferenceId(preference.getId());
            paymentRepository.save(payment);

            log.info("Payment preference created successfully in Mercado Pago: {}", preference.getId());
            return preference.getInitPoint(); // Devolvemos la URL para que el usuario pague

        } catch (MPException | MPApiException e) {
            log.error("Error while creating Mercado Pago preference: {}", e.getMessage());
            throw new RuntimeException("Error while processing payment with Mercado Pago", e);
        }
    }
}
