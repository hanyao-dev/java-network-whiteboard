# 🎨 Java Network Whiteboard

> A production-style Java networking application demonstrating TCP/UDP socket programming, multithreading, concurrent programming, and application-layer protocol design.

![Java](https://img.shields.io/badge/Java-17-orange)
![Maven](https://img.shields.io/badge/Maven-3.9-red)
![TCP](https://img.shields.io/badge/TCP-Socket-blue)
![UDP](https://img.shields.io/badge/UDP-Multicast-blueviolet)
![Concurrency](https://img.shields.io/badge/Concurrency-Multithreading-success)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

---

## 📖 Overview

Java Network Whiteboard is a production-style Java networking application built with Java Core.

The project demonstrates modern software engineering practices including:

- TCP Socket Programming
- UDP Multicast Peer Discovery
- Client / Server Architecture
- Application-layer Protocol Design
- Multithreading
- Concurrent Programming
- Swing User Interface
- Layered Architecture

This project is continuously improved as part of my Java software engineering portfolio.

---

# ✨ Features

### Networking

- TCP Client / Server Communication
- UDP Peer Discovery
- Reliable Message Synchronization
- Connection Management

### Whiteboard

- Real-time Drawing Synchronization
- Multi-user Collaboration
- Canvas Clear Synchronization

### Chat

- Real-time Chat
- Join / Leave Notification

### Software Engineering

- Layered Architecture
- Thread-safe Services
- ExecutorService
- Graceful Shutdown

---

# 🛠 Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 17 |
| Networking | TCP Socket / UDP Multicast |
| UI | Swing |
| Concurrency | ExecutorService / ConcurrentHashMap |
| Build Tool | Maven |
| Testing | JUnit 5 |
| Version Control | Git / GitHub |

---

# 📂 Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.hanyao.whiteboard
│   │       ├── config
│   │       ├── model
│   │       ├── network
│   │       ├── protocol
│   │       ├── service
│   │       ├── ui
│   │       └── util
│   └── resources
```

---

# 🏗 Architecture

```text
Swing UI
    │
    ▼
Service
    │
    ▼
Protocol
    │
    ▼
Network
 ┌─────────────┐
 │ TCP │ UDP   │
 └─────────────┘
    │
    ▼
Remote Peers
```

---

# 🚀 Getting Started

## 1. Clone Repository

```bash
git clone https://github.com/hanyao-dev/java-network-whiteboard.git
```

---

## 2. Build Project

```bash
mvn clean compile
```

---

## 3. Run Tests

```bash
mvn test
```

---

## 4. Run Application

Server

```bash
mvn exec:java "-Dexec.args=--server 5050 alice"
```

Client

```bash
mvn exec:java "-Dexec.args=--connect localhost 5050 bob"
```

---

# 🌐 Network Protocol

## TCP

- JOIN
- LEAVE
- DRAW
- CHAT
- CLEAR
- PING
- PONG

## UDP

- Peer Discovery
- Broadcast / Multicast Announcement

### Message Format

```text
JOIN|username|ip|port
LEAVE|username
DRAW|x1|y1|x2|y2|color|strokeWidth
CHAT|username|message
CLEAR|username
PING|username
PONG|username
```

---

# 📌 Current Progress

- ✅ TCP Socket Communication
- ✅ UDP Peer Discovery
- ✅ Client / Server Architecture
- ✅ Application-layer Protocol
- ✅ Swing User Interface
- ✅ Multithreading
- ✅ Thread-safe Services
- ✅ Layered Architecture

---

# 📅 Roadmap

## Version 1.1

- [ ] Improve Whiteboard Synchronization
- [ ] Better Peer Discovery
- [ ] Message Validation
- [ ] Logging

## Version 1.2

- [ ] Unit Test
- [ ] Integration Test
- [ ] GitHub Actions
- [ ] Maven Wrapper

## Version 2.0

- [ ] Undo / Redo
- [ ] File Sharing
- [ ] TLS Encryption
- [ ] Authentication
- [ ] Docker Deployment

---

# 👨‍💻 Author

**Han Yao**

- GitHub: https://github.com/hanyao-dev

---

# ⭐ Why This Project?

This project is built as a long-term Java networking portfolio to demonstrate production-style software engineering practices, including socket programming, TCP/UDP communication, concurrent programming, application-layer protocol design, and clean software architecture.
