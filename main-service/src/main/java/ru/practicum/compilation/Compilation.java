package ru.practicum.compilation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.practicum.event.Event;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "compilations")
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean pinned;

    @Column(nullable = false, unique = true)
    private String title;

    @ManyToMany
    @JoinTable(name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id"))
    private Set<Event> events = new HashSet<>();
}
