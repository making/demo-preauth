# Auth System

An authentication system for demonstrating Spring Security's `PreAuthenticatedAuthenticationProvider`.
Provides user authentication, token issuance, and token validation functionality.

## Getting Started

### Option 1: Run with Maven

```bash
./mvnw spring-boot:run
```

### Option 2: Build and run JAR

```bash
./mvnw clean package -DskipTests
java -jar target/auth-system-0.0.1-SNAPSHOT.jar
```

The server starts on port 9999 by default.

## Configuration

The following properties can be configured in `application.properties`:

| Property                               | Default Value            | Description                                             |
|----------------------------------------|--------------------------|---------------------------------------------------------|
| `server.port`                          | `9999`                   | Server port                                             |
| `auth.system.api-secret`               | `demo-shared-secret-key` | API key for token validation                            |
| `auth.system.token-expiry`             | `5m`                     | Token expiry duration (e.g., `5m`, `1h`, `30s`)         |
| `auth.system.allowed-redirect-origins` | `http://localhost:8080`  | Allowed redirect origins (comma-separated for multiple) |

## Test Users

| Username | Password    | Display Name | Roles       |
|----------|-------------|--------------|-------------|
| `user1`  | `password1` | User One     | USER        |
| `admin1` | `password1` | Admin One    | USER, ADMIN |
| `user2`  | `password2` | User Two     | USER        |

## Endpoints

### Login Page

```
GET /login?redirect={redirectUrl}
```

**Parameters:**

- `redirect` (optional): URL to redirect to after successful login

**Behavior:**

- With `redirect` parameter: Redirects to the specified URL with a token after successful login
- Without `redirect` parameter: Displays the internal home page after successful login

**Redirect URL format:**

```
{redirectUrl}?token={generatedToken}
```

**Security:**

- Redirect destination must be in `allowed-redirect-origins`
- Only `http` or `https` schemes are allowed

### Token Validation API

```
GET /api/validate?token={token}
```

**Request Headers:**

- `X-API-Key`: API secret key (required)

**Response:**

Success (HTTP 200):

```json
{
  "valid": true,
  "username": "user1",
  "displayName": "User One",
  "roles": [
    "USER"
  ]
}
```

Failure (HTTP 200):

```json
{
  "valid": false,
  "reason": "TOKEN_NOT_FOUND"
}
```

**Failure Reasons:**

- `TOKEN_NOT_FOUND`: Token does not exist
- `TOKEN_EXPIRED`: Token has expired
- `TOKEN_ALREADY_USED`: Token has already been used
- `USER_NOT_FOUND`: User not found

**Authentication Error (HTTP 401):**

- Returned when `X-API-Key` header is missing or invalid

## Integration Flow

```
┌──────────┐     ┌─────────────┐     ┌──────────────┐
│  Client  │     │  Your App   │     │  Auth System │
└────┬─────┘     └──────┬──────┘     └──────┬───────┘
     │                  │                   │
     │ 1. Access        │                   │
     ├─────────────────>│                   │
     │                  │                   │
     │ 2. Redirect to Auth System           │
     │<─────────────────┤                   │
     │                  │                   │
     │ 3. GET /login?redirect=http://yourapp/callback
     ├──────────────────────────────────────>
     │                  │                   │
     │ 4. Login Form    │                   │
     │<──────────────────────────────────────┤
     │                  │                   │
     │ 5. POST /login (credentials)         │
     ├──────────────────────────────────────>
     │                  │                   │
     │ 6. Redirect with token               │
     │<──────────────────────────────────────┤
     │   http://yourapp/callback?token=xxx  │
     │                  │                   │
     │ 7. Callback with token               │
     ├─────────────────>│                   │
     │                  │                   │
     │                  │ 8. Validate token │
     │                  ├──────────────────>│
     │                  │   GET /api/validate?token=xxx
     │                  │   X-API-Key: secret
     │                  │                   │
     │                  │ 9. User info      │
     │                  │<──────────────────┤
     │                  │                   │
     │ 10. Authenticated│                   │
     │<─────────────────┤                   │
     │                  │                   │
```

### Flow Description

1. **Access**: The client (browser) accesses a protected resource on your application.

2. **Redirect to Auth System**: Your application detects the user is not authenticated and redirects the client to the Auth System's login page, including a `redirect` parameter with the callback URL.

3. **GET /login**: The client follows the redirect and requests the login page from the Auth System. The `redirect` parameter specifies where to return after successful authentication.

4. **Login Form**: The Auth System returns the login form to the client.

5. **POST /login**: The user enters their credentials and submits the form. The Auth System validates the username and password.

6. **Redirect with token**: Upon successful authentication, the Auth System generates a one-time token and redirects the client back to your application's callback URL with the token as a query parameter.

7. **Callback with token**: The client follows the redirect to your application's callback endpoint, passing the token.

8. **Validate token**: Your application extracts the token from the request and calls the Auth System's validation API (`GET /api/validate`) with the `X-API-Key` header for authentication.

9. **User info**: The Auth System validates the token and returns user information (username, display name, roles). The token is marked as used and cannot be reused.

10. **Authenticated**: Your application creates a session for the user based on the returned user information and grants access to the protected resource.

## Usage Example

### Token Validation with cURL

```bash
curl -H "X-API-Key: demo-shared-secret-key" \
  "http://localhost:9999/api/validate?token=YOUR_TOKEN"
```
