# 03. Spring Architecture

- Keep the implementation simple:
  - Controller
  - Service
  - Repository
  - Entity
  - DTO
- Put business rules in services/entities, not controllers.
- Use transactions around state-changing use cases.
- Use JPA repositories unless the user chooses MyBatis.
- Prefer meaningful domain methods for state changes.
- Avoid exposing mutable entity internals directly through API responses.
- Keep authentication simple and documented: `X-User-Id` header or request parameter.
- Use clear error responses for invalid state, not silent no-ops.

