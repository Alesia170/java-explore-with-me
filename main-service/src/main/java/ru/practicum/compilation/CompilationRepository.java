package ru.practicum.compilation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query(
            value = """
                    SELECT *
                    FROM compilations
                    WHERE :pinned IS NULL OR pinned = :pinned
                    ORDER BY id
                    LIMIT :size OFFSET :from
                    """,
            nativeQuery = true
    )
    List<Compilation> findAllWithOffset(@Param("pinned") Boolean pinned,
                                        @Param("from") int from,
                                        @Param("size") int size);

    boolean existsByTitle(String title);

    Optional<Compilation> findByTitle(String title);
}