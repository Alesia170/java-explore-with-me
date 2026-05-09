package ru.practicum.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() {
        category1 = new Category();
        category1.setId(1L);
        category1.setName("Концерты");

        category2 = new Category();
        category2.setId(2L);
        category2.setName("Вечеринки");

        NewCategoryDto categoryDto = new NewCategoryDto();
        categoryDto.setName("Дискотеки");
    }

    @Test
    void shouldReturnEmptyListWhenCategoriesNotFound() {
        when(categoryRepository.findAllWithOffset(0, 10))
                .thenReturn(List.of());

        List<CategoryDto> result = categoryService.getCategories(0, 10);

        assertThat(result).isEmpty();

        verify(categoryRepository).findAllWithOffset(0, 10);
    }

    @Test
    void shouldReturnAllCategoriesAndGetById() {
        when(categoryRepository.findAllWithOffset(0, 10))
                .thenReturn(List.of(category1, category2));

        List<CategoryDto> result = categoryService.getCategories(0, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("Вечеринки");
    }

    @Test
    void shouldThrowNotFoundWhenCategoryNotFound() {
        Long catId = 99L;
        when(categoryRepository.findById(catId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(catId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Категория c id = " + catId + " не найдена или недоступна");

        verify(categoryRepository).findById(catId);
    }

    @Test
    void shouldCreateCategoryWhenNameIsUnique() {
        NewCategoryDto newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("Дискотеки");

        Category savedCategory = new Category();
        savedCategory.setId(3L);
        savedCategory.setName("Дискотеки");

        when(categoryRepository.existsByName(savedCategory.getName()))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(savedCategory);

        CategoryDto result = categoryService.createCategory(newCategoryDto);

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo("Дискотеки");

        verify(categoryRepository).existsByName("Дискотеки");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void shouldThrowConflictWhenCategoryNameAlreadyExists() {
        NewCategoryDto newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("Концерты");

        when(categoryRepository.existsByName("Концерты"))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(newCategoryDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Категория с именем " + newCategoryDto.getName() + " уже существует");

        verify(categoryRepository).existsByName("Концерты");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldDeleteCategory() {
        Long catId = 99L;

        when(categoryRepository.findById(catId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(catId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Категория c id = " + catId + " не найдена или недоступна");

        verify(categoryRepository).findById(catId);
        verify(eventRepository, never()).existsByCategoryId(anyLong());
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void shouldThrowConflictWhenCategoryHasEvents() {
        Long catId = 1L;

        when(categoryRepository.findById(catId))
                .thenReturn(Optional.of(category1));

        when(eventRepository.existsByCategoryId(catId))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(catId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Существуют события, связанные с категорией");

        verify(categoryRepository).findById(catId);
        verify(eventRepository).existsByCategoryId(catId);
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void shouldDeleteCategoryWhenCategoryExistsAndHasNoEvents() {
        Long catId = 1L;

        when(categoryRepository.findById(catId))
                .thenReturn(Optional.of(category1));

        when(eventRepository.existsByCategoryId(catId))
                .thenReturn(false);

        categoryService.deleteCategory(catId);

        verify(categoryRepository).findById(catId);
        verify(eventRepository).existsByCategoryId(catId);
        verify(categoryRepository).deleteById(catId);
    }
}
