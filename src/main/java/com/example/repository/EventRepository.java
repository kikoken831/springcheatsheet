package com.example.repository;

import com.example.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByCategoryOrderByStartDateAsc(String category);
    List<Event> findAllByStatusOrderByCreatedAtDesc(String status);
    List<Event> findAllByLocationContainingIgnoreCase(String location);
}