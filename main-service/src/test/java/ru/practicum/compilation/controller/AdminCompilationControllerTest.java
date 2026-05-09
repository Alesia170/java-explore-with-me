package ru.practicum.compilation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.compilation.CompilationService;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCompilationController.class)
class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    @Test
    void shouldCreateCompilationThenReturn201() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Лучшие события");
        request.setPinned(true);
        request.setEvents(List.of(1L, 2L));

        CompilationDto response = new CompilationDto();
        response.setId(1L);
        response.setTitle("Лучшие события");
        response.setPinned(true);
        response.setEvents(List.of());

        when(compilationService.createCompilation(any(NewCompilationDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Лучшие события"))
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    void shouldCreateCompilationWhenInvalidRequestThenReturn400() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("");
        request.setPinned(true);
        request.setEvents(List.of());

        mockMvc.perform(post("/admin/compilations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateCompilationWhenConflictThenReturn409() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Лучшие события");
        request.setPinned(true);
        request.setEvents(List.of());

        when(compilationService.createCompilation(any(NewCompilationDto.class)))
                .thenThrow(new ConflictException("Подборка с таким заголовком уже существует"));

        mockMvc.perform(post("/admin/compilations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldDeleteCompilationThenReturn204() throws Exception {
        mockMvc.perform(delete("/admin/compilations/{compId}", 1L))
                .andExpect(status().isNoContent());

        verify(compilationService).deleteCompilation(1L);
    }

    @Test
    void shouldDeleteCompilationWhenCompilationNotFoundThenReturn404() throws Exception {
        doThrow(new NotFoundException("Подборка не найдена"))
                .when(compilationService).deleteCompilation(1L);

        mockMvc.perform(delete("/admin/compilations/{compId}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteCompilationWhenInvalidCompIdThenReturn400() throws Exception {
        mockMvc.perform(delete("/admin/compilations/{compId}", 0L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateCompilationThenReturn200() throws Exception {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Обновленная подборка");
        request.setPinned(false);
        request.setEvents(List.of(1L));

        CompilationDto response = new CompilationDto();
        response.setId(1L);
        response.setTitle("Обновленная подборка");
        response.setPinned(false);
        response.setEvents(List.of());

        when(compilationService.updateCompilation(eq(1L), any(UpdateCompilationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Обновленная подборка"))
                .andExpect(jsonPath("$.pinned").value(false));
    }

    @Test
    void shouldUpdateCompilationWhenInvalidCompIdThenReturn400() throws Exception {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Обновленная подборка");

        mockMvc.perform(patch("/admin/compilations/{compId}", 0L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateCompilationWhenCompilationNotFoundThenReturn404() throws Exception {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Обновленная подборка");

        when(compilationService.updateCompilation(eq(1L), any(UpdateCompilationRequest.class)))
                .thenThrow(new NotFoundException("Подборка не найдена"));

        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateCompilationWhenConflictThenReturn409() throws Exception {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Подборка");

        when(compilationService.updateCompilation(eq(1L), any(UpdateCompilationRequest.class)))
                .thenThrow(new ConflictException("Подборка с таким заголовком уже существует"));

        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
