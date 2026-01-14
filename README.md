# Demo Pre-Authentication System

A demonstration project showing how to implement pre-authentication with Spring Security's
`PreAuthenticatedAuthenticationProvider`.

https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/preauth.html

## Project Structure

```
demo-preauth/
├── auth-system/    # Authentication server (port 9999)
└── demo-app/       # Client application (port 8080)
```

| Module      | Description                                    | Port |
|-------------|------------------------------------------------|------|
| auth-system | Authentication server with login and token API | 9999 |
| demo-app    | Protected application using pre-authentication | 8080 |

## Quick Start

### 1. Start auth-system

```bash
cd auth-system
./mvnw spring-boot:run
```

### 2. Start demo-app (in another terminal)

```bash
cd demo-app
./mvnw spring-boot:run
```

### 3. Open browser

Access http://localhost:8080/ and you will be redirected to auth-system for login.

## Test Users

| Username | Password    | Roles       |
|----------|-------------|-------------|
| `user1`  | `password1` | USER        |
| `admin1` | `password1` | USER, ADMIN |
| `user2`  | `password2` | USER        |

## Architecture

```
┌─────────┐      ┌──────────┐      ┌─────────────┐
│ Browser │      │ demo-app │      │ auth-system │
└────┬────┘      └────┬─────┘      └──────┬──────┘
     │                │                   │
     │  1. Access     │                   │
     ├───────────────>│                   │
     │                │                   │
     │  2. Redirect   │                   │
     │<───────────────┤                   │
     │                │                   │
     │  3. Login      │                   │
     ├────────────────────────────────────>
     │                │                   │
     │  4. Token      │                   │
     │<────────────────────────────────────┤
     │                │                   │
     │  5. Callback   │                   │
     ├───────────────>│                   │
     │                │                   │
     │                │  6. Validate      │
     │                ├──────────────────>│
     │                │<──────────────────┤
     │                │                   │
     │  7. Session    │                   │
     │<───────────────┤                   │
```

1. User accesses a protected page on demo-app
2. demo-app redirects unauthenticated user to auth-system
3. User logs in at auth-system
4. auth-system issues a one-time token and redirects back
5. Browser sends token to demo-app
6. demo-app validates token via auth-system API
7. demo-app creates session and serves the page

## Documentation

- [auth-system/README.md](auth-system/README.md) - Authentication server details
- [demo-app/README.md](demo-app/README.md) - Client application details

## Requirements

- Java 21+
