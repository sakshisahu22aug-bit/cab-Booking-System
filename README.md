# Production-Level Cab Booking System (Uber Clone)

An enterprise-grade, microservice-ready Cab Booking System backend built with **Spring Boot 3.2.x**, **Java 17**, **PostgreSQL**, **Spring Security (JWT)**, **Spring WebSocket (STOMP)**, and **Stripe**.

---

## 🛠️ Tech Stack & Architecture

- **Java**: 17 LTS
- **Framework**: Spring Boot 3.2.4
- **Database**: PostgreSQL 14+ with Hibernate 6 / Spring Data JPA
- **Security**: Spring Security 6 with stateless JWT Bearer token authentication (JJWT 0.12.5)
- **Geospatial Matching**: Native Haversine spherical distance calculation
- **Real-Time Communication**: Spring WebSocket with STOMP message broker (`/topic`, `/app`)
- **Payments**: Stripe Java SDK (PaymentIntent API)
- **API Documentation**: Springdoc OpenAPI 3.0 / Swagger UI
- **Invoicing**: iTextPDF 5.5.x for ride receipt PDF generation
- **Notifications**: Spring Mail (asynchronous email dispatch)
- **Boilerplate Reduction**: Project Lombok

---

## 📁 Project Architecture & Package Structure

```
com.labmentix.cabbooking
├── CabBookingApplication.java
├── config
│   ├── DataInitializer.java
│   ├── OpenApiConfig.java
│   ├── SecurityConfig.java
│   └── WebSocketConfig.java
├── controller
│   ├── AuthController.java
│   ├── DriverController.java
│   ├── InvoiceController.java
│   ├── PaymentController.java
│   ├── RatingController.java
│   └── RideController.java
├── dto
│   ├── ApiResponse.java
│   ├── AuthResponse.java
│   ├── DriverLocationUpdate.java
│   ├── DriverRegistrationRequest.java
│   ├── ErrorResponse.java
│   ├── FareEstimate.java
│   ├── LoginRequest.java
│   ├── PaymentRequest.java
│   ├── PaymentResponse.java
│   ├── RatingRequest.java
│   ├── RatingResponse.java
│   ├── RegisterRequest.java
│   ├── RideRequest.java
│   ├── RideResponse.java
│   └── UserProfileResponse.java
├── entity
│   ├── Driver.java
│   ├── Payment.java
│   ├── Rating.java
│   ├── Ride.java
│   └── User.java
├── enums
│   ├── PaymentMethod.java
│   ├── PaymentStatus.java
│   ├── RideStatus.java
│   ├── UserRole.java
│   └── VehicleType.java
├── exception
│   ├── BadRequestException.java
│   ├── GlobalExceptionHandler.java
│   ├── PaymentException.java
│   ├── ResourceNotFoundException.java
│   ├── RideException.java
│   └── UnauthorizedException.java
├── repository
│   ├── DriverRepository.java
│   ├── PaymentRepository.java
│   ├── RatingRepository.java
│   ├── RideRepository.java
│   └── UserRepository.java
├── security
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtService.java
└── service
    ├── AuthService.java
    ├── DriverService.java
    ├── EmailService.java
    ├── InvoiceService.java
    ├── PaymentService.java
    ├── RatingService.java
    └── RideService.java
```

---

## 🚀 Getting Started

### 1. Prerequisites
- **JDK 17** or later
- **Maven 3.8+**
- **PostgreSQL 14+** running on `localhost:5432` with a database named `cab_booking_db`

### 2. Configure Database & Secrets
Update `src/main/resources/application.yml` or export environment variables:
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
export STRIPE_API_KEY=sk_test_...
```

### 3. Build & Run
```bash
mvn clean install
mvn spring-boot:run
```

The application runs on `http://localhost:8080/api/v1`.

---

## 🔑 Default Seed Credentials

Upon startup, `DataInitializer` seeds test accounts:

| Role | Email | Password |
|---|---|---|
| **Admin** | `admin@labmentix.com` | `Password123!` |
| **Rider** | `rider@labmentix.com` | `Password123!` |
| **Driver** | `driver@labmentix.com` | `Password123!` |

---

## 📖 API Documentation & Swagger UI

Once the application is started, visit:
- **Swagger UI**: [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)
- **OpenAPI v3 JSON**: [http://localhost:8080/api/v1/v3/api-docs](http://localhost:8080/api/v1/v3/api-docs)

---

## 📡 WebSocket Topics & Endpoints

- **WebSocket Handshake URL**: `ws://localhost:8080/api/v1/ws` (SockJS: `http://localhost:8080/api/v1/ws`)
- **Ride Request Broadcast**: `/topic/rides/requests`
- **Ride Status Updates**: `/topic/ride/{rideId}/status`
- **Driver GPS Location Streaming**: `/topic/driver/{driverId}/location`

---

## 💳 Payment Gateway

Supports Stripe PaymentIntents with automatic fallbacks for mock development testing. Supported payment methods:
- `CARD`
- `UPI`
- `WALLET`
- `CASH`

---

## 📄 PDF Invoice Generation

Call `GET /api/v1/invoices/ride/{rideId}` to stream a formatted ride tax invoice and receipt generated via iText.
