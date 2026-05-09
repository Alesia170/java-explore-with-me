package ru.practicum.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        List<Category> categories = categoryRepository.findAllWithOffset(from, size);

        log.info("Найдено {} категорий", categories.size());

        return categories.stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        log.info("Получение категории по id = {}", catId);

        Category category = getCategoryOrThrow(catId);

        log.info("Категория найдена: id = {}, name = {}", category.getId(), category.getName());

        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto categoryDto) {
        if (categoryRepository.existsByName(categoryDto.getName())) {
            log.warn("Попытка создать категорию с уже существующей категорией");
            throw new ConflictException("Категория с именем " + categoryDto.getName() + " уже существует");
        }

        Category category = CategoryMapper.toCategory(categoryDto);
        Category savedCategory = categoryRepository.save(category);

        log.info("Категория создана");

        return CategoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        getCategoryOrThrow(catId);

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Существуют события, связанные с категорией");
        }

        categoryRepository.deleteById(catId);

        log.info("Категория c id = {} удалена", catId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, NewCategoryDto categoryDto) {
        Category category = getCategoryOrThrow(catId);

        if (categoryDto.getName() != null) {
            categoryRepository.findByName(categoryDto.getName())
                    .ifPresent(existingCategory -> {
                        if (!existingCategory.getId().equals(catId)) {
                            log.warn("Невозможно обновить имя, так как оно уже существует");
                            throw new ConflictException("Категория " + categoryDto.getName() + " уже существует");
                        }
                    });
            category.setName(categoryDto.getName());
        }

        Category updatedCategory = categoryRepository.save(category);

        log.info("Категория с id = {} обновлена", catId);

        return CategoryMapper.toCategoryDto(updatedCategory);
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.warn("Попытка получить несуществующую категорию");
                    return new NotFoundException("Категория c id = " + catId + " не найдена или недоступна");
                });
    }
}
