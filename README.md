# 🚀 Resilient API Gateway with Rate Limiting

A production-ready API Gateway built with Spring Cloud Gateway that protects backend services from traffic spikes and cascading failures. Features distributed rate limiting using Redis and circuit breaker patterns with Resilience4j, all monitored via Prometheus metrics.

## 🎯 Overview

This API Gateway acts as a single entry point for microservices, providing critical infrastructure capabilities:

- **🛡️ Rate Limiting** - Prevent abuse and protect backend services from traffic spikes
- **⚡ Circuit Breaker** - Stop cascading failures when downstream services are unhealthy
- **📊 Observability** - Monitor traffic patterns and system health with Prometheus
- **🔀 Intelligent Routing** - Route requests to appropriate backend services
- **⚙️ Resilience** - Gracefully handle failures and recover automatically

Perfect for microservices architectures that need to handle unpredictable traffic while maintaining reliability.

## 🏗️ Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────┐
│      API Gateway (Port 8080)     │
│  ┌────────────────────────────┐  │
│  │   Rate Limit Filter        │  │ ◄── Redis (Token Bucket)
│  │   (100 req/min per IP)     │  │
│  └────────────┬───────────────┘  │
│               ▼                  │
│  ┌────────────────────────────┐  │
│  │   Circuit Breaker Filter   │  │ ◄── Resilience4j
│  │   (Prevent Cascading)      │  │
│  └────────────┬───────────────┘  │
│               ▼                  │
│  ┌────────────────────────────┐  │
│  │   Route to Backend         │  │
│  └────────────────────────────┘  │
└──────────────┬───────────────────┘
               │
       ┌───────┴────────┬──────────┐
       ▼                ▼          ▼
  ┌─────────┐    ┌──────────┐  ┌──────────┐
  │  User   │    │  Order   │  │ Payment  │
  │ Service │    │ Service  │  │ Service  │
  └─────────┘    └──────────┘  └──────────┘
```

## ✨ Key Features

### 1. Distributed Rate Limiting

Uses **Redis-based token bucket algorithm** to enforce request quotas across multiple gateway instances:

- **Capacity**: 100 tokens per bucket
- **Refill Rate**: 10 tokens/second
- **Scope**: Per client IP address
- **Response**: HTTP 429 (Too Many Requests) when limit exceeded

**How it works:**
1. Each client gets a virtual "bucket" of tokens
2. Every request consumes 1 token
3. Tokens refill automatically at a fixed rate
4. When bucket is empty, requests are rejected
5. Allows burst traffic while enforcing average limits

### 2. Circuit Breaker Pattern

Implements **Resilience4j circuit breaker** to prevent cascading failures:

- **Failure Threshold**: 50% failure rate triggers circuit open
- **Minimum Calls**: 5 calls before calculating failure rate
- **Wait Duration**: 30 seconds in open state before retry
- **Half-Open Test**: 3 calls to test service recovery

**States:**
- **CLOSED**: Normal operation, all requests pass through
- **OPEN**: Service is failing, requests fail immediately
- **HALF_OPEN**: Testing if service has recovered

### 3. Prometheus Metrics

Exposes detailed metrics for monitoring:

- Request rates and latencies
- Rate limit hits/misses
- Circuit breaker state changes
- HTTP status code distribution
- Custom business metrics

Access metrics at: `http://localhost:8080/actuator/prometheus`

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven 3.6+

### Running with Docker Compose

The easiest way to run the entire stack:

```bash
# Clone the repository
git clone https://github.com/saivarun-narajala/api-gateway-rate-limiter.git
cd api-gateway-rate-limiter

# Start all services
docker-compose up --build

# Services will be available at:
# - API Gateway: http://localhost:8080
# - Prometheus: http://localhost:9090
# - Grafana: http://localhost:3000 (admin/admin)
```

### Running Locally

1. **Start Redis**
   ```bash
   docker run -d -p 6379:6379 redis:7-alpine
   ```

2. **Build and run the gateway**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

3. **Test the gateway**
   ```bash
   curl http://localhost:8080/gateway/health
   ```

## 📡 API Examples

### Check Gateway Health

```bash
curl http://localhost:8080/gateway/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "api-gateway"
}
```

### Check Rate Limit Status

```bash
curl "http://localhost:8080/gateway/rate-limit/status?key=192.168.1.1"
```

**Response:**
```json
{
  "key": "192.168.1.1",
  "remainingTokens": 87,
  "capacity": 100,
  "refillRate": "10 tokens/second"
}
```

### Check Circuit Breaker Status

```bash
curl "http://localhost:8080/gateway/circuit-breaker/status?service=user-service"
```

**Response:**
```json
{
  "service": "user-service",
  "state": "CLOSED",
  "failureRate": 12.5,
  "numberOfCalls": 8,
  "numberOfFailedCalls": 1
}
```

### Test Rate Limiting

Send rapid requests to trigger rate limiting:

```bash
for i in {1..150}; do
  curl http://localhost:8080/api/users/123
  echo ""
done
```

After ~100 requests, you'll receive:
```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Retry-After: 60
```

## 🔧 Configuration

