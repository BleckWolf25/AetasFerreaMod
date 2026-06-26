# Contributing to Aetas Ferrea Mod

First off, thank you for taking the time to contribute! Contributions from the community help make Aetas Ferrea Mod a more realistic, immersive, and polished medieval survival experience for everyone.

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

---

## Table of Contents

1. [How Can I Contribute?](#how-can-i-contribute)
   - [Reporting Bugs](#reporting-bugs)
   - [Suggesting Enhancements](#suggesting-enhancements)
   - [Pull Requests](#pull-requests)
2. [Development Setup](#development-setup)
   - [Prerequisites](#prerequisites)
   - [Setting Up Your Workspace](#setting-up-your-workspace)
   - [Gradle Development Tasks](#gradle-development-tasks)
3. [Style & Code Guidelines](#style--code-guidelines)
   - [Java Coding Style](#java-coding-style)
   - [Minecraft/Forge Best Practices](#minecraftforge-best-practices)
   - [Commit Messages](#commit-messages)
4. [Security Vulnerabilities](#security-vulnerabilities)

---

## How Can I Contribute?

### Reporting Bugs

We use structured GitHub Issue Forms to track bug reports. Before submitting a bug report, please:

1. Check the existing issues to ensure it hasn't been reported or resolved already.
2. Test on a clean environment without conflicting optimization/overhaul mods unless the bug is directly related to compatibility.
3. Fill out the [Bug Report Template](https://github.com/BleckWolf25/AetasFerreaMod/issues/new?template=bug_report.yml) completely, including:
   - Minecraft, Forge, and Mod versions.
   - Step-by-step instructions to reproduce the issue.
   - A link to your `latest.log`, `debug.log`, or crash report.

### Suggesting Enhancements

If you have ideas for new features, realism mechanics, or balance adjustments:

1. Search the issues to verify your suggestion hasn't been discussed before.
2. Open a [Feature Request](https://github.com/BleckWolf25/AetasFerreaMod/issues/new?template=feature_request.yml) describing the feature, the problem it solves, and how it might be implemented.

### Pull Requests

To submit code changes:

1. **Fork** the repository and create your branch from `main` or the active development branch (e.g., `feature/your-feature-name` or `bugfix/issue-description`).
2. Make your changes, keeping them focused. Avoid unrelated changes.
3. Write clean, readable code following our guidelines.
4. Ensure your changes compile and pass any tests locally.
5. Submit a Pull Request (PR) with a clear description of the changes and references to any related issues.

---

## Development Setup

Aetas Ferrea is built on Minecraft **1.20.1** using the **Minecraft Forge** framework.

### Prerequisites

- **Java Development Kit (JDK) 17**: Ensure you have JDK 17 installed and configured in your environment variables (`JAVA_HOME`). We recommend Eclipse Adoptium.
- **Git**: Installed and configured on your system.
- **An IDE**: IntelliJ IDEA (highly recommended) or Eclipse.

### Setting Up Your Workspace

1. **Clone the repository:**

   ```bash
   git clone https://github.com/BleckWolf25/AetasFerreaMod.git
   cd AetasFerreaMod
   ```

2. **Import into your IDE:**
   - **IntelliJ IDEA:**
     1. Open IntelliJ.
     2. Choose **Open** and select the project's root directory containing the `build.gradle` file.
     3. Allow Gradle to import and index the project (this may take a few minutes as Minecraft assets are downloaded).
     4. Go to `Gradle` tool window -> `Tasks` -> `forgegradleruns` and run `genIntellijRuns` to generate the run configurations.
   - **Eclipse:**
     1. Run `./gradlew eclipse` in your terminal.
     2. Import the project as an existing Eclipse project.

### Gradle Development Tasks

Use the following Gradle wrapper commands in your project root:

- **Generate Run Configurations:**
  - Windows: `.\gradlew.bat genIntellijRuns` or `.\gradlew.bat eclipse`
  - Bash: `./gradlew genIntellijRuns` or `./gradlew eclipse`
- **Run the Client (for testing):**
  - Windows: `.\gradlew.bat runClient`
  - Bash: `./gradlew runClient`
- **Run the Server (for multiplayer testing):**
  - Windows: `.\gradlew.bat runServer`
  - Bash: `./gradlew runServer`
- **Generate Data (Datagen for tags, recipes, assets):**
  - Windows: `.\gradlew.bat runData`
  - Bash: `./gradlew runData`
- **Build the Mod Jar:**
  - Windows: `.\gradlew.bat build`
  - Bash: `./gradlew build`
  - The compiled jar will be output to `build/libs/`.

---

## Style & Code Guidelines

### Java Coding Style

To keep the codebase uniform and easy to read:

- **Indentation:** Use 4 spaces for indentation. Do not use tabs.
- **Naming Conventions:**
  - Classes and Interfaces: `PascalCase`
  - Methods and Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase.with.dots` (e.g., `com.aetasferrea.aetasferreamod`)
- **Braces:** Use standard Egyptian brackets style:

  ```java
  public class Example {
      public void doSomething() {
          if (condition) {
              // code
          } else {
              // code
          }
      }
  }
  ```

- **Comments:** Retain existing comments and docstrings. Add JavaDoc to new public classes or complex methods.

### Minecraft/Forge Best Practices

- **Registry Entries:** Use `DeferredRegister` for registering Blocks, Items, Entities, and other registry entries.
- **Side Safety:** Be careful with Client-Only vs. Server-Only classes. Do not call client-only code (e.g., rendering, Minecraft client instances, textures) from common code. Use `@OnlyIn` or physical side separation (such as registering client-side events in a client setup class) to avoid crashes on servers.
- **Datagen:** Do not write JSON files for recipes, tags, loot tables, or block states by hand if they can be generated. Use the built-in Minecraft data generators (`src/generated/resources`) and run `runData` to update them.

### Commit Messages

Use clear and descriptive commit messages. We recommend using prefix tags for commits, such as:

- `feat: ...` for a new feature.
- `fix: ...` for a bug fix.
- `docs: ...` for documentation changes.
- `refactor: ...` for code style or internal design changes.
- `style: ...` for formatting fixes.

Example:

```text
feat: add custom attributes to Palfrey horse class
```

---

## Security Vulnerabilities

Please do not report security vulnerabilities in public issues. Refer to our [Security Policy](SECURITY.md) for instructions on how to report security issues privately.
