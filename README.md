# Vessel Route Optimizer

Multi-stop maritime route planner using UN/LOCODE port database (1,200+ ports) and graph algorithms to minimize voyage distance and fuel consumption.

## Tech Stack
- Java 17, Spring Boot 3, PostgreSQL, JPA/Hibernate
- Maven for build management
- JUnit 5 for testing

## Data Sources
- **UN/LOCODE**: Official port code database from UNECE (free CSV, 1,200+ ports with coordinates)
- **OSRM**: Open Source Routing Machine API for distance approximations (public demo server)

## Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### Database Setup
```bash
createdb vessel_optimizer
```

### Configuration
Copy `src/main/resources/application-example.yml` to `application.yml` and update:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vessel_optimizer
    username: your_user
    password: your_password
```

### Run Application
```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

## API Usage

### Calculate Optimal Route
```bash
curl -X POST http://localhost:8080/api/routes \
  -H "Content-Type: application/json" \
  -d '{"portCodes": ["USNYC", "GBLGP", "SGSIN", "CNSHA"]}'
```

Response:
```json
{
  "route": ["USNYC", "GBLGP", "SGSIN", "CNSHA"],
  "totalDistanceKm": 21847.3,
  "estimatedFuelTons": 327.7,
  "legs": [
    {"from": "USNYC", "to": "GBLGP", "distanceKm": 5570.2},
    {"from": "GBLGP", "to": "SGSIN", "distanceKm": 10823.1},
    {"from": "SGSIN", "to": "CNSHA", "distanceKm": 5454.0}
  ]
}
```

### List All Ports
```bash
curl http://localhost:8080/api/ports
```

## Running Tests
```bash
mvn test
```

## Project Structure
```
src/main/java/com/vessel/optimizer
├── config          # Database & app configuration
├── controller      # REST endpoints
├── dto             # Request/Response objects
├── model           # JPA entities
├── repository      # Spring Data repositories
├── service         # Business logic (loader, router, OSRM client)
└── Application.java
```

## Key Features
- **Port Ingestion**: Loads UN/LOCODE CSV into PostgreSQL with geospatial indexing
- **Graph Routing**: Dijkstra's algorithm with haversine distance heuristic over adjacency list
- **Strategy Pattern**: Pluggable distance providers (Haversine, OSRM)
- **REST API**: Validated endpoints with global exception handling
- **TDD**: 94% coverage on routing engine with parameterized tests

## Environment Variables
- `OSRM_BASE_URL`: Override OSRM server (default: `https://router.project-osrm.org`)

## License
MIT