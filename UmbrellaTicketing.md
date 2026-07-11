# Ticketing System — Real-Time Ticket Management

A distributed desktop system for managing basketball tournament tickets, built in **both Java and C#** to explore gRPC and real-time client-server communication across two ecosystems. Connected clients receive live seat-availability updates the moment a sale happens.

## Two Implementations

| Version | Stack | Repository |
|---------|-------|------------|
| **Java** | JavaFX, gRPC, Protocol Buffers, SQLite/JDBC, CompletableFuture | [Ticketing-App-Java](https://github.com/claudiu28/Ticketing-App-Java) |
| **C#** | Windows Forms, gRPC, EF Core, SQLite, Next.js admin, Swagger | [Ticketing-App-Csharp](https://github.com/claudiu28/Ticketing-App-Csharp) |

Both versions share the same core design (gRPC contracts, Observer/Proxy pattern for live updates, JWT + BCrypt authentication), implemented idiomatically in each language.

---

## Core Features

- **Authentication** — cashiers log in with username and password (stored as BCrypt hashes; hashes never returned in responses).
- **Ticket sales** — sell tickets by customer name, address, and number of seats; sold-out matches are flagged.
- **Real-time updates** — every connected client instantly sees updated seat availability via the Observer/Proxy pattern over gRPC streaming.
- **Search** — look up tickets by customer name and/or address.
- **Session handling** — clean login/logout with session tracking to prevent duplicate logins.

The C# version additionally includes a **Next.js admin module** for match CRUD (teams, stage, price, capacity, date/time), secured with JWT and documented with Swagger/OpenAPI.

---

## Tech Highlights

- **gRPC + Protocol Buffers** for efficient, typed client-server communication.
- **Observer/Proxy pattern** to broadcast live seat updates to all connected clients.
- **Asynchronous processing** — CompletableFuture (Java) / Task-based async (C#).
- **Security** — JWT authentication and BCrypt password hashing.
- **Persistence** — SQLite via JDBC (Java) / EF Core (C#).

---

## Architecture

The system follows a client-server model: a desktop client (JavaFX or Windows Forms) communicates with a gRPC server. When a ticket is sold, the server pushes the updated seat count to every connected client through the Observer/Proxy pattern, keeping all views in sync in real time.

```
Desktop Client(s)  <--- gRPC streaming --->  Server  <--->  SQLite
   (JavaFX / WinForms)                     (Observer/Proxy)
```

---

## Getting Started

Each version has its own setup instructions in its repository:
- **Java:** [Ticketing-App-Java](https://github.com/claudiu28/Ticketing-App-Java)
- **C#:** [Ticketing-App-Csharp](https://github.com/claudiu28/Ticketing-App-Csharp)

---

*Built as a personal project to explore gRPC, real-time communication, and design patterns across the Java and .NET ecosystems.*
