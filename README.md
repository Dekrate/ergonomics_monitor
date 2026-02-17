# Ergonomics Monitor - Setup & Development

## 🚀 Quick Start (Recommended)

### Prerequisites
- Docker Desktop installed
- Java 17+ 
- Maven 3.6+

### Development Setup

1. **Start infrastructure services:**
   ```bash
   # Windows
   .\start-dev.bat
   
   # Linux/Mac
   ./start-dev.sh
   ```

2. **Or manually:**
   ```bash
   # Start databases
   docker-compose up -d
   
   # Run application
   mvn spring-boot:run -Dspring.profiles.active=dev
   ```

### Services
- **Application**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Ollama AI**: http://localhost:11434

## 📋 Available Profiles

- `dev` - Development with Docker infrastructure
- `test` - Testing with Testcontainers
- `prod` - Production configuration

## 🧪 Testing

```bash
# Unit tests
mvn test

# Integration tests with Testcontainers
mvn test -Dspring.profiles.active=test
```

## 🛠️ Development Tools

```bash
# Clean & build
mvn clean compile

# Run with specific profile
mvn spring-boot:run -Dspring.profiles.active=dev

# Database migration
mvn flyway:migrate

# Stop all services
docker-compose down
```

## 📁 Project Structure

```
src/
├── main/
│   ├── java/pl/dekrate/ergonomicsmonitor/
│   │   ├── config/          # Spring configuration
│   │   ├── controller/      # REST endpoints
│   │   ├── model/           # Domain models
│   │   ├── repository/      # Data access
│   │   └── service/         # Business logic
│   └── resources/
│       ├── db/migration/    # Flyway SQL scripts
│       └── application*.yml # Configuration files
└── test/                    # Tests with Testcontainers
```

## 🔧 Troubleshooting

### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# View logs
docker logs ergonomics-postgres

# Reset database
docker-compose down -v
docker-compose up -d
```

### Ollama AI Issues
```bash
# Check Ollama status
curl http://localhost:11434/api/health

# Install model manually
docker exec -it ergonomics-ollama ollama pull bielik
```
