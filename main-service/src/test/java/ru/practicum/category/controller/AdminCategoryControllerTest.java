package ru.practicum.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.category.CategoryService;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCategoryController.class)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    void shouldCreateCategoryThenReturn201() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Концерты");

        CategoryDto response = new CategoryDto();
        response.setId(1L);
        response.setName("Концерты");

        when(categoryService.createCategory(any(NewCategoryDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/admin/categories")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Концерты"));
    }

    @Test
    void shouldCreateCategoryWhenInvalidRequestThenReturn400() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("");

        mockMvc.perform(post("/admin/categories")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateCategoryWhenCategoryAlreadyExistsThenReturn409() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Концерты");

        when(categoryService.createCategory(any(NewCategoryDto.class)))
                .thenThrow(new ConflictException("Категория с таким названием уже существует"));

        mockMvc.perform(post("/admin/categories")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldDeleteCategoryWhenCategoryExistsThenReturn204() throws Exception {
        mockMvc.perform(delete("/admin/categories/{catId}", 1L))
                .andExpect(status().isNoContent());

        Mockito.verify(categoryService).deleteCategory(1L);
    }

    @Test
    void shouldDeleteCategoryWhenCategoryNotFoundThenReturn404() throws Exception {
        doThrow(new NotFoundException("Категория не найдена"))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/admin/categories/{catId}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteCategoryWhenCategoryHasEventsThenReturn409() throws Exception {
        doThrow(new ConflictException("Существуют события, связанные с категорией"))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/admin/categories/{catId}", 1L))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteCategoryWhenInvalidCatIdThenReturn400() throws Exception {
        mockMvc.perform(delete("/admin/categories/{catId}", 0L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateCategoryWhenValidRequestThenReturn200() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Театры");

        CategoryDto response = new CategoryDto();
        response.setId(1L);
        response.setName("Театры");

        when(categoryService.updateCategory(eq(1L), any(NewCategoryDto.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/admin/categories/{catId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Театры"));
    }

    @Test
    void shouldUpdateCategoryWhenInvalidCatIdThenReturn400() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Театры");

        mockMvc.perform(patch("/admin/categories/{catId}", 0L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateCategoryWhenCategoryNotFoundThenReturn404() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Театры");

        when(categoryService.updateCategory(eq(1L), any(NewCategoryDto.class)))
                .thenThrow(new NotFoundException("Категория не найдена"));

        mockMvc.perform(patch("/admin/categories/{catId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateCategoryWhenNameAlreadyExistsThenReturn409() throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName("Театры");

        when(categoryService.updateCategory(eq(1L), any(NewCategoryDto.class)))
                .thenThrow(new ConflictException("Категория с таким названием уже существует"));

        mockMvc.perform(patch("/admin/categories/{catId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
