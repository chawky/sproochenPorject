package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.constants.AppConstants;
import com.nailic.sproochencoach.dto.ResendReceivedEmailDto;
import com.nailic.sproochencoach.exceptions.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class ResendReceivedEmailClient {
    private static final Logger log = LoggerFactory.getLogger(ResendReceivedEmailClient.class);

    private final RestClient resendRestClient;
    private final String apiKey;

    public ResendReceivedEmailClient(
            @Qualifier(AppConstants.RestClientBeans.RESEND) RestClient resendRestClient,
            @Value(AppConstants.PropertyPlaceholders.EMAIL_RESEND_API_KEY) String apiKey
    ) {
        this.resendRestClient = resendRestClient;
        this.apiKey = apiKey;
    }

    public ResendReceivedEmailDto getReceivedEmail(String emailId) {
        validateConfiguration();

        try {
            return resendRestClient.get()
                    .uri(AppConstants.ApiPaths.RESEND_RECEIVING_EMAIL, emailId)
                    .retrieve()
                    .body(ResendReceivedEmailDto.class);
        } catch (RestClientResponseException exception) {
            log.error(
                    "Resend received email retrieval failed. statusCode={}, emailId={}",
                    exception.getStatusCode().value(),
                    emailId,
                    exception
            );
            throw new EmailDeliveryException(
                    "Email provider rejected the received email request",
                    exception.getStatusCode().value(),
                    exception
            );
        } catch (RestClientException exception) {
            log.error("Resend received email retrieval failed. emailId={}", emailId, exception);
            throw new EmailDeliveryException(
                    "Email provider received email request failed",
                    HttpStatus.BAD_GATEWAY.value(),
                    exception
            );
        }
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmailDeliveryException(
                    "Resend API key is not configured",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }
}
