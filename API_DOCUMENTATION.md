# Ergonomics Monitor - Complete Implementation

## 🎯 Overview

Full-stack ergonomics monitoring application with:
- ✅ **AI-powered break recommendations** using Ollama LLM
- ✅ **Real-time WebSocket streaming** for live dashboard updates
- ✅ **REST API endpoints** for comprehensive dashboard functionality  
- ✅ **Database migrations** with Flyway for schema management
- ✅ **Reactive programming** with Spring WebFlux and R2DBC
- ✅ **SOLID architecture** with Strategy and Adapter patterns

## 🏗️ Architecture Summary

### Core Components
- **AI Strategy**: `AIBreakRecommendationStrategy` - Intelligent break analysis using Ollama
- **WebSocket**: `ActivityWebSocketHandler` - Real-time bidirectional communication
- **REST API**: `DashboardController` - Comprehensive dashboard endpoints
- **Database**: Flyway migrations with PostgreSQL + R2DBC
- **Reactive Streams**: End-to-end reactive programming with Reactor

### Design Patterns Applied
- **Strategy Pattern**: Multiple analysis strategies (Pomodoro, AI)
- **Builder Pattern**: Immutable value objects
- **Repository Pattern**: Data access abstraction
- **Adapter Pattern**: Cross-platform notifications

## 🚀 Quick Start

### Prerequisites
- Java 23+
- PostgreSQL 16+
- Ollama with `bielik-11b-v3.0-instruct:bf16` model
- Docker (optional, for Testcontainers)

### Configuration
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/ergonomics_db
    username: postgres
    password: password
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: SpeakLeash/bielik-11b-v3.0-instruct:bf16

ergonomics:
  websocket:
    enabled: true
    path: "/ws/activity"
  notifications:
    windows:
      enabled: true
```

### Build & Run
```bash
mvn clean compile
mvn spring-boot:run
```

## 📡 API Endpoints

### Dashboard API

#### Get Daily Metrics
```http
GET /api/dashboard/metrics/{userId}?date=2026-02-17
```
Response:
```json
{
  "id": "uuid",
  "userId": "uuid", 
  "metricDate": "2026-02-17",
  "totalEvents": 1250,
  "avgIntensity": 45.3,
  "maxIntensity": 89.7,
  "productivityScore": 78.5,
  "workDurationMinutes": 485
}
```

#### Weekly Summary
```http
GET /api/dashboard/summary/week/{userId}
```
Response:
```json
{
  "period": "week",
  "startDate": "2026-02-11", 
  "endDate": "2026-02-17",
  "daysWithData": 7,
  "averageProductivityScore": 76.3,
  "totalEvents": 8750,
  "totalWorkHours": 52.4,
  "dailyMetrics": [...]
}
```

#### Real-time Stream (SSE)
```http
GET /api/dashboard/stream/{userId}
Content-Type: text/event-stream
```
Streams live updates every 5 seconds.

#### System Health
```http
GET /api/dashboard/health
```
Response:
```json
{
  "activeWebSocketConnections": 3,
  "systemTime": 1708189234567,
  "status": "healthy",
  "features": {
    "realTimeStreaming": true,
    "aiRecommendations": true,
    "dashboardMetrics": true,
    "webSocketSupport": true
  }
}
```

### Activity API

#### Get Recent Activity
```http
GET /api/activity/recent/{userId}?limit=50
```

#### Manual Break Recommendation
```http
POST /api/activity/break-check/{userId}
```

## 🔌 WebSocket API

### Connection
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/activity');

ws.onmessage = function(event) {
    const update = JSON.parse(event.data);
    console.log('Activity update:', update);
};
```

### Message Types
- `WELCOME` - Connection established
- `DASHBOARD_UPDATE` - Real-time metrics update
- `BREAK_RECOMMENDATION` - New break suggestion
- `HEARTBEAT` - Keep-alive ping

### Client Messages
```javascript
// Send ping
ws.send(JSON.stringify({type: 'PING'}));

// Subscribe to channel
ws.send(JSON.stringify({
  type: 'SUBSCRIBE',
  channel: 'user-activity'
}));
```

## 🤖 AI Integration

### Ollama Setup
```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Download Polish model
ollama pull SpeakLeash/bielik-11b-v3.0-instruct:bf16

# Start server
ollama serve
```

### AI Analysis Features
- **Intelligent Break Timing**: Analyzes work patterns and intensity
- **Personalized Recommendations**: Adapts to user behavior
- **Ergonomic Risk Assessment**: Evaluates RSI and fatigue indicators
- **Natural Language Explanations**: Polish language recommendations

### Example AI Interaction
```json
{
  "needsBreak": true,
  "urgency": "MEDIUM",
  "durationMinutes": 5,
  "reason": "Wykryto zwiększoną intensywność pracy przez ostatnie 45 minut"
}
```

## 🗄️ Database Schema

### Flyway Migrations
- `V1__initial_schema.sql` - Core tables with indexes
- Future migrations for schema evolution

### Tables
```sql
-- Activity Events
CREATE TABLE activity_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    type VARCHAR(50) NOT NULL,
    intensity DOUBLE PRECISION NOT NULL,
    metadata JSONB
);

-- Break Recommendations  
CREATE TABLE break_recommendations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    reason TEXT NOT NULL,
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Dashboard Metrics (Pre-aggregated)
CREATE TABLE dashboard_metrics (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    total_events BIGINT NOT NULL DEFAULT 0,
    avg_intensity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    productivity_score DOUBLE PRECISION,
    work_duration_minutes INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, metric_date)
);
```

## 🧪 Testing

### Run Tests
```bash
# All tests
mvn test

# Integration tests only
mvn test -Dtest=*Integration*

# Specific test
mvn test -Dtest=FullStackIntegrationTest
```

### Test Categories
- **Unit Tests**: Individual component testing
- **Integration Tests**: Component interaction testing
- **Migration Tests**: Database schema validation
- **Performance Tests**: Real-time streaming validation

## 📊 Monitoring & Observability

### Metrics Available
- Active WebSocket connections
- AI recommendation accuracy
- Dashboard query performance  
- Database migration status

### Health Checks
```http
GET /api/dashboard/health
GET /actuator/health (if enabled)
```

## 🔐 Production Considerations

### Security
- Configure CORS properly for production
- Add authentication for sensitive endpoints
- Validate all user inputs
- Use HTTPS in production

### Performance
- Database connection pooling configured
- Reactive streams for backpressure handling
- Efficient WebSocket connection management
- Pre-aggregated dashboard metrics

### Scaling
- Stateless application design
- Database-backed session management
- Horizontal scaling ready
- Load balancer friendly

## 🛠️ Development Guidelines

### Code Style
- SOLID principles throughout
- Reactive programming patterns
- Comprehensive error handling
- Extensive logging for debugging

### Architecture Decisions
- **R2DBC over JPA**: Better reactive support
- **Flyway over Liquibase**: Simplicity and SQL clarity
- **Strategy Pattern**: Extensible analysis algorithms  
- **WebSocket + SSE**: Multiple real-time options

## 🎓 Learning Outcomes

This implementation demonstrates:
- ✅ Advanced Spring Boot reactive programming
- ✅ AI integration with local LLM models
- ✅ Real-time web communication patterns
- ✅ Database migration best practices
- ✅ SOLID design principles in practice
- ✅ Comprehensive testing strategies
- ✅ Production-ready application architecture

---

**Tech Stack**: Java 23, Spring Boot 3.4.2, WebFlux, R2DBC, PostgreSQL, Ollama AI, Flyway, JUnit 5, Testcontainers
