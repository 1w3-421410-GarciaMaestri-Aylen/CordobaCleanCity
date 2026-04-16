# Garbage Reporting Service - Base Architecture

## Package structure

```text
com.example.garbagereporting
|- cache
|- config
|- controller
|- dto
|- exception
|- mapper
|- messaging
|  |- consumer
|  |- event
|  |- publisher
|- model
|- repository
|- service
|  |- impl
|- utils
```

## Naming conventions

- Controllers: `<Resource>Controller`
- Services (interfaces): `<Capability>Service`
- Services (implementations): `<Capability>ServiceImpl`
- Repositories: `<Aggregate>Repository`
- Models (Mongo): singular noun (`GarbageReport`)
- DTOs:
  - Request: `<Action><Resource>RequestDto`
  - Response: `<Resource>ResponseDto`
- Mappers: `<Resource>Mapper`
- Exceptions:
  - Business/functional: `<Domain>ErrorException`
  - HTTP mapping in `GlobalExceptionHandler`
- Messaging:
  - Events: `<Resource><Action>Event`
  - Publishers: `<Resource>EventPublisher`
  - Consumers: `<Resource>EventListener`

## Growth guidelines

- Keep domain logic in `service` and orchestration in controller only.
- Keep external integrations isolated (`messaging`, `cache`, future `client` package).
- Avoid direct controller-to-repository calls.
- Use DTOs for API contracts and keep models internal.
