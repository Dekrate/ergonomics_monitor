#!/bin/bash

echo "Starting PostgreSQL..."
docker-compose -f ../docker-compose.yml up -d

echo "Waiting for PostgreSQL to be ready..."
until docker exec ergonomics-postgres pg_isready -U postgres; do
  sleep 2
done

echo "Infrastructure ready!"
echo "PostgreSQL: localhost:5432"
echo "Ollama: http://localhost:11434 (lokalna instalacja)"

echo "Starting Spring Boot application..."
mvn spring-boot:run -Dspring.profiles.active=dev
