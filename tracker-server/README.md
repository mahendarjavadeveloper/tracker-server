# Tracker Server

Spring Boot API for authentication, device lifecycle, idempotent activity sync, heartbeat state, and the admin dashboard.

```powershell
mvn test
mvn package
```

Configure values from `.env.example`. The application updates the existing tracker tables and does not create a new application table.