### Gateway Routes

Configure routes in `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/users/**
```

### Rate Limiting

Adjust in `TokenBucketRateLimiter.java`:

```java
private static final long BUCKET_CAPACITY = 100;  // Max tokens
private static final long REFILL_RATE = 10;       // Tokens/second
```

### Circuit Breaker

Configure in `Resilience4jConfig.java`:

```java
.failureRateThreshold(50)              // 50% failures = open
.minimumNumberOfCalls(5)               // Min calls before calculation
.waitDurationInOpenState(Duration.ofSeconds(30))
```

## 📊 Monitoring with Prometheus

### View Metrics

1. Open Prometheus: http://localhost:9090
2. Try these queries:

**Request rate:**
```promql
rate(http_server_requests_seconds_count[1m])
```

**Circuit breaker state:**
```promql
resilience4j_circuitbreaker_state
```

**Rate limit rejections:**
```promql
rate(http_server_requests_seconds_count{status="429"}[1m])
```

### Grafana Dashboards

1. Open Grafana: http://localhost:3000
2. Login: admin/admin
3. Add Prometheus datasource: http://prometheus:9090
4. Import Spring Boot dashboard (ID: 4701)

## 🧪 Testing

### Simulate Traffic Spike

```bash
# Install Apache Bench
# Windows: Download from Apache website
# Linux: sudo apt-get install apache2-utils

# Send 1000 requests with 10 concurrent connections
ab -n 1000 -c 10 http://localhost:8080/api/users/123
```

### Simulate Service Failure

Stop a backend service to trigger circuit breaker:

```bash
docker-compose stop mock-user-service

# Make requests - circuit will open after 50% failure rate
for i in {1..10}; do
  curl http://localhost:8080/api/users/123
done

# Restart service
docker-compose start mock-user-service
```

## 🎓 How It Works

### Token Bucket Algorithm

The rate limiter uses a **token bucket** stored in Redis:

1. **Initialization**: Each client gets a bucket with 100 tokens
2. **Request**: Consumes 1 token from bucket
3. **Refill**: Tokens refill at 10/second automatically
4. **Rejection**: If bucket empty, request gets HTTP 429

**Why Redis?**
- Distributed: Works across multiple gateway instances
- Fast: Sub-millisecond operations
- Atomic: Thread-safe token operations

### Circuit Breaker States

```
         ┌─────────┐
         │ CLOSED  │ ◄── Normal operation
         └────┬────┘
              │ 50% failures
              ▼
         ┌─────────┐
         │  OPEN   │ ◄── Fail fast
         └────┬────┘
              │ After 30s
              ▼
         ┌──────────┐
         │HALF_OPEN │ ◄── Test recovery
         └──────────┘
              │
       ┌──────┴──────┐
       │             │
   Success       Failure
       │             │
       ▼             ▼
   CLOSED         OPEN
```

## 🛠️ Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Gateway** | Spring Cloud Gateway | Reactive API gateway |
| **Rate Limiting** | Redis | Distributed token bucket |
| **Circuit Breaker** | Resilience4j | Fault tolerance |
| **Metrics** | Prometheus | Monitoring & alerting |
| **Visualization** | Grafana | Dashboards |
| **Containerization** | Docker | Deployment |

## 📁 Project Structure

```
api-gateway-rate-limiter/
├── src/main/java/com/gateway/
│   ├── ApiGatewayApplication.java      # Main application
│   ├── config/
│   │   ├── RedisConfig.java            # Redis configuration
│   │   └── Resilience4jConfig.java     # Circuit breaker config
│   ├── filter/
│   │   ├── RateLimitFilter.java        # Rate limiting logic
│   │   └── CircuitBreakerFilter.java   # Circuit breaker logic
│   ├── ratelimit/
│   │   └── TokenBucketRateLimiter.java # Token bucket implementation
│   └── controller/
│       └── GatewayController.java      # Monitoring endpoints
├── src/main/resources/
│   └── application.yml                 # Configuration
├── docker-compose.yml                  # Full stack setup
├── prometheus.yml                      # Prometheus config
└── pom.xml                            # Maven dependencies
```

## 🚦 Health Checks

All components expose health endpoints:

```bash
# Gateway health
curl http://localhost:8080/actuator/health

# Prometheus health
curl http://localhost:9090/-/healthy

# Grafana health
curl http://localhost:3000/api/health
```

## 🔐 Production Considerations

For production deployments:

1. **Authentication**: Add OAuth2/JWT validation
2. **HTTPS**: Enable TLS termination
3. **Scaling**: Deploy multiple gateway instances behind load balancer
4. **Redis Cluster**: Use Redis Cluster for high availability
5. **Alerting**: Configure Prometheus alerts for critical metrics
6. **Logging**: Integrate with ELK stack or similar
7. **Rate Limit Customization**: Different limits per API/user tier

## 📝 License

MIT License - feel free to use this as a reference for your own API gateway implementation.

## 🤝 Contributing

This is a portfolio project showcasing API gateway patterns and resilience techniques.

---

**Built with ❤️ using Spring Cloud Gateway, Redis, Resilience4j, and Prometheus**
