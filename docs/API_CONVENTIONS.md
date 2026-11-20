# API and Java Conventions

## Spring equivalents
| Desired convention | Spring-native approach |
|---|---|
| ApiResponse | Generic Java record/DTO for REST |
| ApiError | Stable error DTO and error-code enum |
| asyncHandler | `@RestControllerAdvice`; normal exception propagation |
| Middleware | Spring Security filters, OncePerRequestFilter, HandlerInterceptor |
| public directory | `src/main/resources/static/` |
| src | Maven `src/main/java`, `src/main/resources`, `src/test/java` |
| DB name constants | Environment/deployment configuration, not hard-coded Java constants |
| required/type/lowercase schema options | Java types, Bean Validation, JPA/database constraints and explicit normalization |

## Example DTOs
```java
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    String requestId
) {}

public record ApiError(
    boolean success,
    String code,
    String message,
    String requestId,
    Map<String, Object> details
) {}
```

## Error handling
- REST: `@RestControllerAdvice`, stable error codes and correct HTTP status.
- Authentication/authorization: Spring Security entry point and access-denied handler.
- MCP: protocol-appropriate safe error mapping; do not force REST envelope into MCP.
- Never expose stack traces, SQL, internal hostnames, secrets or raw downstream errors.

## Suggested package structure
```text
com.agentgate
  shared/{api,exception,config,constants,observability}
  security/{config,filter,principal,authorization}
  agent/{api,application,domain,infrastructure}
  tool/{api,application,domain,infrastructure,adapter/mcp,adapter/rest}
  policy/{api,application,domain,infrastructure}
  invocation/{api,application,domain,infrastructure}
  approval/{api,application,domain,infrastructure}
  audit/{api,application,domain,infrastructure}
  quota/{api,application,domain,infrastructure}
  killswitch/{api,application,domain,infrastructure}
  health/{application,domain,infrastructure}
  alert/{application,domain,infrastructure}
  outbox/{application,infrastructure}
```

Keep middleware/filter configuration centralized where appropriate, but do not put every unrelated filter into one giant class. Use ordered, focused filters and Spring Security's filter chain.
