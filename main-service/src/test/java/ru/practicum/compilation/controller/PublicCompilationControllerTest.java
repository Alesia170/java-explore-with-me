package ru.practicum.compilation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.compilation.CompilationService;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.exception.NotFoundException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCompilationController.class)
class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    @Test
    void shouldGetCompilationsThenReturn200() throws Exception {
        CompilationDto compilation = new CompilationDto();
        compilation.setId(1L);
        compilation.setTitle("Лучшие события");
        compilation.setPinned(true);
        compilation.setEvents(List.of());

        when(compilationService.getCompilations(true, 0, 10))
                .thenReturn(List.of(compilation));

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Лучшие события"))
                .andExpect(jsonPath("$[0].pinned").value(true));
    }

    @Test
    void shouldGetCompilationsWhenPinnedIsAbsentThenReturn200() throws Exception {
        CompilationDto compilation = new CompilationDto();
        compilation.setId(1L);
        compilation.setTitle("Лучшие события");
        compilation.setPinned(false);
        compilation.setEvents(List.of());

        when(compilationService.getCompilations(null, 0, 10))
                .thenReturn(List.of(compilation));

        mockMvc.perform(get("/compilations")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Лучшие события"));
    }

    @Test
    void shouldGetCompilationsWhenInvalidFromThenReturn400() throws Exception {
        mockMvc.perform(get("/compilations")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCompilationsWhenInvalidSizeThenReturn400() throws Exception {
        mockMvc.perform(get("/compilations")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCompilationWhenCompilationExistsThenReturn200() throws Exception {
        CompilationDto compilation = new CompilationDto();
        compilation.setId(1L);
        compilation.setTitle("Лучшие события");
        compilation.setPinned(true);
        compilation.setEvents(List.of());

        when(compilationService.getCompilation(1L))
                .thenReturn(compilation);

        mockMvc.perform(get("/compilations/{compId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Лучшие события"))
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    void shouldGetCompilationWhenCompilationNotFoundThenReturn404() throws Exception {
        when(compilationService.getCompilation(1L))
                .thenThrow(new NotFoundException("Подборка не найдена"));

        mockMvc.perform(get("/compilations/{compId}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetCompilationWhenInvalidCompIdThenReturn400() throws Exception {
        mockMvc.perform(get("/compilations/{compId}", 0L))
                .andExpect(status().isBadRequest());
    }
}