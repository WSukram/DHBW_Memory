# DHBW Memory

A Java-based **Memory** card game built with **Spring Boot** and **Vaadin**.
University exam project for **DHBW Ravensburg**, course *Programmieren Java*.

Runs locally as a single executable fat jar (embedded Tomcat → browser at
`localhost:8080`) and as a Docker container behind nginx at
<https://memory.walletpulse.de>.

---

## Quick start

You need **Java 21** and **Maven 3.9+**. If your default `java` is a newer
JDK (25+), pin Maven to JDK 21 first, otherwise the Vaadin frontend plugin
fails with *"Unsupported class file major version"*:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # macOS
# or:  export JAVA_HOME=/path/to/jdk-21            # Linux / Windows (Git Bash)
```

```bash
# Run from source (dev mode, hot reload)
mvn spring-boot:run
# → open http://localhost:8080

# Build the standalone executable fat jar.
# The "production" profile compiles the Vaadin frontend into the jar,
# so the resulting artifact runs without npm / Vite installed.
mvn clean package -P production
java -jar target/DHBW_Memory-1.0-SNAPSHOT.jar

# Generate JavaDoc → target/reports/apidocs/index.html
mvn javadoc:javadoc

# Run all tests (56 unit tests covering model + controller)
mvn test
```

### Docker

A multi-stage `Dockerfile` is included (`maven:3.9-eclipse-temurin-21` build →
`eclipse-temurin-21-jre-alpine` runtime):

```bash
docker build -t dhbw-memory .
docker run --rm -p 8080:8080 dhbw-memory
```

---

## Tech stack

| Component | Choice |
|---|---|
| Language | Java 21 |
| Build | Maven |
| Server | Spring Boot 3.4 (embedded Tomcat) |
| UI | Vaadin 24 (pure-Java web UI) |
| Tests | JUnit 5 |

Vaadin lets the entire UI be written in Java — no separate frontend project
to maintain — while still producing a real web application that runs in any
modern browser.

---

## Architecture

The codebase follows a strict **MVC** layering. The `model` package contains
pure Java and **must not import Spring or Vaadin**; this keeps the game logic
testable and framework-independent.

```
src/main/java/de/dhbw/memory/
├── MemoryApplication.java          @SpringBootApplication entry point
├── model/                          pure Java — Card, Player, Board, Game, Theme, FlipResult
├── controller/
│   └── GameService.java            @Service @UIScope — orchestrates moves, async flip-back
└── view/                           Vaadin views; never mutates model state directly
    ├── StartView.java              @Route("")        configuration screen
    ├── GameView.java               @Route("game")    board + status bar + dialogs
    ├── component/                  MemoryCard, SegmentedControl
    └── dialog/                     HelpDialog, QuitConfirmDialog, EndGameDialog
```

**UML class diagram:**
- v2 — color-coded by layer: [`docs/uml/class-diagram-v2.png`](docs/uml/class-diagram-v2.png) (source: [`class-diagram-v2.puml`](docs/uml/class-diagram-v2.puml))
- v1 — auto-layout reference: [`docs/uml/class-diagram-v1.png`](docs/uml/class-diagram-v1.png) (source: [`class-diagram-v1.puml`](docs/uml/class-diagram-v1.puml))

---

## Themes

Three card themes are bundled — every motif is a custom SVG that renders
crisply on the white card face.

| Theme | Motifs (4×4 / 6×6) |
|---|---|
| **Crypto** | Bitcoin, Ethereum, Solana, … (8 / 18 coins) |
| **Languages** | Java, Python, JavaScript, … (8 / 18 programming languages) |
| **Space** | Rocket, Astronaut, Moon, … (8 / 18 space motifs) |

The card back is a unified WalletPulse-branded SVG.

---

**Course**: Programmieren – Java · **University**: DHBW Ravensburg Campus Friedrichshafen · **Author**: Markus Wenninger
