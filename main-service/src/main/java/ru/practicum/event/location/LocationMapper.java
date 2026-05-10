package ru.practicum.event.location;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.dto.event.Location;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocationMapper {
    public static ru.practicum.event.location.Location toLocationEntity(Location dto) {
        if (dto == null) {
            return null;
        }

        return new ru.practicum.event.location.Location(dto.getLat(), dto.getLon());
    }

    public static Location toLocationDto(ru.practicum.event.location.Location location) {
        if (location == null) {
            return null;
        }

        return new Location(location.getLat(), location.getLon());
    }
}
