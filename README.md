# Resilient API Gateway

This project is a lightweight **API Gateway** built using **Spring Cloud Gateway** to protect backend services from traffic spikes and service failures. It demonstrates two common resilience patterns used in production systems: **Rate Limiting** and **Circuit Breaking**.

The goal of this gateway is to act as a defensive layer in front of downstream services, ensuring stability even when traffic increases or a service becomes slow or unavailable.

---

## What This Gateway Handles

### Rate Limiting
Incoming requests are controlled using a **Redis-backed Token Bucket algorithm**.  
This prevents abuse and protects backend services from overload.

Each client (IP or user) gets a limited number of requests per second with burst capacity support.

### Circuit Breaker
The gateway uses **Resilience4j** to monitor downstream service health.  
If a service becomes slow or starts failing repeatedly, the circuit opens and traffic is automatically redirected to a fallback response instead of letting failures cascade.

### Dynamic Routing
Routes are defined programmatically, allowing flexible forwarding of traffic to backend services without hardcoding logic into controllers.

### Metrics & Monitoring
Application metrics are exposed in **Prometheus format**, allowing real-time monitoring of gateway performance and health.

---

## Tech Stack

- Java 17  
- Spring Boot 3  
- Spring Cloud Gateway  
- Redis (for rate limiting state)  
- Resilience4j (for circuit breaker logic)  
- Prometheus (metrics collection)

---

## How to Run

### Prerequisites

- Java 17  
- Maven  
- Docker & Docker Compose  

---

### Step 1: Start Infrastructure

Redis and Prometheus are required for rate limiting and metrics.

```bash
docker-compose up -d
```

---

### Step 2: Run the Gateway

Using Maven wrapper:

```bash
./mvnw spring-boot:run
```

Or if Maven is installed:

```bash
mvn spring-boot:run
```

The gateway will start on:

```
http://localhost:8080
```

---

## Configuration Details

### Rate Limiting

Defined in `application.yml`

- **Replenish Rate**: 10 requests per second  
- **Burst Capacity**: 20 requests  

This allows short traffic bursts while still protecting the backend.

### Circuit Breaker

If a downstream service:

- Responds slower than **2 seconds**, or  
- Has a failure rate higher than **50%**

The circuit opens and traffic is redirected to the `/fallback` endpoint.

---

## Metrics

Prometheus metrics are available at:

```
http://localhost:8080/actuator/prometheus
```

These include request counts, latency, circuit breaker state, and more.

---

## Purpose of This Project

This project was built to demonstrate how an API Gateway can provide **traffic control, failure handling, and observability** in a microservices environment. It focuses on resilience patterns that are commonly implemented in real-world distributed systems.
