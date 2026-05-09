package ru.practicum.event.location;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.dto.event.LocationDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocationMapper {
    public static Location toLocationEntity(LocationDto dto) {
        if (dto == null) {
            return null;
        }

        return new Location(dto.getLat(), dto.getLon());
    }

    public static LocationDto toLocationDto(Location location) {
        if (location == null) {
            return null;
        }

        return new LocationDto(location.getLat(), location.getLon());
    }
}
