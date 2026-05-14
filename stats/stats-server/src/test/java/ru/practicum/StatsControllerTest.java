package ru.practicum;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.stats.EndpointHitRequestDto;
import ru.practicum.dto.stats.EndpointHitResponseDto;
import ru.practicum.dto.stats.ViewStatsDto;
import ru.practicum.server.StatsController;
import ru.practicum.server.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private StatsService statsService;

    @Test
    void shouldCreateHit() throws Exception {
        EndpointHitRequestDto requestDto = new EndpointHitRequestDto(
                "ewm-main-service",
                "/events",
                "192.168.0.1",
                LocalDateTime.of(2024, 1, 1, 12, 0, 0)
        );

        EndpointHitResponseDto responseDto = new EndpointHitResponseDto(
                1L,
                "ewm-main-service",
                "/events",
                "192.168.0.1",
                LocalDateTime.of(2024, 1, 1, 12, 0, 0)
        );

        when(statsService.create(any(EndpointHitRequestDto.class))).thenReturn(responseDto);

        mvc.perform(post("/hit")
                        .content(mapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.app", is("ewm-main-service")))
                .andExpect(jsonPath("$.uri", is("/events")))
                .andExpect(jsonPath("$.ip", is("192.168.0.1")));

        verify(statsService).create(any(EndpointHitRequestDto.class));
    }

    @Test
    void shouldGetStatsWithoutUrisAndUniqueFalse() throws Exception {
        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events", 10L)
        );

        when(statsService.getStats(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 1, 1, 23, 59, 59),
                null,
                false
        )).thenReturn(stats);

        mvc.perform(get("/stats")
                        .param("start", "2024-01-01 00:00:00")
                        .param("end", "2024-01-01 23:59:59")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].app", is("ewm-main-service")))
                .andExpect(jsonPath("$[0].uri", is("/events")))
                .andExpect(jsonPath("$[0].hits", is(10)));

        verify(statsService).getStats(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 1, 1, 23, 59, 59),
                null,
                false
        );
    }

    @Test
    void shouldGetStatsWithUniqueTrue() throws Exception {
        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events", 3L)
        );

        when(statsService.getStats(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 1, 1, 23, 59, 59),
                null,
                true
        )).thenReturn(stats);

        mvc.perform(get("/stats")
                        .param("start", "2024-01-01 00:00:00")
                        .param("end", "2024-01-01 23:59:59")
                        .param("unique", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits", is(3)));

        verify(statsService).getStats(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 1, 1, 23, 59, 59),
                null,
                true
        );
    }

    @Test
    void shouldGetStatsWithUris() throws Exception {
        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events", 5L),
                new ViewStatsDto("ewm-main-service", "/events/1", 2L)
        );

        when(statsService.getStats(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 1, 1, 23, 59, 59),
                List.of("/events", "/events/1"),
                false
        )).thenReturn(stats);

        mvc.perform(get("/stats")
                        .param("start", "2024-01-01 00:00:00")
                        .param("end", "2024-01-01 23:59:59")
                        .param("uris", "/events", "/events/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].uri", is("/events")))
                .andExpect(jsonPath("$[0].hits", is(5)))
                .andExpect(jsonPath("$[1].uri", is("/events/1")))
                .andExpect(jsonPath("$[1].hits", is(2)));

        verify(statsService).getStats(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 1, 1, 23, 59, 59),
                List.of("/events", "/events/1"),
                false
        );
    }

    @Test
    void shouldReturnBadRequestWhenStartIsMissing() throws Exception {
        mvc.perform(get("/stats")
                        .param("end", "2024-01-01 23:59:59")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(statsService, never()).getStats(any(), any(), any(), anyBoolean());
    }

    @Test
    void shouldReturnBadRequestWhenEndIsMissing() throws Exception {
        mvc.perform(get("/stats")
                        .param("start", "2024-01-01 00:00:00")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(statsService, never()).getStats(any(), any(), any(), anyBoolean());
    }

    @Test
    void shouldReturnBadRequestWhenStartFormatIsInvalid() throws Exception {
        mvc.perform(get("/stats")
                        .param("start", "2025")
                        .param("end", "2024-01-01 23:59:59")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(statsService, never()).getStats(any(), any(), any(), anyBoolean());
    }

    @Test
    void shouldReturnBadRequestWhenEndFormatIsInvalid() throws Exception {
        mvc.perform(get("/stats")
                        .param("start", "2025")
                        .param("end", "time")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(statsService, never()).getStats(any(), any(), any(), anyBoolean());
    }
}