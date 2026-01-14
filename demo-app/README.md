# Demo App

A demonstration application showing how to integrate with an external authentication system using Spring Security's
`PreAuthenticatedAuthenticationProvider`.

## Prerequisites

**auth-system** must be running before starting this application.
See [auth-system/README.md](../auth-system/README.md) for how to start it.

## Getting Started

### Option 1: Run with Maven

```bash
./mvnw spring-boot:run
```

### Option 2: Build and run JAR

```bash
./mvnw clean package -DskipTests
java -jar target/demo-app-0.0.1-SNAPSHOT.jar
```

The application starts on port 8080 by default.

Open http://localhost:8080/ in your browser. You will be redirected to auth-system for login.

## Test Users

| Username | Password    | Roles       |
|----------|-------------|-------------|
| `user1`  | `password1` | USER        |
| `admin1` | `password1` | USER, ADMIN |
| `user2`  | `password2` | USER        |

## Configuration

The following properties can be configured in `application.properties`:

| Property                       | Default Value            | Description                  |
|--------------------------------|--------------------------|------------------------------|
| `server.port`                  | `8080`                   | Server port                  |
| `demo.app.auth-system-url`     | `http://127.0.0.1:9999`  | URL of the auth-system       |
| `demo.app.auth-system-api-key` | `demo-shared-secret-key` | API key for token validation |

## Endpoints

| Path               | Access        | Description    |
|--------------------|---------------|----------------|
| `/`                | Authenticated | Home page      |
| `/dashboard`       | Authenticated | Dashboard page |
| `/admin`           | ADMIN role    | Admin page     |
| `/actuator/health` | Public        | Health check   |
| `/logout`          | Authenticated | Logout (POST)  |

## Authentication Flow

```
┌─────────┐      ┌──────────┐      ┌─────────────┐
│ Browser │      │ Demo App │      │ Auth System │
└────┬────┘      └────┬─────┘      └──────┬──────┘
     │                │                   │
     │ 1. GET /       │                   │
     ├───────────────>│                   │
     │                │                   │
     │ 2. 302 Redirect to auth-system     │
     │<───────────────┤                   │
     │                │                   │
     │ 3. GET /login?redirect=...         │
     ├────────────────────────────────────>
     │                │                   │
     │ 4. Login & authenticate            │
     │<────────────────────────────────────┤
     │                │                   │
     │ 5. 302 Redirect with token         │
     │<────────────────────────────────────┤
     │   /?token=xxx                       │
     │                │                   │
     │ 6. GET /?token=xxx                  │
     ├───────────────>│                   │
     │                │ 7. Validate token │
     │                ├──────────────────>│
     │                │<──────────────────┤
     │                │                   │
     │ 8. 302 Redirect to /               │
     │<───────────────┤                   │
     │                │                   │
     │ 9. GET / (with session)            │
     ├───────────────>│                   │
     │                │                   │
     │ 10. Home page  │                   │
     │<───────────────┤                   │
```

### Flow Description

1. **GET /**: Browser accesses a protected page on Demo App.

2. **302 Redirect to auth-system**: Demo App detects the user is not authenticated and redirects to auth-system's login page with a `redirect` parameter containing the original URL.

3. **GET /login?redirect=...**: Browser follows the redirect to auth-system's login page.

4. **Login & authenticate**: User enters credentials and submits the login form. auth-system validates the credentials.

5. **302 Redirect with token**: Upon successful authentication, auth-system generates a one-time token and redirects browser back to Demo App with the token as a query parameter.

6. **GET /?token=xxx**: Browser follows the redirect to Demo App with the token.

7. **Validate token**: Demo App's `TokenPreAuthenticatedFilter` extracts the token and calls auth-system's `/api/validate` endpoint to verify the token and retrieve user information.

8. **302 Redirect to /**: After successful validation, Demo App creates a session and redirects to the clean URL (without the token parameter).

9. **GET / (with session)**: Browser requests the page again, this time with a valid session cookie.

10. **Home page**: Demo App returns the home page to the authenticated user.

## Running Tests

```bash
./mvnw test
```

Integration tests use Testcontainers to run auth-system automatically.
