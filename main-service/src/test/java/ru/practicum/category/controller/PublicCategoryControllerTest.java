package ru.practicum.category.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.category.CategoryService;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.exception.NotFoundException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCategoryController.class)
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void shouldGetCategoriesThenReturn200() throws Exception {
        CategoryDto category = new CategoryDto();
        category.setId(1L);
        category.setName("Концерты");

        when(categoryService.getCategories(0, 10))
                .thenReturn(List.of(category));

        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Концерты"));
    }

    @Test
    void shouldGetCategoriesWhenInvalidFromThenReturn400() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCategoriesWhenInvalidSizeThenReturn400() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCategoryWhenCategoryExistsThenReturn200() throws Exception {
        CategoryDto category = new CategoryDto();
        category.setId(1L);
        category.setName("Концерты");

        when(categoryService.getCategory(1L))
                .thenReturn(category);

        mockMvc.perform(get("/categories/{catId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Концерты"));
    }

    @Test
    void shouldGetCategoryWhenCategoryNotFoundThenReturn404() throws Exception {
        when(categoryService.getCategory(1L))
                .thenThrow(new NotFoundException("Категория не найдена"));

        mockMvc.perform(get("/categories/{catId}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetCategoryWhenInvalidCatIdThenReturn400() throws Exception {
        mockMvc.perform(get("/categories/{catId}", 0L))
                .andExpect(status().isBadRequest());
    }
}