#!/bin/bash

echo "Starting PostgreSQL..."
docker-compose up -d

echo "Waiting for PostgreSQL to be ready..."
until docker exec ergonomics-postgres pg_isready -U postgres; do
  sleep 2
done

echo "Infrastructure ready!"
echo "PostgreSQL: localhost:5432"
echo "Ollama: http://localhost:11434 (lokalna instalacja)"

echo "Starting Spring Boot application..."
./mvnw spring-boot:run -Dspring.profiles.active=dev
