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

# Run all tests (59 unit tests covering model + controller)
mvn test
# or: ./mvnw test     # uses the bundled Maven wrapper, no global mvn required
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

## Tests

**59 JUnit 5 tests** covering the game logic (model) and the orchestrator
(controller). The model layer is pure Java, so tests run in milliseconds
without spinning up Spring or Vaadin.

| Suite | Package | Tests |
|---|---|---|
| `GameServiceTest` | `controller` | 12 |
| `GameTest` | `model` | 19 |
| `BoardTest` | `model` | 7 |
| `CardTest` | `model` | 7 |
| `PlayerTest` | `model` | 7 |
| `ThemeTest` | `model` | 7 |

```bash
./mvnw test    # → 59 tests, 0 failures, 0 errors, 0 skipped
```

A browsable HTML test report (per-suite breakdown + full `./mvnw test`
console output) is bundled into the fat jar and served at
[`/tests/index.html`](http://localhost:8080/tests/index.html) once the
app is running. JavaDoc is bundled the same way at
[`/docs/index.html`](http://localhost:8080/docs/index.html).

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

## Submission checklist

Run these steps **in order, immediately before sending the submission
email**. The test report page (`/tests/index.html`) embeds the run date
and the full Maven console log, so doing it last keeps that date current.

```bash
# 0. Pin JDK 21 (Vaadin frontend can't parse JDK 25 bytecode)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# 1. Run all tests — must end with "Tests run: 59, Failures: 0"
./mvnw test
```

**2. Update the test report page** at
`src/main/resources/static/tests/index.html`:
- the subtitle date near the top: `… last run YYYY-MM-DD`
- the `[INFO] Finished at: …` line at the bottom of the embedded Maven log
- (any test count / suite time numbers if they drifted)

```bash
# 3. Regenerate JavaDoc and copy it into the jar's static resources
rm -rf src/main/resources/static/docs
mvn javadoc:javadoc
cp -R target/reports/apidocs/. src/main/resources/static/docs/

# 4. Repack the JavaDoc zip (one of the email attachments)
rm -f javadoc.zip
( cd target/reports/apidocs && zip -qr ../../../javadoc.zip . )

# 5. Build the production fat jar — bundles the fresh docs + tests page
mvn clean package -P production -DskipTests

# 6. Refresh the abgabe/ folder (the three email attachments)
cp target/DHBW-Memory-Markus-Wenninger.jar abgabe/
cp javadoc.zip abgabe/
```

After step 6, `abgabe/` contains exactly the three files to attach:

| File | Purpose |
|---|---|
| `DHBW-Memory-Markus-Wenninger.jar` | executable Java application |
| `class-diagramm-v4.png` | UML class diagram |
| `javadoc.zip` | packed JavaDoc |

---

**Course**: Programmieren – Java · **University**: DHBW Ravensburg Campus Friedrichshafen · **Author**: Markus Wenninger
