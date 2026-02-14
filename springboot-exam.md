# Spring Boot Assessment Cheatsheet: Event REST APIs

## Table of Contents
1. [Project Structure](#project-structure)
2. [Controller Layer](#controller-layer)
3. [Service Layer](#service-layer)
4. [Repository Layer](#repository-layer)
5. [Exception Handling](#exception-handling)
6. [Validation](#validation)
7. [Complete Working Example](#complete-working-example)
8. [HTTP Status Codes Reference](#http-status-codes-reference)

---

## Project Structure

```
src/main/java/com/example/event/
├── controller/
│   └── EventController.java
├── service/
│   └── EventService.java
├── repository/
│   └── EventRepository.java
├── model/
│   └── Event.java
├── dto/
│   ├── EventRequest.java
│   └── EventResponse.java
├── exception/
│   ├── EventNotFoundException.java
│   ├── EmptyEventsException.java
│   └── GlobalExceptionHandler.java
└── validation/
    ├── ValidEventDate.java
    └── EventDateValidator.java
```

---

## Controller Layer

### Basic Controller Setup

```java
package com.example.event.controller;

import com.example.event.dto.EventRequest;
import com.example.event.dto.EventResponse;
import com.example.event.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@Validated
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // CREATE: Post mapping example
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET BY ID: PathVariable example
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable @Positive Long id) {
        EventResponse response = eventService.getEventById(id);
        return ResponseEntity.ok(response);
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        List<EventResponse> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    // FILTER BY CATEGORY
    @GetMapping("/category/{category}")
    public ResponseEntity<List<EventResponse>> getEventsByCategory(
            @PathVariable String category) {
        List<EventResponse> events = eventService.getEventsByCategory(category);
        return ResponseEntity.ok(events);
    }

    // FILTER BY STATUS with RequestParam
    @GetMapping("/filter")
    public ResponseEntity<List<EventResponse>> filterEvents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location) {
        List<EventResponse> events = eventService.filterEvents(status, location);
        return ResponseEntity.ok(events);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable @Positive Long id,
            @Valid @RequestBody EventRequest request) {
        EventResponse response = eventService.updateEvent(id, request);
        return ResponseEntity.ok(response);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable @Positive Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Controller Annotations Quick Reference

```java
// Mapping Annotations
@RestController              // Combines @Controller and @ResponseBody
@RequestMapping("/api/events") // Base URL for all endpoints in this controller

@GetMapping                  // HTTP GET
@PostMapping                 // HTTP POST
@PutMapping                  // HTTP PUT
@DeleteMapping               // HTTP DELETE
@PatchMapping                // HTTP PATCH

// Parameter Annotations
@PathVariable                // Extract value from URL path: /events/{id}
@RequestBody                 // Bind HTTP request body to object
@RequestParam                // Extract query parameter: /events?status=active
@RequestHeader               // Extract HTTP header value

// Validation
@Valid                       // Trigger validation on request body
@Validated                   // Enable method-level validation (class level)
```

### ResponseEntity Patterns

```java
// Success with body (200 OK)
return ResponseEntity.ok(data);
return ResponseEntity.status(HttpStatus.OK).body(data);

// Created (201)
return ResponseEntity.status(HttpStatus.CREATED).body(data);

// No content (204)
return ResponseEntity.noContent().build();

// Bad request (400)
return ResponseEntity.badRequest().body(errorMessage);
return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);

// Not found (404)
return ResponseEntity.notFound().build();
return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

// With headers
return ResponseEntity.ok()
    .header("Custom-Header", "value")
    .body(data);
```

---

## Service Layer

### EventService Interface

```java
package com.example.event.service;

import com.example.event.dto.EventRequest;
import com.example.event.dto.EventResponse;
import java.util.List;

public interface EventService {
    EventResponse createEvent(EventRequest request);
    EventResponse getEventById(Long id);
    List<EventResponse> getAllEvents();
    List<EventResponse> getEventsByCategory(String category);
    List<EventResponse> filterEvents(String status, String location);
    EventResponse updateEvent(Long id, EventRequest request);
    void deleteEvent(Long id);
}
```

### EventService Implementation

```java
package com.example.event.service;

import com.example.event.dto.EventRequest;
import com.example.event.dto.EventResponse;
import com.example.event.exception.EmptyEventsException;
import com.example.event.exception.EventNotFoundException;
import com.example.event.model.Event;
import com.example.event.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Autowired
    public EventServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public EventResponse createEvent(EventRequest request) {
        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setLocation(request.getLocation());
        event.setEventDate(request.getEventDate());
        event.setStatus("ACTIVE");
        event.setCreatedAt(LocalDateTime.now());
        
        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    @Override
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
        return mapToResponse(event);
    }

    @Override
    public List<EventResponse> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        if (events.isEmpty()) {
            throw new EmptyEventsException("No events found in the system");
        }
        return events.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventResponse> getEventsByCategory(String category) {
        List<Event> events = eventRepository.findAllByCategoryOrderByEventDateAsc(category);
        if (events.isEmpty()) {
            throw new EmptyEventsException("No events found for category: " + category);
        }
        return events.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventResponse> filterEvents(String status, String location) {
        List<Event> events;
        
        if (status != null && location != null) {
            events = eventRepository.findAllByStatusAndLocation(status, location);
        } else if (status != null) {
            events = eventRepository.findAllByStatusOrderByCreatedAtDesc(status);
        } else if (location != null) {
            events = eventRepository.findAllByLocation(location);
        } else {
            events = eventRepository.findAll();
        }
        
        if (events.isEmpty()) {
            throw new EmptyEventsException("No events match the filter criteria");
        }
        
        return events.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
        
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setLocation(request.getLocation());
        event.setEventDate(request.getEventDate());
        event.setUpdatedAt(LocalDateTime.now());
        
        Event updated = eventRepository.save(event);
        return mapToResponse(updated);
    }

    @Override
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new EventNotFoundException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    // Helper method for mapping
    private EventResponse mapToResponse(Event event) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setCategory(event.getCategory());
        response.setLocation(event.getLocation());
        response.setEventDate(event.getEventDate());
        response.setStatus(event.getStatus());
        response.setCreatedAt(event.getCreatedAt());
        return response;
    }
}
```

### Service Layer Best Practices

```java
// 1. Throw custom exceptions for business rule violations
if (events.isEmpty()) {
    throw new EmptyEventsException("No events found");
}

// 2. Use Optional from repository
Event event = eventRepository.findById(id)
    .orElseThrow(() -> new EventNotFoundException("Event not found"));

// 3. Check existence before delete
if (!eventRepository.existsById(id)) {
    throw new EventNotFoundException("Cannot delete non-existent event");
}

// 4. Use @Transactional for write operations
@Transactional
public void deleteEvent(Long id) {
    // operations here
}

// 5. Keep controller thin - all logic in service
// BAD: Logic in controller
// GOOD: Controller calls service methods
```

---

## Repository Layer

### EventRepository Interface

```java
package com.example.event.repository;

import com.example.event.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Find by single field (returns Optional)
    Optional<Event> findByName(String name);
    
    // Find all by field with ordering
    List<Event> findAllByCategoryOrderByEventDateAsc(String category);
    
    // Find by status with ordering by created date descending
    List<Event> findAllByStatusOrderByCreatedAtDesc(String status);
    
    // Find by location
    List<Event> findAllByLocation(String location);
    
    // Find by multiple fields
    List<Event> findAllByStatusAndLocation(String status, String location);
    
    // Find by category and status
    List<Event> findAllByCategoryAndStatusOrderByNameAsc(String category, String status);
    
    // Find events after certain date
    List<Event> findAllByEventDateAfter(LocalDateTime date);
    
    // Find events between dates
    List<Event> findAllByEventDateBetween(LocalDateTime start, LocalDateTime end);
    
    // Find by name containing (case insensitive)
    List<Event> findAllByNameContainingIgnoreCase(String keyword);
    
    // Check existence
    boolean existsByName(String name);
    
    // Count by category
    long countByCategory(String category);
    
    // Delete by status
    void deleteByStatus(String status);
}
```

### JPA Query Method Naming Convention

```java
// Keyword patterns:
findBy...           // SELECT * FROM entity WHERE ...
findAllBy...        // SELECT * FROM entity WHERE ... (multiple results)
countBy...          // SELECT COUNT(*) FROM entity WHERE ...
deleteBy...         // DELETE FROM entity WHERE ...
existsBy...         // Check if exists (returns boolean)

// Comparison operators:
findByNameEquals(String name)                    // = name
findByAgeGreaterThan(int age)                   // > age
findByAgeLessThan(int age)                      // < age
findByAgeGreaterThanEqual(int age)              // >= age
findByAgeLessThanEqual(int age)                 // <= age
findByNameContaining(String keyword)            // LIKE %keyword%
findByNameStartingWith(String prefix)           // LIKE prefix%
findByNameEndingWith(String suffix)             // LIKE %suffix
findByNameIgnoreCase(String name)               // case insensitive

// Logical operators:
findByNameAndCategory(String name, String cat)  // AND
findByNameOrCategory(String name, String cat)   // OR
findByStatusNot(String status)                  // NOT
findByAgeIn(List<Integer> ages)                 // IN clause
findByAgeBetween(int start, int end)            // BETWEEN

// Ordering:
findAllByOrderByNameAsc()                       // ORDER BY name ASC
findAllByCategoryOrderByEventDateDesc(String c) // WHERE + ORDER BY
findAllByStatusOrderByCreatedAtDescNameAsc(String s) // Multiple order columns

// Null handling:
findByDescriptionIsNull()                       // IS NULL
findByDescriptionIsNotNull()                    // IS NOT NULL

// Limiting results:
findFirstByCategory(String category)            // LIMIT 1
findTop3ByOrderByCreatedAtDesc()               // LIMIT 3
```

### Optional<T> vs List<T>

```java
// Use Optional<T> when expecting at most ONE result
Optional<Event> findById(Long id);
Optional<Event> findByName(String name);

// Use List<T> when expecting ZERO or MORE results
List<Event> findAllByCategory(String category);
List<Event> findAllByStatus(String status);

// Working with Optional
Optional<Event> eventOpt = eventRepository.findById(id);

// Pattern 1: orElseThrow
Event event = eventOpt.orElseThrow(() -> 
    new EventNotFoundException("Event not found"));

// Pattern 2: orElse (default value)
Event event = eventOpt.orElse(new Event());

// Pattern 3: orElseGet (lazy default)
Event event = eventOpt.orElseGet(() -> createDefaultEvent());

// Pattern 4: ifPresent
eventOpt.ifPresent(event -> System.out.println(event.getName()));

// Pattern 5: isPresent check
if (eventOpt.isPresent()) {
    Event event = eventOpt.get();
}

// Pattern 6: map
String name = eventOpt.map(Event::getName).orElse("Unknown");
```

---

## Exception Handling

### Custom Exceptions

```java
// EventNotFoundException.java
package com.example.event.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(String message) {
        super(message);
    }
    
    public EventNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
// EmptyEventsException.java
package com.example.event.exception;

public class EmptyEventsException extends RuntimeException {
    public EmptyEventsException(String message) {
        super(message);
    }
}
```

```java
// InvalidEventException.java
package com.example.event.exception;

public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }
}
```

### Global Exception Handler

```java
package com.example.event.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Handle EventNotFoundException (404)
    @ExceptionHandler(EventNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleEventNotFoundException(
            EventNotFoundException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // Handle EmptyEventsException (404)
    @ExceptionHandler(EmptyEventsException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleEmptyEventsException(
            EmptyEventsException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // Handle InvalidEventException (400)
    @ExceptionHandler(InvalidEventException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleInvalidEventException(
            InvalidEventException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Handle validation errors from @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // Handle constraint violations from @Validated
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(
            ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // Handle generic exceptions (500)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An internal error occurred",
                LocalDateTime.now(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### ErrorResponse DTO

```java
package com.example.event.exception;

import java.time.LocalDateTime;

public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private String path;

    public ErrorResponse() {
    }

    public ErrorResponse(int status, String message, LocalDateTime timestamp, String path) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.path = path;
    }

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
```

### Exception Handling Patterns

```java
// In Service Layer - throw exceptions
public Event getEvent(Long id) {
    return eventRepository.findById(id)
        .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
}

// In Service Layer - throw for business rule violations
public List<Event> getAllEvents() {
    List<Event> events = eventRepository.findAll();
    if (events.isEmpty()) {
        throw new EmptyEventsException("No events available");
    }
    return events;
}

// Global Handler catches and formats response
@ExceptionHandler(EventNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(EventNotFoundException ex) {
    // Return 404 with error details
}
```

---

## Validation

### Built-in Validation Annotations

```java
package com.example.event.dto;

import com.example.event.validation.ValidEventDate;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

public class EventRequest {

    @NotNull(message = "Name is required")
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @NotEmpty(message = "Description cannot be empty")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Category is required")
    @Pattern(regexp = "^(CONFERENCE|WORKSHOP|SEMINAR|MEETUP)$", 
             message = "Category must be one of: CONFERENCE, WORKSHOP, SEMINAR, MEETUP")
    private String category;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    @ValidEventDate  // Custom validator
    private LocalDateTime eventDate;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 10000, message = "Capacity cannot exceed 10000")
    private Integer capacity;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    @DecimalMax(value = "100000.0", message = "Price cannot exceed 100000")
    @Digits(integer = 6, fraction = 2, message = "Price format invalid")
    private Double price;

    @Email(message = "Invalid email format")
    private String contactEmail;

    @Positive(message = "Organizer ID must be positive")
    private Long organizerId;

    @PositiveOrZero(message = "Attendee count cannot be negative")
    private Integer attendeeCount;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    
    public Long getOrganizerId() { return organizerId; }
    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }
    
    public Integer getAttendeeCount() { return attendeeCount; }
    public void setAttendeeCount(Integer attendeeCount) { this.attendeeCount = attendeeCount; }
}
```

### Validation Annotations Reference

```java
// Null checks
@NotNull        // Must not be null
@Null           // Must be null

// String validations
@NotBlank       // Not null, not empty, contains non-whitespace
@NotEmpty       // Not null, not empty (for String, Collection, Map, Array)
@Size(min=, max=)  // Length validation
@Pattern(regexp="") // Regex validation
@Email          // Email format validation

// Number validations
@Min(value)           // Minimum value (inclusive)
@Max(value)           // Maximum value (inclusive)
@DecimalMin(value)    // Decimal minimum
@DecimalMax(value)    // Decimal maximum
@Positive             // Must be > 0
@PositiveOrZero       // Must be >= 0
@Negative             // Must be < 0
@NegativeOrZero       // Must be <= 0
@Digits(integer=, fraction=) // Numeric format

// Date/Time validations
@Past               // Date must be in the past
@PastOrPresent      // Date must be in past or present
@Future             // Date must be in the future
@FutureOrPresent    // Date must be in future or present

// Boolean validation
@AssertTrue         // Must be true
@AssertFalse        // Must be false
```

### Custom Validator - Step by Step

#### Step 1: Define the Annotation

```java
package com.example.event.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EventDateValidator.class)  // Link to validator class
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEventDate {
    
    String message() default "Event date must be at least 24 hours in the future";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
```

#### Step 2: Implement the Validator

```java
package com.example.event.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class EventDateValidator implements ConstraintValidator<ValidEventDate, LocalDateTime> {

    @Override
    public void initialize(ValidEventDate constraintAnnotation) {
        // Initialization logic if needed (usually empty)
    }

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull, so we allow null here
        if (value == null) {
            return true;
        }
        
        // Business rule: Event must be at least 24 hours in the future
        LocalDateTime minimumDate = LocalDateTime.now().plusHours(24);
        return value.isAfter(minimumDate);
    }
}
```

#### Step 3: Use the Annotation in Model/DTO

```java
package com.example.event.dto;

import com.example.event.validation.ValidEventDate;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class EventRequest {
    
    @NotNull(message = "Event date is required")
    @ValidEventDate  // Apply custom validator
    private LocalDateTime eventDate;
    
    // Other fields and getters/setters
}
```

### Complete Custom Validator Example: ValidCategory

```java
// Step 1: Annotation
package com.example.event.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CategoryValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCategory {
    String message() default "Invalid category";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    // Optional: custom attribute
    String[] allowedValues() default {};
}

// Step 2: Validator Implementation
package com.example.event.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class CategoryValidator implements ConstraintValidator<ValidCategory, String> {

    private List<String> allowedCategories;

    @Override
    public void initialize(ValidCategory constraintAnnotation) {
        // Get allowed values from annotation or use defaults
        String[] values = constraintAnnotation.allowedValues();
        if (values.length > 0) {
            allowedCategories = Arrays.asList(values);
        } else {
            allowedCategories = Arrays.asList("CONFERENCE", "WORKSHOP", "SEMINAR", "MEETUP");
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return allowedCategories.contains(value.toUpperCase());
    }
}

// Step 3: Usage
public class EventRequest {
    @ValidCategory(allowedValues = {"CONFERENCE", "WORKSHOP", "SEMINAR", "MEETUP"})
    private String category;
}
```

### Enable Validation in Controller

```java
// Method 1: Use @Valid for request body validation
@PostMapping
public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
    // Validation happens automatically before method execution
}

// Method 2: Use @Validated at class level for @PathVariable/@RequestParam validation
@RestController
@Validated  // Enable validation for path variables and request params
public class EventController {
    
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable @Positive(message = "ID must be positive") Long id) {
        // Validation occurs automatically
    }
}
```

---

## Complete Working Example

### Event Entity

```java
package com.example.event.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String location;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Event() {
    }

    public Event(String name, String description, String category, String location, 
                 LocalDateTime eventDate, String status) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.location = location;
        this.eventDate = eventDate;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

### EventResponse DTO

```java
package com.example.event.dto;

import java.time.LocalDateTime;

public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String location;
    private LocalDateTime eventDate;
    private String status;
    private LocalDateTime createdAt;

    // Constructors
    public EventResponse() {
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

---

## HTTP Status Codes Reference

### Success Codes (2xx)

```java
// 200 OK - Standard response for successful GET/PUT
return ResponseEntity.ok(data);
return ResponseEntity.status(HttpStatus.OK).body(data);

// 201 CREATED - Resource successfully created (POST)
return ResponseEntity.status(HttpStatus.CREATED).body(data);

// 204 NO CONTENT - Success but no content to return (DELETE)
return ResponseEntity.noContent().build();
```

### Client Error Codes (4xx)

```java
// 400 BAD REQUEST - Invalid input/validation failure
return ResponseEntity.badRequest().body(error);
return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

// 404 NOT FOUND - Resource doesn't exist
return ResponseEntity.notFound().build();
return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

// 409 CONFLICT - Conflict with current state (duplicate resource)
return ResponseEntity.status(HttpStatus.CONFLICT).body(error);

// 422 UNPROCESSABLE ENTITY - Validation error
return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
```

### Server Error Codes (5xx)

```java
// 500 INTERNAL SERVER ERROR - Generic server error
return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
```

### Common Usage by HTTP Method

```java
// POST (Create)
// Success: 201 CREATED
// Error: 400 BAD REQUEST (validation), 409 CONFLICT (duplicate)

// GET (Read)
// Success: 200 OK
// Error: 404 NOT FOUND

// PUT (Update)
// Success: 200 OK
// Error: 400 BAD REQUEST (validation), 404 NOT FOUND

// DELETE (Delete)
// Success: 204 NO CONTENT
// Error: 404 NOT FOUND

// GET (List/Filter)
// Success: 200 OK (even if empty list)
// Error: 400 BAD REQUEST (invalid filter parameters)
// OR throw EmptyEventsException -> 404 NOT FOUND if business requirement
```

---

## Quick Tips & Common Patterns

### Controller Best Practices

```java
// 1. Keep controllers thin
// BAD
@PostMapping
public ResponseEntity<Event> create(@RequestBody EventRequest req) {
    Event event = new Event();
    event.setName(req.getName());
    // ... lots of mapping logic
    return ResponseEntity.ok(eventRepository.save(event));
}

// GOOD
@PostMapping
public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest req) {
    EventResponse response = eventService.createEvent(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

// 2. Use proper HTTP status codes
@PostMapping
public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(req));
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    eventService.deleteEvent(id);
    return ResponseEntity.noContent().build();  // 204
}

// 3. Use @Validated at class level for path/param validation
@RestController
@Validated
public class EventController {
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> get(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }
}
```

### Service Layer Best Practices

```java
// 1. Always throw exceptions for error conditions
public EventResponse getEventById(Long id) {
    Event event = eventRepository.findById(id)
        .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
    return mapToResponse(event);
}

// 2. Handle empty results based on business requirements
public List<EventResponse> getAllEvents() {
    List<Event> events = eventRepository.findAll();
    if (events.isEmpty()) {
        throw new EmptyEventsException("No events found");
    }
    return events.stream().map(this::mapToResponse).collect(Collectors.toList());
}

// 3. Use @Transactional for write operations
@Transactional
public void deleteEvent(Long id) {
    if (!eventRepository.existsById(id)) {
        throw new EventNotFoundException("Event not found");
    }
    eventRepository.deleteById(id);
}
```

### Repository Query Patterns

```java
// Single result - Use Optional<T>
Optional<Event> findById(Long id);
Optional<Event> findByName(String name);

// Multiple results - Use List<T>
List<Event> findAllByCategory(String category);
List<Event> findAllByCategoryOrderByEventDateAsc(String category);

// With ordering
List<Event> findAllByStatusOrderByCreatedAtDesc(String status);
List<Event> findAllByCategoryOrderByEventDateAscNameAsc(String category);

// Date filtering
List<Event> findAllByEventDateAfter(LocalDateTime date);
List<Event> findAllByEventDateBetween(LocalDateTime start, LocalDateTime end);
```

### Exception Handling Pattern

```java
// 1. Create custom exceptions
public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(String message) {
        super(message);
    }
}

// 2. Throw in service layer
public Event getEvent(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));
}

// 3. Handle in @ControllerAdvice
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EventNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}
```

### Validation Pattern

```java
// 1. In DTO - use built-in validators
public class EventRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100)
    private String name;
    
    @Future(message = "Event date must be in future")
    private LocalDateTime eventDate;
}

// 2. For custom validation - create annotation + validator
@ValidEventDate
private LocalDateTime eventDate;

// 3. In controller - use @Valid
@PostMapping
public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(req));
}

// 4. Handle validation errors in GlobalExceptionHandler
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, String>> handleValidation(
        MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
        String field = ((FieldError) error).getField();
        String msg = error.getDefaultMessage();
        errors.put(field, msg);
    });
    return ResponseEntity.badRequest().body(errors);
}
```

---

## application.properties / application.yml

### application.properties

```properties
# Server configuration
server.port=8080

# Database configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Validation
spring.mvc.throw-exception-if-no-handler-found=true
spring.web.resources.add-mappings=false
```

### application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  h2:
    console:
      enabled: true
      path: /h2-console
```

---

## Testing Examples (Bonus)

### Controller Test

```java
package com.example.event.controller;

import com.example.event.dto.EventRequest;
import com.example.event.dto.EventResponse;
import com.example.event.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @Test
    void createEvent_Success() throws Exception {
        EventRequest request = new EventRequest();
        request.setName("Tech Conference");
        request.setCategory("CONFERENCE");
        request.setLocation("Singapore");
        request.setEventDate(LocalDateTime.now().plusDays(30));

        EventResponse response = new EventResponse();
        response.setId(1L);
        response.setName("Tech Conference");

        when(eventService.createEvent(any(EventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tech Conference"));
    }

    @Test
    void getEventById_Success() throws Exception {
        EventResponse response = new EventResponse();
        response.setId(1L);
        response.setName("Tech Conference");

        when(eventService.getEventById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tech Conference"));
    }
}
```

### Service Test

```java
package com.example.event.service;

import com.example.event.dto.EventRequest;
import com.example.event.dto.EventResponse;
import com.example.event.exception.EventNotFoundException;
import com.example.event.model.Event;
import com.example.event.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void getEventById_Success() {
        Event event = new Event();
        event.setId(1L);
        event.setName("Tech Conference");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        EventResponse response = eventService.getEventById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Tech Conference", response.getName());
        verify(eventRepository, times(1)).findById(1L);
    }

    @Test
    void getEventById_NotFound() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> {
            eventService.getEventById(1L);
        });
    }
}
```

---

## Final Checklist for Exam

### Controller ✓
- [ ] Use `@RestController` and `@RequestMapping`
- [ ] Use correct mapping annotations: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- [ ] Use `@PathVariable` for URL parameters
- [ ] Use `@RequestBody` with `@Valid` for request payload
- [ ] Return `ResponseEntity` with appropriate status codes
- [ ] Keep controller methods simple (delegate to service)

### Service ✓
- [ ] Use `@Service` annotation
- [ ] Implement business logic in service layer
- [ ] Throw custom exceptions when needed
- [ ] Use `Optional.orElseThrow()` for not found scenarios
- [ ] Check for empty results and throw `EmptyEventsException`

### Repository ✓
- [ ] Extend `JpaRepository<Entity, ID>`
- [ ] Use correct method naming convention: `findAllByXXOrderByYY`
- [ ] Return `Optional<T>` for single result queries
- [ ] Return `List<T>` for multiple result queries

### Exception Handling ✓
- [ ] Create custom exception classes extending `RuntimeException`
- [ ] Create `@ControllerAdvice` class for global exception handling
- [ ] Use `@ExceptionHandler` for each exception type
- [ ] Return appropriate HTTP status codes (404, 400, 500)
- [ ] Handle `MethodArgumentNotValidException` for validation errors

### Validation ✓
- [ ] Use built-in annotations: `@NotNull`, `@NotBlank`, `@Size`, `@Positive`, `@Future`, etc.
- [ ] Use `@Valid` in controller method parameters
- [ ] Use `@Validated` at controller class level for path variable validation
- [ ] For custom validators: define annotation → implement validator → apply annotation
- [ ] Implement `ConstraintValidator<AnnotationType, FieldType>`

### HTTP Status Codes ✓
- [ ] 200 OK for successful GET/PUT
- [ ] 201 CREATED for successful POST
- [ ] 204 NO CONTENT for successful DELETE
- [ ] 400 BAD REQUEST for validation errors
- [ ] 404 NOT FOUND for missing resources

---

**END OF CHEATSHEET**

Good luck with your assessment! Remember to:
1. Keep controllers simple
2. Put all business logic in services
3. Throw exceptions in service layer
4. Handle exceptions in `@ControllerAdvice`
5. Use correct HTTP status codes
6. Validate input with `@Valid` and custom validators