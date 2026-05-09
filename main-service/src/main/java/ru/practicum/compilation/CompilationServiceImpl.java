package ru.practicum.compilation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        List<Compilation> compilations = compilationRepository.findAllWithOffset(pinned, from, size);

        log.info("Найдено {} подборок событий", compilations.size());

        return compilations.stream()
                .map(CompilationMapper::toCompilationDto)
                .toList();
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = getCompilationOrThrow(compId);

        log.info("Подборка с id = {} найдена", compId);

        return CompilationMapper.toCompilationDto(compilation);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        if (compilationRepository.existsByTitle(dto.getTitle())) {
            log.warn("Попытка создать подборку с уже существующем заголовком: {}", dto.getTitle());
            throw new ConflictException("Подборка с таким заголовком уже существует");
        }

        Set<Event> events = getEvents(dto.getEvents());

        Compilation compilation = CompilationMapper.toCompilation(dto, events);
        Compilation savedCompilation = compilationRepository.save(compilation);

        log.info("Создана подборка с id = {}", savedCompilation.getId());

        return CompilationMapper.toCompilationDto(savedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = getCompilationOrThrow(compId);

        compilationRepository.delete(compilation);

        log.info("Подборка с id = {} удалена", compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = getCompilationOrThrow(compId);

        if (request.getTitle() != null) {
            compilationRepository.findByTitle(request.getTitle())
                    .ifPresent(existingTitle -> {
                        if (!existingTitle.getId().equals(compId)) {
                            log.warn("Попытка обновить подборку с уже существующем заголовком: {}",
                                    existingTitle.getTitle());
                            throw new ConflictException("Подборка с таким заголовком уже существует");
                        }
                    });
            compilation.setTitle(request.getTitle());
        }

        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }

        if (request.getEvents() != null) {
            Set<Event> events = getEvents(request.getEvents());
            compilation.setEvents(events);
        }

        Compilation updateCompilation = compilationRepository.save(compilation);

        log.info("Подборка с id = {} обновлена", updateCompilation.getId());

        return CompilationMapper.toCompilationDto(updateCompilation);
    }

    private Compilation getCompilationOrThrow(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> {
                    log.warn("Подборка с id = {} не найдена", compId);
                    return new NotFoundException("Подборка не найдена или недоступна");
                });

    }

    private Set<Event> getEvents(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> uniqueEventIds = new HashSet<>(eventIds);

        List<Event> events = eventRepository.findAllById(uniqueEventIds);

        if (events.size() != uniqueEventIds.size()) {
            Set<Long> foundIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());

            Set<Long> missingIds = new HashSet<>(uniqueEventIds);
            missingIds.removeAll(foundIds);

            log.warn("Не найдены события с id: {}", missingIds);

            throw new NotFoundException("События не найдены: " + missingIds);
        }
        return new HashSet<>(events);
    }
}
