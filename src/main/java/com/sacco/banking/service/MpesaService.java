package com.sacco.banking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpesaService {

    @Value("${mpesa.api-url}")
    private String mpesaApiUrl;

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    private final WebClient webClient;

    public String sendMoney(String phoneNumber, BigDecimal amount, String description) {
        try {
            // Get access token
            String accessToken = getAccessToken();

            // Prepare STK push request
            Map<String, Object> stkPushRequest = prepareStkPushRequest(phoneNumber, amount, description);

            // Make STK push request
            Map<String, Object> response = webClient.post()
                    .uri(mpesaApiUrl + "/mpesa/stkpush/v1/processrequest")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(stkPushRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && "0".equals(response.get("ResponseCode"))) {
                return (String) response.get("CheckoutRequestID");
            } else {
                throw new RuntimeException("M-Pesa transaction failed: " + response);
            }

        } catch (Exception e) {
            log.error("M-Pesa transaction failed", e);
            throw new RuntimeException("M-Pesa transaction failed: " + e.getMessage());
        }
    }

    private String getAccessToken() {
        String credentials = consumerKey + ":" + consumerSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        Map<String, Object> response = webClient.get()
                .uri(mpesaApiUrl + "/oauth/v1/generate?grant_type=client_credentials")
                .header("Authorization", "Basic " + encodedCredentials)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }

    private Map<String, Object> prepareStkPushRequest(String phoneNumber, BigDecimal amount, String description) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String password = Base64.getEncoder().encodeToString(
                (shortcode + passkey + timestamp).getBytes()
        );

        Map<String, Object> request = new HashMap<>();
        request.put("BusinessShortCode", shortcode);
        request.put("Password", password);
        request.put("Timestamp", timestamp);
        request.put("TransactionType", "CustomerPayBillOnline");
        request.put("Amount", amount.intValue());
        request.put("PartyA", phoneNumber);
        request.put("PartyB", shortcode);
        request.put("PhoneNumber", phoneNumber);
        request.put("CallBackURL", "https://your-callback-url.com/mpesa/callback");
        request.put("AccountReference", "SACCO-" + UUID.randomUUID().toString().substring(0, 8));
        request.put("TransactionDesc", description);

        return request;
    }
}