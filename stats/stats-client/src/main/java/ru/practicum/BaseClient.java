package ru.practicum;

import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class BaseClient {
    protected final RestTemplate rest;

    public BaseClient(RestTemplate rest) {
        this.rest = rest;
    }

    protected ResponseEntity<Object> get(String path) {
        return makeAndSendRequest(HttpMethod.GET, path, null);
    }

    protected <T> ResponseEntity<Object> post(T body) {
        return makeAndSendRequest(HttpMethod.POST, "/hit", body);
    }

    private <T> ResponseEntity<Object> makeAndSendRequest(HttpMethod method,
                                                          String path,
                                                          @Nullable T body) {

        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders());

        try {
            ResponseEntity<Object> response = rest.exchange(path, method, requestEntity, Object.class);
            return prepareResponse(response);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private static ResponseEntity<Object> prepareResponse(ResponseEntity<Object> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response;
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.getStatusCode());
        return response.hasBody() ? builder.body(response.getBody()) : builder.build();
    }
}
