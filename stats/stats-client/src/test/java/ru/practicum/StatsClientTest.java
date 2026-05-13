package ru.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import ru.practicum.dto.stats.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class StatsClientTest {

    private StatsClient statsClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        statsClient = new StatsClient("http://localhost:9090", "ewm-main-service", builder);
        mockServer = MockRestServiceServer.bindTo(statsClient.rest).build();
    }

    @Test
    void shouldCreateHit() {
        String responseJson = "{"
                              + "\"id\":1,"
                              + "\"app\":\"ewm-main-service\","
                              + "\"uri\":\"/events/1\","
                              + "\"ip\":\"192.168.0.1\","
                              + "\"timestamp\":\"2024-01-10 12:30:00\""
                              + "}";

        mockServer.expect(requestTo("http://localhost:9090/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, "application/json"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseJson));

        ResponseEntity<Object> response = statsClient.create("/events/1", "192.168.0.1");

        assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(response.getBody(), notNullValue());

        mockServer.verify();
    }

    @Test
    void shouldGetStatsWithUris() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 2, 0, 0, 0);

        String expectedUrl = "http://localhost:9090/stats" +
                             "?start=2024-01-01%2000:00:00" +
                             "&end=2024-01-02%2000:00:00" +
                             "&unique=true" +
                             "&uris=/events/1" +
                             "&uris=/events/2";

        String responseJson = "["
                              + "{"
                              + "\"app\":\"ewm-main-service\","
                              + "\"uri\":\"/events/1\","
                              + "\"hits\":5"
                              + "},"
                              + "{"
                              + "\"app\":\"ewm-main-service\","
                              + "\"uri\":\"/events/2\","
                              + "\"hits\":3"
                              + "}"
                              + "]";

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        ResponseEntity<List<ViewStatsDto>> response = statsClient.getStats(start, end, List.of("/events/1", "/events/2"), true);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), notNullValue());

        mockServer.verify();
    }

    @Test
    void shouldGetStatsWithoutUris() {
        LocalDateTime start = LocalDateTime.of(2024, 2, 1, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 2, 2, 10, 0, 0);

        String expectedUrl = "http://localhost:9090/stats" +
                             "?start=2024-02-01%2010:00:00" +
                             "&end=2024-02-02%2010:00:00" +
                             "&unique=false";

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        ResponseEntity<List<ViewStatsDto>> response = statsClient.getStats(start, end, null, false);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), notNullValue());

        mockServer.verify();
    }

    @Test
    void shouldThrowBadRequestWhenServerReturnsError() {
        LocalDateTime start = LocalDateTime.of(2024, 3, 2, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 3, 1, 10, 0, 0);

        String expectedUrl = "http://localhost:9090/stats" +
                             "?start=2024-03-02%2010:00:00" +
                             "&end=2024-03-01%2010:00:00" +
                             "&unique=false";

        String errorBody = "{\"error\":\"start must be before end\"}";

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> statsClient.getStats(start, end, null, false)
        );

        assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(exception.getResponseBodyAsString(), equalTo(errorBody));

        mockServer.verify();
    }
}