# JPA Patterns

## Repository Rules
- Extend JpaRepository
- Use derived query methods (findByX)
- Use @Query for complex queries with JPQL
- Never use native queries unless absolutely necessary

## Performance
- Watch for N+1: use JOIN FETCH or EntityGraph
- Use pagination for list endpoints
- Set appropriate fetch types (LAZY by default)

## Transactions
- @Transactional on service layer, not repository
- Use readOnly=true for read operations
- Be aware of transaction boundaries with Kafka
