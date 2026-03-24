# Clean Code Rules

## Naming
- Classes: PascalCase (AccountService)
- Methods: camelCase (createAccount)
- Constants: UPPER_SNAKE_CASE (MAX_TRANSFER_AMOUNT)
- Packages: lowercase (com.finflow.transaction)

## Methods
- Max 20 lines per method
- Single responsibility per method
- No more than 3 parameters (use object if more)
- Return early to avoid deep nesting

## Classes
- Max 200 lines per class
- Single responsibility per class
- Prefer composition over inheritance
- Make classes final when possible

## Comments
- Javadoc on all public methods
- No obvious comments (// set name -> NO)
- Explain WHY not WHAT
