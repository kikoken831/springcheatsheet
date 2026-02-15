package com.example.service;


import com.example.entity.Event;
import com.example.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    EventRepository eventRepo;


    @Autowired
    public EventService(EventRepository repo) {
        this.eventRepo = repo;
    }

    public List<Event> findAll() {
        return eventRepo.findAll();
    }

}
