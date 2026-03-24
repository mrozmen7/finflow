# Spring Boot Patterns

## Controller Rules
- Use @RestController, never @Controller for APIs
- Return ResponseEntity with proper HTTP status codes
- Use @Valid for request validation
- Keep controllers thin - delegate to service layer

## Service Rules
- Use constructor injection, never field injection
- Mark service methods @Transactional where needed
- Throw custom exceptions, catch in @ControllerAdvice

## Entity Rules
- Use BigDecimal for money, never double/float
- Always add @Version for optimistic locking
- Use @PrePersist/@PreUpdate for audit fields
- Protected no-arg constructor for JPA

## DTO Rules
- Use Java Records for DTOs
- Separate request and response DTOs
- Use @Valid annotations on request DTOs
