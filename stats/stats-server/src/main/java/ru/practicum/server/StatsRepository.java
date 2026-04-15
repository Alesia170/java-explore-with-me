package ru.practicum.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(
                e.app,
                e.uri,
                count(e)
            )
            FROM EndpointHit AS e
            WHERE e.timestamp BETWEEN :start AND :end
            GROUP BY e.app, e.uri
            ORDER BY COUNT(e) DESC
            """)
    List<ViewStatsDto> getStats(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(
                e.app,
                e.uri,
                count(DISTINCT e.ip)
            )
            FROM EndpointHit AS e
            WHERE e.timestamp BETWEEN :start AND :end
            GROUP BY e.app, e.uri
            ORDER BY COUNT(DISTINCT e.ip) DESC
            """)
    List<ViewStatsDto> getUniqueStats(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(
                e.app,
                e.uri,
                count(e)
            )
            FROM EndpointHit AS e
            WHERE e.timestamp BETWEEN :start AND :end
                AND e.uri IN :uris
            GROUP BY e.app, e.uri
            ORDER BY COUNT(e) DESC
            """)
    List<ViewStatsDto> getStatsByUris(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("uris") List<String> uris);

    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(
                e.app,
                e.uri,
                count(DISTINCT e.ip)
            )
            FROM EndpointHit AS e
            WHERE e.timestamp BETWEEN :start AND :end
                AND e.uri IN :uris
            GROUP BY e.app, e.uri
            ORDER BY COUNT(DISTINCT e.ip) DESC
            """)
    List<ViewStatsDto> getUniqueStatsByUris(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("uris") List<String> uris);
}
