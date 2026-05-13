package ru.practicum.compilation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.category.Category;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private Event event;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Concerts");

        User user = new User();
        user.setId(1L);
        user.setName("Name");
        user.setEmail("email@mail.com");

        event = new Event();
        event.setId(1L);
        event.setTitle("Test event");
        event.setAnnotation("Test annotation");
        event.setCategory(category);
        event.setInitiator(user);
        event.setPaid(false);
        event.setEventDate(LocalDateTime.now().plusDays(1));
        event.setViews(0L);
        event.setConfirmedRequests(0);

        compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("Best events");
        compilation.setPinned(false);
        compilation.setEvents(Set.of(event));
    }

    @Test
    void shouldGetCompilationsWhenPinnedIsNull() {
        when(compilationRepository.findAllWithOffset(null, 0, 10))
                .thenReturn(List.of(compilation));

        List<CompilationDto> result = compilationService.getCompilations(null, 0, 10);

        assertEquals(1, result.size());
        assertEquals(compilation.getId(), result.getFirst().getId());
        assertEquals(compilation.getTitle(), result.getFirst().getTitle());

        verify(compilationRepository).findAllWithOffset(null, 0, 10);
    }

    @Test
    void shouldGetCompilationsWhenPinnedIsTrue() {
        compilation.setPinned(true);

        when(compilationRepository.findAllWithOffset(true, 0, 10))
                .thenReturn(List.of(compilation));

        List<CompilationDto> result = compilationService.getCompilations(true, 0, 10);

        assertEquals(1, result.size());
        assertTrue(result.getFirst().getPinned());

        verify(compilationRepository).findAllWithOffset(true, 0, 10);
    }

    @Test
    void shouldReturnEmptyListWhenCompilationsNotFound() {
        when(compilationRepository.findAllWithOffset(null, 0, 10))
                .thenReturn(List.of());

        List<CompilationDto> result = compilationService.getCompilations(null, 0, 10);

        assertTrue(result.isEmpty());

        verify(compilationRepository).findAllWithOffset(null, 0, 10);
    }

    @Test
    void shouldGetCompilationWhenCompilationExists() {
        when(compilationRepository.findById(compilation.getId()))
                .thenReturn(Optional.of(compilation));

        CompilationDto result = compilationService.getCompilation(compilation.getId());

        assertEquals(compilation.getId(), result.getId());
        assertEquals(compilation.getTitle(), result.getTitle());
        assertEquals(compilation.getPinned(), result.getPinned());

        verify(compilationRepository).findById(compilation.getId());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenCompilationNotFound() {
        when(compilationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> compilationService.getCompilation(99L));

        verify(compilationRepository).findById(99L);
    }

    @Test
    void shouldCreateCompilation() {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Best events");
        request.setPinned(true);
        request.setEvents(List.of(event.getId()));

        Compilation savedCompilation = new Compilation();
        savedCompilation.setId(1L);
        savedCompilation.setTitle(request.getTitle());
        savedCompilation.setPinned(request.getPinned());
        savedCompilation.setEvents(Set.of(event));

        when(eventRepository.findAllById(Set.of(event.getId())))
                .thenReturn(List.of(event));
        when(compilationRepository.saveAndFlush(any(Compilation.class)))
                .thenReturn(savedCompilation);

        CompilationDto result = compilationService.createCompilation(request);

        assertEquals(savedCompilation.getId(), result.getId());
        assertEquals(request.getTitle(), result.getTitle());
        assertEquals(request.getPinned(), result.getPinned());

        verify(eventRepository).findAllById(Set.of(event.getId()));
        verify(compilationRepository).saveAndFlush(any(Compilation.class));
    }

    @Test
    void shouldThrowConflictExceptionWhenTitleAlreadyExists() {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Best events");
        request.setPinned(false);
        request.setEvents(List.of(event.getId()));

        when(eventRepository.findAllById(Set.of(event.getId())))
                .thenReturn(List.of(event));

        when(compilationRepository.saveAndFlush(any(Compilation.class)))
                .thenThrow(new DataIntegrityViolationException("Подборка с таким заголовком уже существует"));

        assertThrows(ConflictException.class,
                () -> compilationService.createCompilation(request));

        verify(eventRepository).findAllById(Set.of(event.getId()));
        verify(compilationRepository).saveAndFlush(any(Compilation.class));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEventNotFound() {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Best events");
        request.setPinned(false);
        request.setEvents(List.of(1L, 2L));

        when(eventRepository.findAllById(Set.of(1L, 2L)))
                .thenReturn(List.of(event));

        assertThrows(NotFoundException.class,
                () -> compilationService.createCompilation(request));

        verify(eventRepository).findAllById(Set.of(1L, 2L));
        verify(compilationRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldCreateCompilationWithEmptyEventsWhenEventsAreNull() {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Empty compilation");
        request.setPinned(false);
        request.setEvents(null);

        Compilation savedCompilation = new Compilation();
        savedCompilation.setId(2L);
        savedCompilation.setTitle(request.getTitle());
        savedCompilation.setPinned(request.getPinned());
        savedCompilation.setEvents(Set.of());

        when(compilationRepository.saveAndFlush(any(Compilation.class)))
                .thenReturn(savedCompilation);

        CompilationDto result = compilationService.createCompilation(request);

        assertEquals(savedCompilation.getId(), result.getId());
        assertEquals(request.getTitle(), result.getTitle());
        assertTrue(result.getEvents().isEmpty());

        verify(eventRepository, never()).findAllById(any());
        verify(compilationRepository).saveAndFlush(any(Compilation.class));
    }

    @Test
    void shouldDeleteCompilation() {
        when(compilationRepository.findById(compilation.getId()))
                .thenReturn(Optional.of(compilation));

        compilationService.deleteCompilation(compilation.getId());

        verify(compilationRepository).findById(compilation.getId());
        verify(compilationRepository).delete(compilation);
    }

    @Test
    void deleteCompilationWhenCompilationNotFoundShouldThrowNotFoundException() {
        when(compilationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> compilationService.deleteCompilation(99L));

        verify(compilationRepository).findById(99L);
        verify(compilationRepository, never()).delete(any());
    }

    @Test
    void shouldUpdateCompilation() {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Updated title");
        request.setPinned(true);
        request.setEvents(List.of(event.getId()));

        Compilation updatedCompilation = new Compilation();
        updatedCompilation.setId(compilation.getId());
        updatedCompilation.setTitle(request.getTitle());
        updatedCompilation.setPinned(request.getPinned());
        updatedCompilation.setEvents(Set.of(event));

        when(compilationRepository.findById(compilation.getId()))
                .thenReturn(Optional.of(compilation));
        when(eventRepository.findAllById(Set.of(event.getId())))
                .thenReturn(List.of(event));
        when(compilationRepository.saveAndFlush(compilation))
                .thenReturn(updatedCompilation);

        CompilationDto result = compilationService.updateCompilation(compilation.getId(), request);

        assertEquals(compilation.getId(), result.getId());
        assertEquals(request.getTitle(), result.getTitle());
        assertEquals(request.getPinned(), result.getPinned());

        verify(compilationRepository).findById(compilation.getId());
        verify(eventRepository).findAllById(Set.of(event.getId()));
        verify(compilationRepository).saveAndFlush(compilation);
    }

    @Test
    void shouldUpdateCompilationWhenCompilationNotFoundThenThrowNotFoundException() {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Updated title");

        when(compilationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> compilationService.updateCompilation(99L, request));

        verify(compilationRepository).findById(99L);
        verify(compilationRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldThrowConflictExceptionWhenUpdatingTitleAlreadyExists() {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Existing title");

        when(compilationRepository.findById(compilation.getId()))
                .thenReturn(Optional.of(compilation));

        when(compilationRepository.saveAndFlush(any(Compilation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate title"));

        assertThrows(ConflictException.class,
                () -> compilationService.updateCompilation(compilation.getId(), request));

        verify(compilationRepository).findById(compilation.getId());
        verify(eventRepository, never()).findAllById(any());
        verify(compilationRepository).saveAndFlush(any(Compilation.class));
    }

    @Test
    void shouldUpdateCompilationWhenEventNotFoundThenThrowNotFoundException() {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setEvents(List.of(1L, 2L));

        when(compilationRepository.findById(compilation.getId()))
                .thenReturn(Optional.of(compilation));
        when(eventRepository.findAllById(Set.of(1L, 2L)))
                .thenReturn(List.of(event));

        assertThrows(NotFoundException.class,
                () -> compilationService.updateCompilation(compilation.getId(), request));

        verify(compilationRepository).findById(compilation.getId());
        verify(eventRepository).findAllById(Set.of(1L, 2L));
        verify(compilationRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateCompilationWhenEventsAreEmptyThenSetEmptyEvents() {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setEvents(List.of());

        Compilation updatedCompilation = new Compilation();
        updatedCompilation.setId(compilation.getId());
        updatedCompilation.setTitle(compilation.getTitle());
        updatedCompilation.setPinned(compilation.getPinned());
        updatedCompilation.setEvents(Set.of());

        when(compilationRepository.findById(compilation.getId()))
                .thenReturn(Optional.of(compilation));
        when(compilationRepository.saveAndFlush(compilation))
                .thenReturn(updatedCompilation);

        CompilationDto result = compilationService.updateCompilation(compilation.getId(), request);

        assertEquals(compilation.getId(), result.getId());
        assertTrue(result.getEvents().isEmpty());

        verify(compilationRepository).findById(compilation.getId());
        verify(eventRepository, never()).findAllById(any());
        verify(compilationRepository).saveAndFlush(compilation);
    }
}
