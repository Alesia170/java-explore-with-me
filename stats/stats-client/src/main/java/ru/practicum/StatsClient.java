package ru.practicum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.stats.EndpointHitRequestDto;
import ru.practicum.dto.stats.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatsClient extends BaseClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String serviceName;

    @Autowired
    public StatsClient(@Value("${stats-server.url}") String serverUrl,
                       @Value("${ewm.service.name}") String serviceName,
                       RestTemplateBuilder builder) {
        super(builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .build());
        this.serviceName = serviceName;
    }

    public ResponseEntity<Object> create(String uri, String ip) {
        EndpointHitRequestDto dto = new EndpointHitRequestDto(
                serviceName,
                uri,
                ip,
                LocalDateTime.now()
        );
        return post(dto);
    }

    public ResponseEntity<List<ViewStatsDto>> getStats(LocalDateTime start, LocalDateTime end,
                                                       List<String> uris, boolean unique) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                uriComponentsBuilder.queryParam("uris", uri);
            }
        }

        return getStats(uriComponentsBuilder.build().toUriString());
    }
}

