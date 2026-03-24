# Logging Patterns

## Rules
- Use SLF4J with Logback
- Use parameterized logging: log.info("Created account: {}", id)
- Never log sensitive data (passwords, tokens, full card numbers)
- Use appropriate levels:
  - ERROR: system failure, needs immediate attention
  - WARN: unexpected but handled situation
  - INFO: business events (transaction created, account opened)
  - DEBUG: technical details (SQL queries, method entry/exit)

## MDC (Mapped Diagnostic Context)
- Add correlationId to every request
- Add userId when authenticated
- Format: JSON structured logging in production
