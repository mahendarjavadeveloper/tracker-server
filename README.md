# Tracker Server

Spring Boot API for authentication, device lifecycle, idempotent activity sync, heartbeat state, and the admin dashboard.

```powershell
mvn test
mvn package
```

Configure values from `.env.example`. The application updates the existing tracker tables and does not create a new application table.

Only exceptions are logged. The server writes `logs/windows-tracker-server.log`
and keeps up to three 2 MB files. Set `TRACKER_SERVER_LOG_FILE` to override the
file path.
