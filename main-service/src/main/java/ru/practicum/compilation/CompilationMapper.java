package ru.practicum.compilation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.event.Event;
import ru.practicum.event.EventMapper;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompilationMapper {
    public static CompilationDto toCompilationDto(Compilation compilation) {
        return new CompilationDto(
                compilation.getEvents().stream()
                        .map(EventMapper::toEventShortDto)
                        .toList(),
                compilation.getId(),
                compilation.getPinned(),
                compilation.getTitle()
                );
    }

    public static Compilation toCompilation(NewCompilationDto dto, Set<Event> events) {
        Compilation compilation = new Compilation();

        compilation.setEvents(events);
        compilation.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
        compilation.setTitle(dto.getTitle());

        return compilation;
    }
}
