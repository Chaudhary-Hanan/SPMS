# Student Productivity Management System (SPMS)

A complete, beautiful JavaFX 17 desktop application for student productivity.

---

## Quick Start

### Prerequisites
- **JDK 17+** (JDK 21 or 25 recommended – all are compatible)
- **Maven 3.8+** (or use the included `mvnw` wrapper)

### Run from terminal
```bash
# From the project root directory:
mvn clean javafx:run
```

### Run from IntelliJ IDEA
1. Open the project (**File → Open** → select the project folder)
2. Let IntelliJ import Maven dependencies (may take a minute the first time)
3. Run **`SPMSApp`** (`src/main/java/com/spms/app/SPMSApp.java`)
   - Or use **Run → Edit Configurations → Add → Maven** with goal `javafx:run`

> **Tip:** If IntelliJ shows "Project SDK is not defined", set it to your JDK 17+ installation.

---

## Project Structure

```
src/main/java/
├── module-info.java
└── com/spms/
    ├── app/
    │   └── SPMSApp.java              ← Entry point
    ├── model/
    │   ├── Assignment.java
    │   ├── Exam.java
    │   ├── Goal.java
    │   ├── Note.java
    │   ├── Reminder.java
    │   ├── StudyBlock.java
    │   ├── StudyPlan.java
    │   ├── StudySession.java
    │   └── UserProfile.java
    ├── service/
    │   ├── DatabaseService.java      ← All SQLite persistence
    │   ├── TimerService.java         ← Pomodoro timer logic
    │   ├── AnalyticsService.java     ← Metrics computation
    │   └── GamificationService.java  ← Badges & streaks
    ├── view/
    │   ├── MainWindow.java           ← Shell + sidebar navigation
    │   ├── DashboardView.java
    │   ├── StudyPlannerView.java
    │   ├── FocusTimerView.java
    │   ├── GoalsView.java
    │   ├── NotesView.java
    │   ├── RemindersView.java
    │   ├── AnalyticsView.java
    │   └── ExamsView.java
    └── util/
        ├── DateUtil.java
        └── UIFactory.java

src/main/resources/com/spms/styles/
└── main.css                          ← Full dark-purple theme
```

---

## Data Storage

Data is persisted in a **SQLite database** at:
```
~/.spms/spms.db          (Windows: C:\Users\<you>\.spms\spms.db)
```
Sample data is automatically inserted on first launch so the app looks populated immediately.

---

## Features

| Module | What it does |
|---|---|
| **Dashboard** | Greeting, 4 stat cards (hours, tasks, goals, streak), deadline list, exam countdown, weekly study chart |
| **Study Planner** | Create plans with auto-generated daily study blocks; mark blocks complete |
| **Focus Timer** | Pomodoro timer (25/5/15 min) with circular progress ring, mood selector, configurable durations, session log |
| **Goals** | Daily/weekly/monthly goals with progress bars, badge indicators, and progress updates |
| **Notes** | Split-pane editor with search, subject filter, tagging, and auto-save |
| **Reminders** | Urgency-coded reminders with snooze, dismiss, and done actions |
| **Analytics** | Bar charts (weekly hours, subject distribution), productivity score ring, badges, weak-subject detector |
| **Exams** | Large live countdown for next exam; full exam table with sortable columns |

---

## Architecture

- **Pattern:** MVC — Models, Service layer, View layer
- **Persistence:** SQLite via `sqlite-jdbc`
- **UI:** Pure programmatic JavaFX (no FXML for new code)
- **Theme:** Dark purple CSS (`main.css`) applied globally
- **Navigation:** Sidebar buttons swap the center content pane with a fade transition
