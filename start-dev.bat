@echo off
echo Starting PostgreSQL...
docker-compose up -d

echo Waiting for PostgreSQL to be ready...
:wait_postgres
docker exec ergonomics-postgres pg_isready -U postgres >nul 2>&1
if errorlevel 1 (
    timeout /t 2 >nul
    goto wait_postgres
)

echo ✅ Infrastructure ready!
echo 📊 PostgreSQL: localhost:5432
echo 🤖 Ollama: http://localhost:11434 (lokalna instalacja)

echo 🚀 Starting Spring Boot application...
mvn spring-boot:run -D"spring.profiles.active=dev"
