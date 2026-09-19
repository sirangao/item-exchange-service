# Building a Spring Boot + MySQL REST API from Scratch

**A complete, beginner-friendly walkthrough** — from an empty folder to a running REST API backed by a real MySQL database, with your Java entity classes generated automatically from your database tables.

This guide documents exactly how the `itemExchange` project was built, in enough detail that you can replicate the whole thing for a project of your own.

---

## Who this guide is for

You should be comfortable with:

- **Java basics** — classes, methods, packages, `import` statements
- **HTTP basics** — what GET and POST are, what a URL path looks like
- **SQL basics** — `CREATE TABLE`, `SELECT`, primary keys, foreign keys

You do **not** need to know anything about Spring Boot, Maven, Jakarta EE, JPA, or Hibernate. Every one of those is explained the first time it comes up.

## What you'll have at the end

```
Browser / curl  ──HTTP──▶  Jersey resource class  ──▶  Spring Data repository
                              (@Path, @GET)              (UsersRepository)
                                                              │
                                                              ▼
                                                   Hibernate (the ORM)
                                                              │
                                                              ▼
                                                    MySQL  (web_exchange)
```

You'll type `http://localhost:9090/api/users/id/5` into a browser and get back a row from your MySQL database as JSON — without writing a single line of SQL in Java.

## Versions used in this guide

Pin these if you want an identical experience. Newer versions usually work, but the Spring Boot version in particular changes property names and Java requirements between major releases.

| Tool / library | Version here | Notes |
| --- | --- | --- |
| Java (JDK) | **17** | Spring Boot 4 requires 17 or newer |
| Spring Boot | **4.0.7** | Chosen in the Initializr |
| Maven | **3.9.16** | Comes bundled — you don't install it (see [1.5](#15-the-maven-wrapper-mvnw)) |
| MySQL Server | **8.4** | 8.0 works identically for this guide |
| MySQL JDBC driver | `mysql-connector-j` **9.7.0** | The Java ↔ MySQL bridge. Spring Boot picks this version; the Hibernate Tools plugin pins its own copy, 9.2.0 ([2.3](#23-add-the-hibernate-tools-plugin)) |
| Hibernate Tools Maven plugin | **7.4.5.Final** | Generates entity classes from your DB |
| Jakarta REST (JAX-RS) API | **4.0.0** | The `@Path` / `@GET` annotations |
| Lombok | **1.18.46** | Writes the getters and setters for the DTO classes ([2.4](#24-add-lombok)) |

---

## Table of contents

- [Part 0 — Install the tools](#part-0--install-the-tools)
- [Part 1 — Generate the project with Spring Initializr](#part-1--generate-the-project-with-spring-initializr)
- [Part 2 — Understanding and finishing the `pom.xml`](#part-2--understanding-and-finishing-the-pomxml)
- [Part 3 — Set up the MySQL database](#part-3--set-up-the-mysql-database)
- [Part 4 — Connect Spring Boot to MySQL](#part-4--connect-spring-boot-to-mysql)
- [Part 5 — Generate your entity classes with Hibernate](#part-5--generate-your-entity-classes-with-hibernate)
- [Part 6 — Expose the data over HTTP](#part-6--expose-the-data-over-http)
- [Part 7 — Everyday workflow](#part-7--everyday-workflow)
- [Part 8 — The web frontend](#part-8--the-web-frontend)
- [Troubleshooting](#troubleshooting)
- [Glossary](#glossary)
- [Quick reference card](#quick-reference-card)

---

# Part 0 — Install the tools

You need three things installed before you start: a **JDK**, **MySQL**, and an **IDE**.

## 0.1 Install a JDK (Java 17 or newer)

A **JDK** (Java Development Kit) is the compiler plus the runtime. A "JRE" alone is not enough.

**macOS** (using [Homebrew](https://brew.sh)):

```bash
brew install openjdk@17
```

Homebrew will print a `sudo ln -sfn ...` command at the end — run it, or macOS won't find the JDK.

**Windows**: download the **MSI installer** for JDK 17 from [Adoptium](https://adoptium.net/temurin/releases/?version=17) and check the box for "Set JAVA_HOME variable" during install.

**Linux (Debian/Ubuntu)**:

```bash
sudo apt install openjdk-17-jdk
```

**Verify it:**

```bash
java -version
```

You should see something like `java version "17.0.20"`. If you see "command not found", the JDK isn't on your `PATH` — reopen your terminal, and if that fails, revisit the installer notes.

> **Why 17?** Spring Boot 4 refuses to run on anything older. If you see an `UnsupportedClassVersionError` later, it's almost always a Java version mismatch.

## 0.2 Install MySQL Server

**macOS**:

```bash
brew install mysql
```

**Windows**: download the [MySQL Installer](https://dev.mysql.com/downloads/installer/), choose the "Server only" or "Developer Default" setup, and **write down the root password** it asks you to create.

**Linux (Debian/Ubuntu)**:

```bash
sudo apt install mysql-server
```

**Verify it:**

```bash
mysql --version
```

## 0.3 Install an IDE

**IntelliJ IDEA Community Edition** (free) is the smoothest choice for Spring Boot — it understands Maven projects natively and gives you a green ▶ Run button. [Download it here](https://www.jetbrains.com/idea/download/). VS Code with the "Extension Pack for Java" also works.

---

# Part 1 — Generate the project with Spring Initializr

## 1.1 What Spring Initializr actually is

**Spring Initializr** is a website — [start.spring.io](https://start.spring.io) — that generates a ZIP file containing an empty but correctly-wired Spring Boot project.

It is *not* magic and it is *not* required. You could create every file by hand. But a Spring Boot project needs a build file with a dozen exactly-right version numbers, a specific folder layout, and a startup class in the right package — and the Initializr gets all of that correct on the first try. **Everyone starts here.**

You only use it **once per project**, at the very beginning. Anything you forget to add can be added later by editing one file (`pom.xml`), which [Part 2](#part-2--understanding-and-finishing-the-pomxml) covers.

## 1.2 Fill in the form

Go to **[start.spring.io](https://start.spring.io)**. The page has four boxes. Here's every field, what it means, and what to enter.

### Left column — Project, Language, Spring Boot

| Field | Choose | What it means |
| --- | --- | --- |
| **Project** | `Maven` | The build tool. Maven reads a file called `pom.xml` that lists your libraries, then downloads them for you. The alternative, Gradle, is equally good but this guide is Maven-only. |
| **Language** | `Java` | (The other options are Kotlin and Groovy.) |
| **Spring Boot** | `4.0.7` | Pick the highest version **without** `(SNAPSHOT)` or `(M1)`/`(RC1)` next to it — those are unreleased previews. If 4.0.7 isn't listed anymore, take the newest stable 4.0.x. |

### Right column — Project Metadata

These fields decide your package names and your folder names. Get them right now; renaming later is annoying.

| Field | Example value | What it means |
| --- | --- | --- |
| **Group** | `com.siran` | Your organization's reverse-domain identifier. If you own `example.com`, use `com.example`. If you own nothing, use `com.yourname` — it only has to be unique-ish. |
| **Artifact** | `itemExchange` | The project's short name. This becomes the folder name and the JAR file name. |
| **Name** | `itemExchange` | Human-readable name. Auto-fills from Artifact; leave it. |
| **Description** | anything | Free text. Optional. |
| **Package name** | `com.siran.itemExchange` | **The root Java package for all your code.** Initializr auto-fills this as `Group` + `Artifact` but **lowercases it** — so it will suggest `com.siran.itemexchange`. If you want the capital E, type it in manually. |
| **Packaging** | `Jar` | `Jar` produces a self-contained runnable file with a web server inside it. `War` is for deploying into an external server like Tomcat — you almost certainly don't want that. **Choose Jar.** |
| **Java** | `17` | Must match (or be lower than) the JDK you installed in Part 0. |

> ⚠️ **Careful with the package name.** Every class you write must live in this package or a sub-package of it (`com.siran.itemExchange.resource`, `com.siran.itemExchange.dataObjects`, …). Spring Boot only auto-discovers your classes inside this package tree. This trips up nearly every beginner exactly once — see [Troubleshooting](#troubleshooting).

### Dependencies box

Click **ADD DEPENDENCIES** (top right, or `Ctrl`/`⌘` + `B`), then search for and select each of these four:

| Dependency | Category | What it gives you |
| --- | --- | --- |
| **Spring Web** | Web | An embedded Tomcat web server so your app can answer HTTP requests, plus Jackson (the library that turns Java objects into JSON). |
| **Jersey** | Web | An implementation of **JAX-RS / Jakarta REST** — the annotation style (`@Path`, `@GET`, `@PathParam`) used to define endpoints in this project. |
| **Spring Data JPA** | SQL | The database layer. Bundles **Hibernate** (the ORM), and lets you define a database query by writing a Java interface with no body. |
| **MySQL Driver** | SQL | The JDBC driver — the actual code that speaks MySQL's wire protocol over a socket. Without it, Java literally cannot talk to MySQL. |

<details>
<summary><b>Do I really need both Spring Web and Jersey?</b> (click to expand)</summary>

They're two different ways to write REST endpoints:

- **Spring Web (Spring MVC)** uses `@RestController` + `@GetMapping("/hello")`
- **Jersey (Jakarta REST)** uses `@Path("/users")` + `@GET`

This project uses **Jersey** as the primary style because the Jakarta REST annotations are a vendor-neutral standard. Spring Web is still included because the Jersey starter needs a servlet container to run inside, and because Jackson (JSON conversion) comes with it.

Having both on the classpath is supported, but there's a gotcha: by default Jersey grabs *every* URL (`/*`), so a Spring MVC `@RestController` can end up unreachable. This project avoids that by mounting Jersey under `/api`; [Part 6.5](#65-jersey-and-spring-mvc-in-the-same-app) shows how.

If you'd rather use only Spring MVC, skip Jersey entirely and use `@RestController`. Everything else in this guide — MySQL, Hibernate, entity generation — is unchanged.
</details>

### What "starter" means

You'll notice the generated `pom.xml` says `spring-boot-starter-web`, not `tomcat` and `jackson` and `spring-webmvc`. A **starter** is a bundle: one dependency line that pulls in a dozen libraries known to work together at compatible versions. That's the single biggest thing Spring Boot does for you.

## 1.3 Generate and open the project

1. Click **GENERATE** (or `Ctrl`/`⌘` + `↵`). A ZIP downloads.
   - 💡 Curious first? Click **EXPLORE** (`Ctrl`/`⌘` + `Space`) to browse the files in the browser before downloading.
2. Unzip it somewhere sensible — e.g. `~/projects/itemExchange`. **Do not leave it inside your Downloads folder.**
3. Open your IDE → **File → Open** → select the **`itemExchange` folder itself** (not a file inside it).
4. Wait. IntelliJ will show a progress bar in the bottom-right saying it's resolving Maven dependencies. **The first time, this downloads ~100 MB and takes several minutes.** Let it finish before touching anything.

## 1.4 What you just got

```
itemExchange/
├── pom.xml                  ← the build file: your dependency list. You'll edit this a lot.
├── mvnw, mvnw.cmd           ← the Maven wrapper scripts (see below)
├── .mvn/wrapper/            ← config for the wrapper
├── .gitignore               ← files Git should ignore (target/, IDE files)
├── HELP.md                  ← links to Spring docs. Safe to delete.
└── src/
    ├── main/
    │   ├── java/com/siran/itemExchange/
    │   │   └── ItemExchangeApplication.java   ← the entry point: has main()
    │   └── resources/
    │       ├── application.properties         ← all runtime configuration
    │       ├── static/                        ← optional: css/js/images
    │       └── templates/                     ← optional: server-rendered HTML
    └── test/
        └── java/com/siran/itemExchange/
            └── ItemExchangeApplicationTests.java
```

Two folders that appear later and that you should never edit or commit:

- **`target/`** — everything Maven compiles. Deleting it is always safe.
- **`~/.m2/repository/`** — your local cache of every downloaded library, shared across all your projects.

### The entry point

```java
package com.siran.itemExchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ItemExchangeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItemExchangeApplication.class, args);
    }
}
```

That's a normal Java `main` method. The single annotation `@SpringBootApplication` does three things:

1. **Component scanning** — searches this class's package and everything below it for your classes, and creates one instance of each ("bean") that it manages for you.
2. **Auto-configuration** — looks at what's on your classpath and configures it. *It sees the MySQL driver, so it builds a database connection pool. It sees Jersey, so it registers the Jersey servlet.* This is why you'll write so little configuration code.
3. **Configuration** — marks this class as a place where you can define beans by hand.

> 🔑 **This is the most important idea in Spring Boot:** you don't wire things together. You put a library on the classpath and set a few properties, and Spring configures it.

## 1.5 The Maven wrapper (`mvnw`)

You never installed Maven — you don't have to. The Initializr included `mvnw` (macOS/Linux) and `mvnw.cmd` (Windows), small scripts that download the exact Maven version this project expects (3.9.16, pinned in `.mvn/wrapper/maven-wrapper.properties`) and run it.

**Always use `./mvnw`, never `mvn`.** It guarantees everyone on the project builds with the same Maven.

| Command | What it does |
| --- | --- |
| `./mvnw compile` | Compile `src/main/java` into `target/classes` |
| `./mvnw test` | Compile and run the tests |
| `./mvnw spring-boot:run` | Compile and start the app |
| `./mvnw clean` | Delete `target/` |
| `./mvnw clean package` | Full rebuild into a runnable JAR in `target/` |

On Windows, drop the `./` — write `mvnw spring-boot:run`.

## ✅ Checkpoint 1 — Run the empty app

```bash
./mvnw spring-boot:run
```

Expected output (abridged):

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
...
Tomcat started on port 8080 (http) with context path '/'
Started ItemExchangeApplication in 1.8 seconds
```

The app is running and will stay running. **Press `Ctrl + C` to stop it.**

If you see `Tomcat started on port 8080` — Part 1 is done. If you see `Port 8080 was already in use`, something else is on that port; the next section changes it anyway.

### Add your first endpoint (optional sanity check)

Create `src/main/java/com/siran/itemExchange/resource/HelloController.java`:

```java
package com.siran.itemExchange.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello from Spring Boot!";
    }
}
```

Restart, then visit **http://localhost:8080/hello** in a browser. You should see the greeting. It keeps working after Jersey is set up in Part 6, because Jersey is mounted under `/api` and leaves every other URL to Spring MVC ([6.5](#65-jersey-and-spring-mvc-in-the-same-app) explains).

---

# Part 2 — Understanding and finishing the `pom.xml`

`pom.xml` ("Project Object Model") is the one file that controls your build. Three things are worth adding to it by hand.

## 2.1 Anatomy of the file

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.7</version>
</parent>
```

**The parent POM.** This is where Spring Boot's version management lives. Because of it, you can write a dependency **without a `<version>` tag** and Maven will pick the version Spring Boot tested against. That's why most entries below have no version — and it's why you should *not* add versions to Spring dependencies yourself.

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

**Properties.** Named values reused elsewhere in the file. `java.version` tells the compiler which Java level to target.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    ...
</dependencies>
```

**Dependencies.** Every library your code needs. Each is identified by three coordinates: `groupId` (who made it), `artifactId` (which library), `version` (which release — omitted when the parent manages it).

Some dependencies carry a `<scope>`:

| Scope | Meaning |
| --- | --- |
| *(none)* | Available everywhere: compiling, testing, running. The default. |
| `runtime` | Not needed to compile, but needed to run. **The MySQL driver is `runtime`** — your code never mentions the driver class by name; Spring loads it reflectively at startup. |
| `test` | Only for test code. Never shipped in the final JAR. |
| `provided` | Needed to compile, but the deployment environment supplies it. Lombok ([2.4](#24-add-lombok)) uses it for a different reason: it's only needed while compiling. |

```xml
<build>
    <plugins> ... </plugins>
</build>
```

**Plugins** are code that runs *during the build* — as opposed to dependencies, which run *inside your app*. The Hibernate code generator in Part 5 is a plugin.

## 2.2 Add the Jakarta REST API dependency

The Jersey starter brings the *implementation*, but this project declares the **API** explicitly so that the `@Path` / `@GET` annotations resolve against a known version:

```xml
<dependency>
    <groupId>jakarta.ws.rs</groupId>
    <artifactId>jakarta.ws.rs-api</artifactId>
    <version>4.0.0</version>
</dependency>
```

<details>
<summary><b>What is "Jakarta" anyway?</b> (click to expand)</summary>

**Jakarta EE** is a set of *specifications* — interfaces and annotations with no implementation. You'll meet three of them in this project:

| Package | Spec | What it defines | Implemented here by |
| --- | --- | --- | --- |
| `jakarta.ws.rs.*` | Jakarta REST (JAX-RS) | `@Path`, `@GET`, `@POST`, `@PathParam` | **Jersey** |
| `jakarta.persistence.*` | Jakarta Persistence (JPA) | `@Entity`, `@Table`, `@Column`, `@Id` | **Hibernate** |
| `jakarta.annotation.*` | Common Annotations | `@PostConstruct`, `@Resource` | Spring |

You code against the standard annotations; the implementation is swappable.

**One historical note that will save you hours:** these packages used to be called `javax.*`. They were renamed to `jakarta.*` in 2019 when Java EE moved to the Eclipse Foundation. Modern Spring Boot uses `jakarta.*` **exclusively**. If a StackOverflow answer says `import javax.persistence.Entity`, it's written for an older stack — change it to `jakarta.persistence.Entity`. Mixing the two produces baffling errors where an annotation appears to be silently ignored.
</details>

## 2.3 Add the Hibernate Tools plugin

This is the plugin that will read your MySQL schema and write Java classes. Add it inside `<build><plugins>`:

```xml
<plugin>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-maven-plugin</artifactId>
    <version>7.4.5.Final</version>
    <configuration>
        <revengFile>hibernate-reverse-engineering.xml</revengFile>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>9.2.0</version>
        </dependency>
    </dependencies>
</plugin>
```

> 📌 **Note the nested `<dependencies>` block.** A Maven plugin runs in its own isolated classloader and cannot see your project's dependencies. Even though `mysql-connector-j` is already a project dependency, the plugin needs its *own* copy to connect to your database — and because the plugin isn't managed by the Spring Boot parent, you must state the version explicitly here.

## 2.4 Add Lombok

The DTO classes in Part 6 are mostly private fields plus their getters and setters. **Lombok** writes those methods for you at compile time, from an annotation on the class (`@Getter`, `@Setter`), so the source stays short. Add it inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.46</version>
    <scope>provided</scope>
</dependency>
```

The scope is `provided` because Lombok only matters while compiling: it generates code, and by the time the app runs, the generated methods are ordinary Java. Spring Boot's parent POM already manages Lombok's version (1.18.46 here too), so the `<version>` line is optional. You could also have ticked **Lombok** in the Initializr; this project added it by hand, later.

Your IDE has to understand Lombok as well, or it will flag every generated getter as missing. IntelliJ IDEA has Lombok support built in; if it asks to enable annotation processing, say yes. In VS Code, the Extension Pack for Java includes it.

> ⚠️ **On JDK 23 or newer**, `javac` no longer runs annotation processors just because they're on the classpath. Lombok then silently does nothing, and the build fails with "cannot find symbol" on every getter. The fix is to list Lombok under `<annotationProcessorPaths>` in the `maven-compiler-plugin` configuration. On JDK 17, as used here, nothing extra is needed.

## 2.5 The finished `pom.xml`

<details>
<summary><b>Click to see the complete file</b></summary>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.7</version>
        <relativePath/>
    </parent>

    <groupId>com.siran</groupId>
    <artifactId>itemExchange</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jersey</artifactId>
        </dependency>

        <dependency>
            <groupId>jakarta.ws.rs</groupId>
            <artifactId>jakarta.ws.rs-api</artifactId>
            <version>4.0.0</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.46</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <plugin>
                <groupId>org.hibernate.orm</groupId>
                <artifactId>hibernate-maven-plugin</artifactId>
                <version>7.4.5.Final</version>
                <configuration>
                    <revengFile>hibernate-reverse-engineering.xml</revengFile>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>com.mysql</groupId>
                        <artifactId>mysql-connector-j</artifactId>
                        <version>9.2.0</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

The file the Initializr generates also has a few empty elements, such as `<url/>`, `<licenses>`, `<developers>`, and `<scm>`. They stop Maven from inheriting Spring Boot's own values for those from the parent POM. They're left out above to keep it short; leave them in your file.
</details>

## ✅ Checkpoint 2 — Reload and compile

After editing `pom.xml`, your IDE must re-read it:

- **IntelliJ**: a small 🔄 icon appears in the top-right of the editor — click it. (Or right-click the project → **Maven → Reload Project**.)
- **VS Code**: `Ctrl`/`⌘` + `Shift` + `P` → "Java: Clean Java Language Server Workspace".

Then:

```bash
./mvnw clean compile
```

You want `BUILD SUCCESS`. If Maven complains it can't resolve something, check your internet connection and re-run — the first build downloads a lot.

---

# Part 3 — Set up the MySQL database

Spring Boot will not create your database for you. You build the schema in MySQL first, then point the app at it.

## 3.1 Start the MySQL server

**macOS (Homebrew):**

```bash
brew services start mysql
```

**Windows:** MySQL installs as a Windows service that starts automatically. To check: `Win + R` → `services.msc` → look for **MySQL84** → it should say "Running".

**Linux:**

```bash
sudo systemctl start mysql
```

## 3.2 Log in as root

```bash
mysql -u root -p
```

Type the root password you set during installation. On a fresh Homebrew install there is **no** root password yet — just press Enter, or use `mysql -u root`.

You should land at a `mysql>` prompt. Everything in the next sections is typed here.

> 💡 Prefer a GUI? [MySQL Workbench](https://dev.mysql.com/downloads/workbench/) (free, all platforms) or [DBeaver](https://dbeaver.io/) let you run the same SQL in a window with a results grid. Either is fine — the SQL is identical.

## 3.3 Create the database

In MySQL, a "database" (also called a schema) is a namespace holding tables.

```sql
CREATE DATABASE web_exchange
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

> **Always use `utf8mb4`.** MySQL's legacy `utf8` is a broken 3-byte encoding that cannot store emoji or many CJK characters. `utf8mb4` is real UTF-8.

Then switch to it:

```sql
USE web_exchange;
```

## 3.4 Create a dedicated application user

You *can* connect as `root`. **Don't.** If your app is ever compromised, `root` can drop every database on the server. A dedicated user with rights to exactly one database limits the blast radius.

```sql
CREATE USER 'exchange_app'@'localhost' IDENTIFIED BY 'ReplaceThisWithYourOwnPassword';
GRANT ALL PRIVILEGES ON web_exchange.* TO 'exchange_app'@'localhost';
FLUSH PRIVILEGES;
```

| Piece | Meaning |
| --- | --- |
| `'exchange_app'@'localhost'` | In MySQL a user is a **pair**: name + where they may connect from. `@'localhost'` means "only from this machine". |
| `IDENTIFIED BY '...'` | The password. **Pick your own** — don't paste the placeholder. |
| `ON web_exchange.*` | All tables in that one database, and nothing else. |
| `FLUSH PRIVILEGES` | Reload the grant tables so the change takes effect immediately. |

## 3.5 Create the tables

This is the schema for the `itemExchange` project — six tables: users post listings in categories, and then message each other and arrange meetups. It also adds a starter set of categories, since nothing in the API creates them.

```sql
USE web_exchange;

-- People using the marketplace
CREATE TABLE users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    phone       VARCHAR(20),
    college     VARCHAR(100),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Lookup table: "Textbooks", "Furniture", "Electronics", ...
CREATE TABLE categories (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO categories (name) VALUES
    ('Textbooks'), ('Electronics'), ('Furniture'), ('Clothing'),
    ('Sports'), ('Kitchen'), ('Bikes'), ('Other');

-- An item offered for sale, for exchange, or open to both
CREATE TABLE listings (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    user_id          INT NOT NULL,
    category_id      INT,
    title            VARCHAR(150) NOT NULL,
    description      TEXT,
    listing_type     ENUM('sell','exchange','both') NOT NULL DEFAULT 'sell',
    price            DECIMAL(10,2),
    condition_grade  ENUM('new','like_new','good','fair','poor') NOT NULL DEFAULT 'good',
    status           ENUM('available','pending','sold','exchanged') NOT NULL DEFAULT 'available',
    image_url        VARCHAR(500),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_listings_user
        FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_listings_category
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- Which payment methods a listing accepts (many rows per listing)
CREATE TABLE listing_payment_methods (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    listing_id  INT NOT NULL,
    method      ENUM('paypal','venmo','zelle','cash') NOT NULL,
    CONSTRAINT uq_listing_method UNIQUE (listing_id, method),
    CONSTRAINT fk_lpm_listing
        FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

-- Messages between a buyer and a seller about one listing
CREATE TABLE messages (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    listing_id   INT NOT NULL,
    sender_id    INT NOT NULL,
    receiver_id  INT NOT NULL,
    content      TEXT NOT NULL,
    is_read      BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_listing
        FOREIGN KEY (listing_id)  REFERENCES listings(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender
        FOREIGN KEY (sender_id)   REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_messages_receiver
        FOREIGN KEY (receiver_id) REFERENCES users(id)    ON DELETE CASCADE
);

-- An agreed time and place to hand the item over
CREATE TABLE meetups (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    listing_id     INT NOT NULL,
    buyer_id       INT NOT NULL,
    seller_id      INT NOT NULL,
    location       VARCHAR(255) NOT NULL,
    proposed_time  DATETIME,
    status         ENUM('proposed','confirmed','cancelled','completed') NOT NULL DEFAULT 'proposed',
    notes          TEXT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_meetups_listing
        FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    CONSTRAINT fk_meetups_buyer
        FOREIGN KEY (buyer_id)   REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_meetups_seller
        FOREIGN KEY (seller_id)  REFERENCES users(id)    ON DELETE CASCADE
);
```

> 📌 **The same schema also lives in the frontend repository**, as web-exchange's [`database/schema.sql`](https://github.com/sirangao/web-exchange/blob/master/database/schema.sql). That's the copy the README tells people to run. The two differ only cosmetically: that file orders some columns differently, doesn't name its constraints, and adds a few indexes for browsing. If you change the schema, change both.

### Schema decisions that matter for Part 5

Hibernate reads this schema literally, so the choices you make here become the shape of your Java code. Four in particular:

| SQL you write | What Hibernate generates | Why you care |
| --- | --- | --- |
| `FOREIGN KEY (user_id) REFERENCES users(id)` | A `Users users;` field on `Listings` **and** a `Set<Listings>` field on `Users` | **Declare your foreign keys.** Without them Hibernate sees six unrelated tables and generates six unrelated classes with no navigation between them. |
| `NOT NULL`, `VARCHAR(150)`, `UNIQUE` | `@Column(nullable=false, length=150, unique=true)` | Constraints are copied into the annotations. |
| `AUTO_INCREMENT` | `@GeneratedValue(strategy=IDENTITY)` | Without `AUTO_INCREMENT`, the generated class has **no** `@GeneratedValue` and you must set the ID yourself before saving. |
| `ENUM('sell','exchange','both')` | `String` with `length=8` | Hibernate maps MySQL `ENUM` to a plain `String` sized to the longest value. You get no compile-time safety — see [5.7](#57-known-quirks-of-generated-code). |

Two tables here have **two foreign keys to the same table** (`messages` → `users` twice, `meetups` → `users` twice). Watch what Hibernate names those fields in Part 5 — it's the single most confusing thing in the generated output.

## ✅ Checkpoint 3 — Verify the schema

```sql
SHOW TABLES;
```

```
+------------------------------+
| Tables_in_web_exchange       |
+------------------------------+
| categories                   |
| listing_payment_methods      |
| listings                     |
| meetups                      |
| messages                     |
| users                        |
+------------------------------+
```

Confirm the foreign keys actually exist — this is the step people skip, and it's what makes Part 5 work:

```sql
SHOW CREATE TABLE listings\G
```

You should see `CONSTRAINT fk_listings_user FOREIGN KEY (user_id) REFERENCES users (id)` in the output. (If you built the tables from web-exchange's `schema.sql` instead, which doesn't name its constraints, MySQL calls it `listings_ibfk_1`. It's the same key.) If your FKs are missing, the most common cause is a storage-engine mismatch (`MyISAM` silently ignores foreign keys — you want `InnoDB`, which is the default in MySQL 8).

Finally, confirm your **app user** can log in. Exit with `exit` and reconnect as the new user:

```bash
mysql -u exchange_app -p web_exchange
```

If that prompt appears, your credentials work. Type `exit`.

---

# Part 4 — Connect Spring Boot to MySQL

All configuration lives in **`src/main/resources/application.properties`**. Each line is `key=value`; `#` starts a comment.

## 4.1 Keep your password out of Git — the `.env` pattern

Before writing the connection settings: **never hardcode a database password in a file you commit.** Public GitHub repos are scraped for leaked credentials within minutes.

The pattern used in this project is to keep secrets in a separate, un-committed `.env` file.

**Step 1** — create `.env` in the **project root** (next to `pom.xml`, *not* in `src/`):

```properties
MYSQL_USER=exchange_app
MYSQL_PASSWORD=ReplaceThisWithYourOwnPassword
```

**Step 2** — tell Git to ignore it. Add to `.gitignore`:

```gitignore
.env
```

**Step 3** — tell Spring Boot to load it, in `application.properties`:

```properties
spring.config.import=optional:file:.env[.properties]
```

Decoding that value:

| Fragment | Meaning |
| --- | --- |
| `optional:` | Don't crash if the file is missing. Without this, anyone who clones your repo gets a startup failure. |
| `file:.env` | Read the file `.env`, relative to the working directory. |
| `[.properties]` | A **format hint**. Spring normally infers format from the extension, and `.env` has none — this says "parse it as a properties file". |

**Step 4** — reference the values with `${...}` placeholders (next section).

> ⚠️ **Working-directory gotcha.** `file:.env` resolves relative to wherever the process starts. `./mvnw spring-boot:run` from the project root works. If your IDE's run configuration has a different working directory, Spring silently skips the file (it's `optional:`) and you get a confusing "Access denied for user '${MYSQL_USER}'" error. In IntelliJ: **Run → Edit Configurations → Working directory** — set it to the project root.
>
> **Also commit a `.env.example`** with the keys and dummy values, so a teammate cloning the repo knows what to create.

## 4.2 The connection settings

Add these to `application.properties`:

```properties
spring.application.name=itemExchange
server.port=9090

spring.config.import=optional:file:.env[.properties]

# MySQL Connection Settings
spring.datasource.url=jdbc:mysql://localhost:3306/web_exchange
spring.datasource.username=${MYSQL_USER}
spring.datasource.password=${MYSQL_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate Configuration
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
```

(The project's real file has one more line, `app.cors.allowed-origin`, which [Part 8](#part-8--the-web-frontend) adds.)

Line by line:

| Property | Purpose |
| --- | --- |
| `spring.application.name` | Cosmetic; shows up in logs and monitoring. |
| `server.port=9090` | Serve HTTP on 9090 instead of the default 8080. Useful when something else already owns 8080. |
| `spring.datasource.url` | The **JDBC URL** — dissected below. |
| `spring.datasource.username` / `.password` | Pulled from `.env` via `${}`. |
| `spring.datasource.driver-class-name` | Which driver class to load. Spring can infer this from the URL, so it's optional — but being explicit makes failures easier to read. |
| `spring.jpa.properties.hibernate.dialect` | Which SQL flavour Hibernate should emit. Also auto-detected; stated explicitly here. Any property under `spring.jpa.properties.*` is passed straight through to Hibernate. |
| `spring.jpa.hibernate.ddl-auto` | **What Hibernate does to your schema at startup** — see the table below. Read it before you run anything. |
| `spring.jpa.show-sql=true` | Print every SQL statement Hibernate runs. Invaluable while learning; turn it off in production. |

### Reading a JDBC URL

```
jdbc:mysql://localhost:3306/web_exchange
└──┬──┘ └─┬─┘   └───┬───┘ └┬─┘ └────┬─────┘
 always  which     host   port   database
        database
```

You can append connection options after a `?`, e.g. `?useSSL=false&serverTimezone=UTC`. You shouldn't need any for a local MySQL 8 setup.

### `ddl-auto` — the setting that can delete your data

This tells Hibernate what to do with your **schema** every time the app starts.

| Value | Behaviour | Use when |
| --- | --- | --- |
| **`validate`** | Compares your entity classes to the actual tables and **fails to start** if they disagree. Changes nothing. | ✅ **What this project uses.** You own the schema; Hibernate's job is to catch mismatches. |
| `none` | Do nothing at all. | You manage schema with a migration tool like Flyway. |
| `update` | Tries to alter tables to match your entities. | Convenient for prototyping, but it never drops or narrows columns, so schemas drift into a mess. Avoid past day one. |
| `create` | **Drops every table and recreates them.** | Throwaway experiments only. |
| `create-drop` | Same, plus drops everything again on shutdown. | Integration tests against a temp database. |

> 🚨 **`create` and `create-drop` destroy all data in the database, every single start.** Never point them at a database you care about. The pairing of `validate` here with the reverse-engineering workflow in Part 5 is deliberate: **the database is the source of truth, and Hibernate's role is to verify — never to modify.**

## ✅ Checkpoint 4 — Confirm the connection

```bash
./mvnw spring-boot:run
```

Look for these lines:

```
HikariPool-1 - Starting...
HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@...
HikariPool-1 - Start completed.
Tomcat started on port 9090 (http) with context path '/'
Started ItemExchangeApplication in 2.4 seconds
```

**`HikariPool-1 - Start completed`** is the line that proves it worked. (HikariCP is the connection pool Spring Boot uses — it opens a handful of connections up front and reuses them, because opening a TCP connection per request is slow.)

### If it failed

| Error message | Cause | Fix |
| --- | --- | --- |
| `Access denied for user 'x'@'localhost'` | Wrong username/password | Verify by logging in manually: `mysql -u exchange_app -p` |
| `Access denied for user '${MYSQL_USER}'@...` | The `.env` file wasn't found — the placeholder was never substituted | Check the file name/location and your run configuration's working directory ([4.1](#41-keep-your-password-out-of-git--the-env-pattern)) |
| `Communications link failure` | MySQL isn't running, or wrong host/port | `brew services list` / check the Windows service |
| `Unknown database 'web_exchange'` | Database not created, or a typo | `SHOW DATABASES;` in the MySQL client |
| `Failed to determine a suitable driver class` | The MySQL driver dependency is missing | Check `mysql-connector-j` is in `pom.xml`, then reload Maven |
| `Schema-validation: missing table [xyz]` | `ddl-auto=validate` is doing its job — an entity doesn't match the DB | Expected right now if you have entity classes but no tables. Resolved in Part 5. |

---

# Part 5 — Generate your entity classes with Hibernate

## 5.1 The concept: what an ORM does

You have six tables. To use them from Java, you need six classes whose fields line up with the columns. That mapping — table ↔ class, row ↔ object, column ↔ field — is **Object-Relational Mapping (ORM)**, and **Hibernate** is the ORM doing it here.

Once a class is mapped, you write this:

```java
Users user = usersRepository.findById(5).orElseThrow();
System.out.println(user.getEmail());
```

instead of this:

```java
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
ps.setInt(1, 5);
ResultSet rs = ps.executeQuery();
if (rs.next()) System.out.println(rs.getString("email"));
```

A mapped class is called an **entity**. It's an ordinary Java class carrying annotations that say which table and columns it corresponds to.

## 5.2 Two directions, and why we go backwards

| Direction | Meaning |
| --- | --- |
| **Forward engineering** | Write Java entities → Hibernate creates the tables (`ddl-auto=create`/`update`). |
| **Reverse engineering** | Write SQL tables → a tool generates the Java entities. ⬅️ **What this project does.** |

Reverse engineering is the right choice when the database already exists, when you want precise control over indexes and column types, or when you already know SQL better than JPA. Hand-writing six entity classes with all their relationships is also tedious and easy to get subtly wrong.

The tool is **Hibernate Tools**, and the specific generator is **`hbm2java`** ("HBM to Java"). It connects to your live database, reads the metadata catalog, and writes `.java` files.

> ⚠️ **This is a one-time (or occasional) code generation step, not a live sync.** After generating, the files are *yours* — they don't update themselves when you alter a table. See [5.8](#58-when-your-schema-changes-later) for the re-run workflow.

## 5.3 Configure the database connection for the plugin

The plugin runs as part of the **build**, outside your Spring application, so it cannot read `application.properties`. It needs its own connection file.

Create **`src/main/resources/hibernate.properties`**:

```properties
hibernate.connection.driver_class=com.mysql.cj.jdbc.Driver
hibernate.connection.url=jdbc:mysql://localhost:3306/web_exchange
hibernate.connection.username=exchange_app
hibernate.connection.password=ReplaceThisWithYourOwnPassword
hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

> 🔴 **This file contains a plaintext password. Add it to `.gitignore` right now:**
>
> ```gitignore
> src/main/resources/hibernate.properties
> ```
>
> `src/main/resources/hibernate.properties` is the plugin's **default** location — that's why the `pom.xml` doesn't mention it. To keep it out of the packaged app entirely, put it somewhere else (say `tools/hibernate.properties`) and point the plugin at it with `<propertyFile>${project.basedir}/tools/hibernate.properties</propertyFile>`.

Note the key names are **not** the same as the Spring ones — `hibernate.connection.url`, not `spring.datasource.url`. Two different systems, two different vocabularies, same database.

## 5.4 Configure *what* to generate

Create **`hibernate-reverse-engineering.xml`** in the **project root** (this is the file the `<revengFile>` tag in `pom.xml` points to):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE hibernate-reverse-engineering SYSTEM
        "https://hibernate.org/dtd/hibernate-reverse-engineering-3.0.dtd">
<hibernate-reverse-engineering>
    <table-filter match-name=".*" package="com.siran.itemExchange"/>
</hibernate-reverse-engineering>
```

| Attribute | Meaning |
| --- | --- |
| `match-name=".*"` | A **regular expression** matching table names. `.*` means "every table". |
| `package="..."` | The Java package the generated classes go into. |

Useful variations:

```xml
<!-- Skip a table entirely -->
<table-filter match-name="flyway_schema_history" exclude="true"/>

<!-- Only generate for tables starting with "user" -->
<table-filter match-name="user.*" package="com.siran.itemExchange"/>
```

Order matters: put your `exclude="true"` filters **before** the catch-all `.*` line.

## 5.5 Run the generator

Make sure **MySQL is running** (the plugin connects to it), then:

```bash
./mvnw hibernate:hbm2java
```

Reading that command: `hibernate` is the plugin's short prefix, `hbm2java` is the goal. Other goals the same plugin offers:

| Goal | Output |
| --- | --- |
| `hibernate:hbm2java` | **Java entity classes** ⬅️ the one you want |
| `hibernate:hbm2ddl` | A `.sql` script of `CREATE TABLE` statements |
| `hibernate:generateHbm` | Old-style XML mapping files (superseded by annotations) |
| `hibernate:hbm2dao` | Skeleton DAO classes (Spring Data repositories are better) |

Expected output:

```
[INFO] --- hibernate:7.4.5.Final:hbm2java (default-cli) @ itemExchange ---
[INFO] BUILD SUCCESS
```

### Where the files landed

By default the plugin writes to **`target/generated-sources/`**:

```
target/generated-sources/com/siran/itemExchange/
├── Categories.java
├── ListingPaymentMethods.java
├── Listings.java
├── Meetups.java
├── Messages.java
└── Users.java
```

**`target/` is deleted by `./mvnw clean`, so you must move these into your real source tree:**

1. Create the package `com.siran.itemExchange.dataObjects` under `src/main/java/`.
   *(Any name works — `model`, `entity`, `domain` are common conventions. This project uses `dataObjects`.)*
2. Copy the six `.java` files into it.
3. **Fix the `package` line at the top of each file** to match where you put them:

   ```java
   package com.siran.itemExchange.dataObjects;
   ```

   Your IDE can do this in one step: select the files in `target/`, drag them into the new package, and IntelliJ rewrites the package declarations for you.

> 💡 **Skip the copying:** set `<packageName>com.siran.itemExchange.dataObjects</packageName>` and `<outputDirectory>${project.basedir}/src/main/java</outputDirectory>` in the plugin's `<configuration>` and the files are written straight to the right place. Only do this once you're comfortable — writing directly into `src/` means a re-run **silently overwrites** your files. See [5.8](#58-when-your-schema-changes-later).

## 5.6 Reading the generated code

Here's the top of the generated `Users.java`, annotated:

```java
package com.siran.itemExchange.dataObjects;
// Generated Aug 17, 2026 by Hibernate Tools 7.4.5.Final

import jakarta.persistence.*;
import static jakarta.persistence.GenerationType.IDENTITY;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity                                          // ① this class maps to a table
@Table(name = "users",                           // ② which table
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "username")
    })
public class Users implements java.io.Serializable {

    private Integer id;
    private String username;
    private String email;
    private Date createdAt;
    private Set<Listings> listingses = new HashSet<>(0);   // ⑥
    // ... plus phone, college, password, updatedAt, and more Sets

    public Users() { }                           // ③ no-arg constructor: required

    public Users(String username, String password, String email) { ... }

    @Id                                          // ④ primary key
    @GeneratedValue(strategy = IDENTITY)         // ⑤ the DB assigns it
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() { return this.id; }
    public void setId(Integer id) { this.id = id; }

    @Column(name = "username", unique = true, nullable = false, length = 50)
    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; }

    @Temporal(TemporalType.TIMESTAMP)            // ⑦
    @Column(name = "created_at", length = 19)
    public Date getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "users")   // ⑥
    public Set<Listings> getListingses() { return this.listingses; }
    public void setListingses(Set<Listings> l) { this.listingses = l; }
}
```

| # | Annotation | What it means |
| --- | --- | --- |
| ① | `@Entity` | Marks the class as persistent. Hibernate scans for these at startup. |
| ② | `@Table(name="users")` | The table it maps to. Needed because the class is `Users` and the table is `users` (and Java class names don't have to match table names at all). |
| ③ | *(no-arg constructor)* | JPA creates objects reflectively and needs a constructor with no arguments. Never delete it. |
| ④ | `@Id` | The primary key field. Every entity must have exactly one (or a composite key). |
| ⑤ | `@GeneratedValue(strategy=IDENTITY)` | The database generates the value — MySQL `AUTO_INCREMENT`. Leave `id` null when saving a new row; Hibernate fills it in after the `INSERT`. |
| ⑥ | `@OneToMany` / `@ManyToOne` | A foreign-key relationship, in Java form. `mappedBy="users"` says *the other side owns this relationship* — the `listings.user_id` column lives on the `Listings` entity's `users` field. `FetchType.LAZY` means the collection isn't loaded from the DB until you actually call the getter. |
| ⑦ | `@Temporal(TemporalType.TIMESTAMP)` | For the legacy `java.util.Date` type, says whether to store date, time, or both. |

### Both sides of a relationship

The foreign key `listings.user_id → users.id` produces **two** mapped fields:

```java
// In Listings.java — the "owning" side; this is where the actual column lives
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
public Users getUsers() { return this.users; }
```

```java
// In Users.java — the "inverse" side; a convenience view of the same FK
@OneToMany(fetch = FetchType.LAZY, mappedBy = "users")
public Set<Listings> getListingses() { return this.listingses; }
```

Which means you can navigate in either direction:

```java
listing.getUsers().getEmail();       // who posted this?
user.getListingses().size();         // how many things has this person posted?
```

### Property access vs. field access

Notice the annotations sit on the **getters**, not the fields. That's not decoration — JPA infers its access strategy from where you put `@Id`. Because `@Id` is on `getId()`, Hibernate uses **property access**: it calls your getters and setters rather than touching fields directly.

**Practical consequence: if you add a new persistent field by hand, annotate its getter, not the field.** Mixing the two styles in one class causes fields to be silently ignored.

## 5.7 Known quirks of generated code

Generated code is correct but not beautiful. Expect these:

| Quirk | Example | What to do |
| --- | --- | --- |
| **Doubled-up plurals** | `Set<Listings> listingses`, `messageses`, `meetupses` | The tool pluralizes an already-plural table name. Harmless. Rename with your IDE's refactor tool if it bothers you — but see [5.8](#58-when-your-schema-changes-later) first. |
| **Plural class names** | `Users` for a single user | It names the class after the table. `Users user = ...` reads oddly forever. |
| **Two FKs to one table** | `usersBySenderId` / `usersByReceiverId` on `Messages` | Actually helpful — this is the tool disambiguating. Same on `Meetups`: `usersByBuyerId` / `usersBySellerId`, and on `Users` the reverse sides are `messagesesForSenderId`, `meetupsesForBuyerId`, etc. |
| **`ENUM` becomes `String`** | `listing_type ENUM(...)` → `@Column(length=8) String listingType` | No compile-time safety; `setListingType("selll")` compiles and fails at runtime. Convert to a real Java `enum` with `@Enumerated(EnumType.STRING)` once you're comfortable. This project keeps the `String` and checks the value on the way in instead, with `@Pattern` on `ListingInput` ([Part 8](#part-8--the-web-frontend)). |
| **Missing `@GeneratedValue`** | `ListingPaymentMethods.id` has `@Id` but no `@GeneratedValue` | Means that column wasn't `AUTO_INCREMENT` when the class was generated. Either add `AUTO_INCREMENT` to the column and regenerate, or set the ID yourself before saving. |
| **`java.util.Date`** | `private Date createdAt;` | The legacy date type. `LocalDateTime` is better; you can change the type and drop `@Temporal`. |
| **No `equals`/`hashCode`/`toString`** | — | Fine to start. Add them (based on the business key, not the ID) when you put entities in `Set`s that matter. |

## 5.8 When your schema changes later

You add a column in MySQL. Now what?

**Option A — regenerate everything (early days, no hand edits yet).**

```bash
./mvnw hibernate:hbm2java     # writes fresh files into target/generated-sources/
```

Then copy over your `dataObjects` package and re-fix the `package` lines.

> 🔴 **This overwrites your files.** Any method you hand-wrote, any field you renamed, any type you improved — gone. Commit to Git before regenerating so you can diff and recover.

**Option B — edit the entity by hand (once the classes have real work in them).**

Add the field and annotate its getter:

```java
private String dormBuilding;

@Column(name = "dorm_building", length = 100)
public String getDormBuilding() { return this.dormBuilding; }
public void setDormBuilding(String dormBuilding) { this.dormBuilding = dormBuilding; }
```

In practice you generate **once** at the start, then hand-edit. Reverse engineering is a bootstrapping tool, not a permanent pipeline.

Whichever option you take, `spring.jpa.hibernate.ddl-auto=validate` is your safety net: if the class and the table disagree, the app refuses to start and tells you exactly which column is wrong. That's a startup error instead of a bug in production.

## ✅ Checkpoint 5 — Validate entities against the database

```bash
./mvnw clean compile
./mvnw spring-boot:run
```

With `ddl-auto=validate`, a clean startup **is** the test: Hibernate has compared all six classes against all six tables and found them consistent.

A failure looks like this, and is genuinely helpful:

```
Schema-validation: missing column [dorm_building] in table [users]
```

| Validation error | Meaning |
| --- | --- |
| `missing table [x]` | An `@Entity` exists for a table that isn't in the DB. Check spelling and that you're pointed at the right database. |
| `missing column [x] in table [y]` | The entity has a field the table doesn't. You changed one side only. |
| `wrong column type ... found [varchar], but expecting [integer]` | Type mismatch between class and column. |

---

# Part 6 — Expose the data over HTTP

Entities are mapped. Now serve them.

## 6.1 A repository — database access with no implementation

Create `src/main/java/com/siran/itemExchange/dataRepositories/UsersRepository.java`:

```java
package com.siran.itemExchange.dataRepositories;

import com.siran.itemExchange.dataObjects.Users;
import org.springframework.data.repository.CrudRepository;

// Spring generates the implementation at startup — you never write one.
public interface UsersRepository extends CrudRepository<Users, Integer> {
}
```

That's the whole file. It's an **interface with no methods**, and at startup Spring Data JPA generates a class implementing it and registers it as a bean.

`CrudRepository<Users, Integer>` reads as: *"a repository of `Users` entities whose primary key is an `Integer`"* — that second type parameter must match the type of your `@Id` field.

You get these for free:

| Method | SQL it runs |
| --- | --- |
| `save(user)` | `INSERT` (or `UPDATE` if the ID is already set) |
| `findById(5)` | `SELECT * FROM users WHERE id = 5` — returns `Optional<Users>` |
| `findAll()` | `SELECT * FROM users` |
| `count()` | `SELECT count(*) FROM users` |
| `deleteById(5)` | `DELETE FROM users WHERE id = 5` |
| `existsById(5)` | `SELECT ... LIMIT 1` |

And you can add queries just by naming a method — Spring parses the name and writes the SQL. These are the ones this project's `UsersRepository` declares, and the resource in 6.3 uses them:

```java
public interface UsersRepository extends CrudRepository<Users, Integer> {
    Optional<Users> findByUsername(String username);           // WHERE username = ?
    boolean existsByUsername(String username);                 // is this username taken?
    boolean existsByEmail(String email);                       // is this email taken?
    boolean existsByEmailAndIdNot(String email, Integer id);   // ...by anyone other than user `id`?
}
```

Still no implementation. This is the payoff for having mapped entities. The same naming scheme goes further than this: `findByCollegeOrderByCreatedAtDesc(String college)` would filter and sort, for example.

When a query is too involved to spell as a method name, write it yourself with `@Query`. `ListingsRepository`, which extends `JpaRepository` (a `CrudRepository` with some JPA-specific extras), does that to load each listing together with its seller and category in one query; [Part 8](#part-8--the-web-frontend) explains why that matters.

## 6.2 Tell Jersey where your endpoints are

Create `src/main/java/com/siran/itemExchange/config/JerseyConfig.java`:

```java
package com.siran.itemExchange.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        packages("com.siran.itemExchange.resource");
    }
}
```

`packages(...)` tells Jersey to scan that package for `@Path`-annotated classes and register each one. The alternative is listing them explicitly — `register(UserResource.class);` — which is more typing but makes registration obvious.

`@ApplicationPath("/api")` mounts every Jersey resource under `/api`, so a class annotated `@Path("/users")` answers at `/api/users`. Two reasons to do this rather than serve from the root: it keeps one namespace for the API, separate from anything Spring MVC serves; and it gives a frontend dev server a single prefix to proxy. Without the annotation Jersey sits at `/`, and the URLs below lose their `/api` segment.

`JerseyConfig` grows twice more: [6.7](#67-return-errors-as-json) registers the error mappers, and [Part 8](#part-8--the-web-frontend) adds a CORS filter.

> ⚠️ **A trap worth knowing about.** Some IDEs, when you type `packages(`, offer an auto-import of `org.springframework.core.annotation.AnnotationFilter.packages` — an unrelated static method with a similar signature. Java's scoping rules mean the inherited `ResourceConfig.packages()` still wins, so this compiles and works, but the stray `import static` is confusing. If your editor added one, delete it.

## 6.3 Write a resource class

"Resource class" is the Jakarta REST term for what Spring MVC calls a controller: the class whose methods answer HTTP requests.

Before writing one, decide what goes over the wire. It's tempting to hand back the `Users` entity itself, but entities make poor JSON: `Users` has lazy collections that throw once the database session has closed, relationships that can loop forever, and a `password` that must never leave the server ([6.6](#66-two-gotchas-when-returning-entities-as-json) has the details). So every endpoint in this project uses **DTOs** (data transfer objects): small classes shaped exactly like the JSON, one for what the client sends and one for what it gets back. They live in a `dto` package.

### The request body

Create `src/main/java/com/siran/itemExchange/dto/UserInput.java`:

```java
package com.siran.itemExchange.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter                                 // Lombok writes getUsername(), getPassword(), ...
@Setter                                 // ...and the setters Jackson fills the object through
public class UserInput {

    @NotBlank @Size(max = 50)           // mirrors username VARCHAR(50) NOT NULL
    private String username;

    @NotBlank @Size(max = 255)
    private String password;

    @NotBlank @Email @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String college;
}
```

The rules on the fields come from **Jakarta Bean Validation**, which the Jersey starter already includes. They repeat the limits of the `users` columns, so a request that would break a column rule is turned away with a readable message before it gets anywhere near MySQL. They don't run by themselves: the resource asks for them with `@Valid`, below.

### The response body

Create `src/main/java/com/siran/itemExchange/dto/UserResponse.java`:

```java
package com.siran.itemExchange.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.siran.itemExchange.dataObjects.Users;
import lombok.Getter;

import java.util.Date;

@Getter
public class UserResponse {

    private Integer id;
    private String username;
    @JsonIgnore                         // carried along, but never written to the JSON
    private String password;
    private String email;
    private String phone;
    private String college;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date updatedAt;

    public UserResponse(Integer id, String username, String password, String email, String phone,
                        String college, Date createdAt, Date updatedAt) {
        this.id = id;
        this.username = username;
        // ...one assignment per field
    }

    public static UserResponse from(Users u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getPassword(), u.getEmail(),
                u.getPhone(), u.getCollege(), u.getCreatedAt(), u.getUpdatedAt());
    }
}
```

`from(...)` is the one place a `Users` entity turns into JSON-ready data. It reads plain columns only, never the lazy collections, so it's safe to call after the session has closed. `@JsonFormat` writes the timestamps as ISO-8601 strings in UTC, like `2026-09-18T20:15:00Z`.

One more DTO: every failure in this project answers with the same small object, `{"error": "..."}`, so the frontend always knows where to find the message. Create `src/main/java/com/siran/itemExchange/dto/ApiError.java`:

```java
package com.siran.itemExchange.dto;

import lombok.Getter;

@Getter
public class ApiError {

    private final String error;

    public ApiError(String error) {
        this.error = error;
    }
}
```

### The resource

Create `src/main/java/com/siran/itemExchange/resource/UserResource.java`. The project's version also has sign-in and two kinds of update, which [Part 8](#part-8--the-web-frontend) covers; these are the core three:

```java
package com.siran.itemExchange.resource;

import com.siran.itemExchange.dataObjects.Users;
import com.siran.itemExchange.dataRepositories.UsersRepository;
import com.siran.itemExchange.dto.ApiError;
import com.siran.itemExchange.dto.UserInput;
import com.siran.itemExchange.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Optional;

@Path("/users")                                   // base path for everything here
public class UserResource {

    @Autowired                                     // Spring injects the repository
    private UsersRepository usersRepository;

    @GET
    @Path("/id/{id}")                              // → GET /api/users/id/5
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Integer id) {
        Optional<Users> maybeUser = usersRepository.findById(id);
        if (maybeUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(UserResponse.from(maybeUser.get())).build();
    }

    @GET
    @Path("/{username}")                           // → GET /api/users/alice
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByUsername(@PathParam("username") String username) {
        Optional<Users> maybeUser = usersRepository.findByUsername(username);
        if (maybeUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(UserResponse.from(maybeUser.get())).build();
    }

    @POST
    @Path("/add")                                  // → POST /api/users/add, JSON body
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addNewUserJson(@NotNull @Valid UserInput userInput) {
        // Checked up front so the client gets a message it can show, rather than a
        // unique-constraint failure from MySQL.
        if (usersRepository.existsByUsername(userInput.getUsername().trim())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ApiError("That username is already taken"))
                    .build();
        }
        if (usersRepository.existsByEmail(userInput.getEmail().trim().toLowerCase())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ApiError("That email is already registered"))
                    .build();
        }

        Users user = new Users();
        apply(userInput, user);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        usersRepository.save(user);
        return Response.status(Response.Status.CREATED).entity(UserResponse.from(user)).build();
    }

    // Copies a request onto an entity. The update endpoint reuses it.
    private static void apply(UserInput in, Users user) {
        user.setUsername(in.getUsername().trim());
        user.setPassword(in.getPassword());
        user.setEmail(in.getEmail().trim().toLowerCase());
        user.setPhone(in.getPhone());
        user.setCollege(in.getCollege());
    }
}
```

`/id/{id}` and `/{username}` don't collide: a `{variable}` matches exactly one path segment, so `/users/id/5` can only be the first and `/users/alice` only the second.

| Annotation | Meaning |
| --- | --- |
| `@Path("/users")` on the class | Base URL segment for every method in the class. Jersey's `/api` from 6.2 goes in front, so these answer at `/api/users/...`. |
| `@Path("/id/{id}")` on a method | Appended to the base → `/users/id/{id}`. Braces mark a **path variable**. |
| `@GET`, `@POST`, `@PUT`, `@DELETE` | Which HTTP method this handles. |
| `@PathParam("id")` | Bind the `{id}` segment to this parameter. **The name in the annotation must match the name in the braces.** |
| `@QueryParam("type")` | Bind `?type=...` from the query string. Not needed here; `ListingResource` uses it for its browse filters. |
| `@Produces(MediaType.APPLICATION_JSON)` | Response `Content-Type`. Jackson serializes the returned object to JSON. |
| `@Consumes(MediaType.APPLICATION_JSON)` | The request body is JSON. Jackson turns it into the method's one un-annotated parameter, here a `UserInput`. |
| `@Valid` | Check that parameter's Bean Validation rules before the method runs. If one fails, the method is never called and the client gets a `400` ([6.7](#67-return-errors-as-json) shapes the message). |
| `@NotNull` | Also reject a request with no body at all, which would otherwise arrive as `null`. |

The duplicate checks run before `save()` on purpose. MySQL's `UNIQUE` constraints would stop a second `alice` anyway, but as a database error the client can't show anyone; checking first turns it into a `409` with a message.

> ⚠️ **Match your types to your entity.** `Users.id` is an `Integer`, so the path parameter is `Integer id`, the same type as the second parameter of `CrudRepository<Users, Integer>`.
>
> ⚠️ **The password is stored as typed.** Taking it in a JSON body keeps it out of URLs and server logs, where a `?password=` query string would end up. But `apply()` still saves it as plain text. Real sign-up endpoints store a **hash** (BCrypt) instead; see the warning in [Part 8](#part-8--the-web-frontend).

`ListingResource` and `CategoryResource` are built the same way: DTOs in and out, and `404` for a missing record. README.md's [API reference](README.md#api-reference) lists every endpoint.

## 6.4 Test it

Start the app and, in a **second terminal**, create a user:

```bash
curl -i -X POST http://localhost:9090/api/users/add \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123", "email": "alice@example.com"}'
```

The `-i` flag prints the response headers, so you can see the status code: `HTTP/1.1 201`. The body is the new user, with its generated `id` and without the password:

```json
{"id":1,"username":"alice","email":"alice@example.com","phone":null,"college":null,"createdAt":"2026-09-18T20:15:00Z","updatedAt":"2026-09-18T20:15:00Z"}
```

Read it back by id, then by username:

```bash
curl -i http://localhost:9090/api/users/id/1
```

```bash
curl -i http://localhost:9090/api/users/alice
```

Then try the failure cases. Send the same `POST` again and you get `409` with `{"error":"That username is already taken"}`. Change the email to `not-an-email` and validation answers `400` with `{"error":"email: must be a well-formed email address"}`.

Confirm the row actually landed:

```sql
SELECT id, username, email FROM web_exchange.users;
```

Because `spring.jpa.show-sql=true` is on, you'll also see Hibernate's SQL in the app's console:

```
Hibernate: insert into users (college,created_at,email,password,phone,updated_at,username) values (?,?,?,?,?,?,?)
```

Reading that log is the fastest way to understand what your JPA code is really doing.

## 6.5 Jersey and Spring MVC in the same app

By default Spring Boot registers Jersey as a **servlet mapped to `/*`** — it claims every URL. Without a prefix, the `/hello` `@RestController` from Part 1 would stop responding once Jersey is configured, because Jersey answers first and has no matching resource.

This project doesn't have that problem, thanks to the `@ApplicationPath("/api")` on `JerseyConfig` from 6.2. Spring Boot reads the annotation and maps Jersey to `/api/*` only. Spring MVC keeps everything else, so http://localhost:9090/hello still works.

Two other ways to get there, if you'd rather not use the annotation:

**A. Set the prefix in `application.properties`:**

```properties
spring.jersey.application-path=/api
```

Same effect as the annotation. If both are present, the property wins.

**B. Register Jersey as a filter that passes unmatched requests through:**

```properties
spring.jersey.type=filter
```

Now anything Jersey doesn't recognize falls through to Spring MVC at the same paths.

Simplest of all: pick one style and drop the other. Mixing them is legal but rarely worth the confusion.

## 6.6 Two gotchas when returning entities as JSON

These are why 6.3 returns DTOs instead of entities.

**Infinite recursion.** `Users` holds a `Set<Listings>`, and each `Listings` holds a `Users`. Serializing one to JSON can loop forever and blow the stack. Fixes, in order of preference:

1. **Return a DTO** — a small purpose-built class with only the fields the client needs. This is the right answer in real projects, and what this one does.
2. Annotate the back-reference with `@JsonIgnore`.
3. Use `@JsonManagedReference` / `@JsonBackReference` on the two sides.

**`LazyInitializationException`.** Relationships are `FetchType.LAZY`, so a `Set<Listings>` is a placeholder until you touch it — and it can only load while the database session is open. Touching it after the session closes throws. In a Spring MVC app, Spring Boot's `spring.jpa.open-in-view=true` default keeps the session open for the whole request and hides the problem. It doesn't help here: open-in-view is a Spring MVC interceptor, and Jersey requests never pass through it, so the session closes as soon as each repository call returns. The durable fixes are fetching what you need up front with a `JOIN FETCH` query, or (again) mapping to a DTO that only reads what's already loaded. This project does both; [Part 8](#part-8--the-web-frontend) shows the `JOIN FETCH` side.

## 6.7 Return errors as JSON

A frontend needs every failure in a shape it can display. Left alone, the failures here would come back with generic bodies that don't say what went wrong: a failed validation wouldn't name the field, and a write that MySQL rejects would be a bare `500`. This project answers them all with the `ApiError` from 6.3:

```json
{"error": "email: must be a well-formed email address"}
```

Resources return an `ApiError` themselves for the failures they expect, like the taken username in 6.3. Everything else goes through **exception mappers**: classes that turn an exception into a response. There are three, in the `config` package:

| Mapper | Catches | Answers with |
| --- | --- | --- |
| `ConstraintViolationMapper` | `ConstraintViolationException`, thrown when `@Valid` fails | `400`, and one `field: problem` per failed rule |
| `WebApplicationExceptionMapper` | Jakarta REST's own exceptions, such as the `BadRequestException` that `ListingResource` throws for an unknown `userId` | The exception's status and message |
| `DataIntegrityViolationMapper` | Spring's `DataIntegrityViolationException`, when MySQL refuses a write | `409` |

The validation one does the most work:

```java
package com.siran.itemExchange.config;

import com.siran.itemExchange.dto.ApiError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolationMapper::describe)
                .collect(Collectors.joining("; "));

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiError(message.isEmpty() ? "Invalid request" : message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static String describe(ConstraintViolation<?> violation) {
        String field = StreamSupport.stream(violation.getPropertyPath().spliterator(), false)
                .map(Path.Node::getName)
                .reduce((first, last) -> last)
                .orElse("request");
        return field + ": " + violation.getMessage();
    }
}
```

A violation's property path runs from the method, through the parameter, down to the field. `describe` keeps only the last step, because that's the name the client actually sent.

Jersey only uses providers it has been told about, and `packages(...)` scans `resource/` alone. So `JerseyConfig` registers the mappers by name:

```java
@Component
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        packages("com.siran.itemExchange.resource");

        register(ConstraintViolationMapper.class);
        register(WebApplicationExceptionMapper.class);
        register(DataIntegrityViolationMapper.class);
    }
}
```

A mapper you forget to register is never called, and its failures go back to the generic response. [Part 8](#part-8--the-web-frontend) adds one more `register` line, for CORS.

One response still doesn't use `ApiError`: a lookup that finds nothing returns `Response.status(Response.Status.NOT_FOUND).build()`, with no body. Jersey hands an error status with no body to the servlet container, which forwards it to Spring Boot's standard error handler. So a missing ID comes back as:

```json
{"timestamp": "...", "status": 404, "error": "Not Found", "path": "/api/users/id/99"}
```

It has an `error` field too, so the frontend can show it the same way.

---

# Part 7 — Everyday workflow

## Running the app

| What | Command |
| --- | --- |
| Run from the terminal | `./mvnw spring-boot:run` |
| Run from IntelliJ | Click ▶ next to `main()` in `ItemExchangeApplication` |
| Stop it | `Ctrl + C` |
| Build a runnable JAR | `./mvnw clean package` → `target/itemExchange-0.0.1-SNAPSHOT.jar` |
| Run that JAR anywhere | `java -jar target/itemExchange-0.0.1-SNAPSHOT.jar` |

Live reload is worth setting up early — add `spring-boot-devtools` (scope `runtime`, `optional` true) and the app restarts itself whenever you recompile.

## After changing your schema

1. Apply the `ALTER TABLE` in MySQL.
2. Update the entity class — regenerate ([5.8 Option A](#58-when-your-schema-changes-later)) or hand-edit ([Option B](#58-when-your-schema-changes-later)).
3. Restart. `ddl-auto=validate` confirms the two sides agree.

## Files you must never commit

```gitignore
target/
.env
src/main/resources/hibernate.properties
.idea/
*.iml
```

Before your first push, verify nothing sensitive is staged:

```bash
git status --short
```

If a secret ever *is* committed, changing the password in MySQL is the only real fix — Git history keeps the old value forever.

---

# Part 8 — The web frontend

This API is consumed by a React single-page app that lives in its own repository,
**[web-exchange](https://github.com/sirangao/web-exchange)**. The two are deliberately
kept apart: this project owns the data and the HTTP surface, that one owns the UI.

Run this app first (`./mvnw spring-boot:run`, port 9090), then `npm start` in
web-exchange (port 3000). Its `package.json` sets `"proxy": "http://localhost:9090"`,
so the dev server forwards every `/api/*` request here. Because the browser only ever
talks to port 3000, the requests are same-origin and CORS never applies in development.

For a deployed build, where the frontend is served from a different origin, the
browser does enforce CORS, and the API has to send the headers itself. That's a Jersey
response filter, `config/CorsFilter.java`:

```java
package com.siran.itemExchange.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;

public class CorsFilter implements ContainerResponseFilter {

    private final String allowedOrigin;

    public CorsFilter(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().putSingle("Access-Control-Allow-Origin", allowedOrigin);
        response.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.getHeaders().putSingle("Access-Control-Allow-Headers", HttpHeaders.CONTENT_TYPE + ", " + HttpHeaders.AUTHORIZATION);
        response.getHeaders().putSingle("Access-Control-Max-Age", "3600");
    }
}
```

The allowed origin is a setting rather than a constant. Add it to
`application.properties`:

```properties
app.cors.allowed-origin=http://localhost:3000
```

`app.cors.allowed-origin` isn't a Spring Boot property; it's this project's own, and
`JerseyConfig` reads it with `@Value` to build the filter. This is `JerseyConfig` in its
final form:

```java
package com.siran.itemExchange.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        packages("com.siran.itemExchange.resource");

        register(new CorsFilter(allowedOrigin));
        register(ConstraintViolationMapper.class);
        register(WebApplicationExceptionMapper.class);
        register(DataIntegrityViolationMapper.class);
    }
}
```

Because that `@Value` has no default, the app refuses to start until the property exists.

## What the frontend needs from this API

Beyond the CRUD covered above, a marketplace UI needs a handful of endpoints worth
noting:

| Endpoint | Why it exists |
| --- | --- |
| `POST /api/users/login` | Confirms a username and password and returns the user. No token — see the warning below. |
| `GET /api/listings` | Browsing, filtered by `?type=` and `?category=` (by name). Returns `available` listings unless `?status=` asks for another. |
| `GET /api/listings/user/{userId}` | A seller's own listings, in every status. |
| `PUT /api/users/profile/{id}` | Contact details only, because `UserInput` requires a password the client never receives. |
| `GET /api/categories` | Fills the category filter and the create form. |

README.md's [API reference](README.md#api-reference) lists every endpoint, with its
request fields and status codes.

> ⚠️ **There is no authentication.** `POST /users/login` compares a plaintext password
> and hands back the user record; nothing afterwards is authenticated, so any caller
> can put any `userId` in a request body. That is fine on localhost and nowhere else.
> Adding BCrypt touches two lines — the hash in `apply()`, which both sign-up and
> `updateById` go through, and the compare in `login` — and a token or session is a
> larger, separate change.

## Two things the client can't work around

**Lazy relations.** `Listings.users` and `Listings.categories` are `FetchType.LAZY`,
and Jersey requests don't get Spring MVC's open-in-view, so the persistence session is
closed by the time a resource maps an entity. Reading `getUsers().getId()` happens to
work — a lazy proxy knows its own id without a query — but `getUsername()` throws
`LazyInitializationException`. That's why `ListingsRepository` has explicit
`join fetch` queries: a listing card shows the seller's name, so the name has to be
loaded up front. The bonus is that browsing is one query rather than one per listing.

**Enum drift.** The `@Pattern` regexes on `ListingInput` have to match the database's
`ENUM` definitions exactly. If they disagree, a value passes validation and then fails
on insert with an opaque constraint error. Check with:

```sql
SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'web_exchange' AND DATA_TYPE = 'enum';
```

---

# Troubleshooting

## Startup failures

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `Failed to determine a suitable driver class` | MySQL driver missing, or no `spring.datasource.url` | Check both `pom.xml` and `application.properties`; reload Maven |
| `Access denied for user '${MYSQL_USER}'` | `.env` not loaded — the literal placeholder was used | Wrong file location or wrong run-config working directory ([4.1](#41-keep-your-password-out-of-git--the-env-pattern)) |
| `Communications link failure` | MySQL not running | `brew services start mysql` / start the Windows service |
| `Unknown database 'web_exchange'` | Typo or missing DB | `SHOW DATABASES;` |
| `Schema-validation: missing table [users]` | Entity exists, table doesn't; or the app is connected to a different database than you think | Compare `spring.datasource.url` with `SHOW TABLES;` |
| `Port 9090 was already in use` | Something else is on the port | Change `server.port`, or `lsof -ti:9090 \| xargs kill` |
| `UnsupportedClassVersionError` | Running on a JDK older than 17 | `java -version`; fix the IDE's Project SDK |
| App starts but **no repository bean** | Class outside the `@SpringBootApplication` package tree | Everything must be under `com.siran.itemExchange` |
| `Could not resolve placeholder 'app.cors.allowed-origin'` | `JerseyConfig` reads a property that isn't set | Add `app.cors.allowed-origin=http://localhost:3000` to `application.properties` ([Part 8](#part-8--the-web-frontend)) |

## HTTP errors

| Symptom | Likely cause |
| --- | --- |
| `404` on a Jersey endpoint | The URL is missing the `/api` prefix; or the resource class isn't in the scanned package; or `@Path` is on the wrong element; or `JerseyConfig` is missing `@Component` |
| `404` on a Spring MVC endpoint after adding Jersey | Jersey has no application path, so it claims `/*` — see [6.5](#65-jersey-and-spring-mvc-in-the-same-app) |
| `500` with `LazyInitializationException` | Lazy collection touched after the session closed — see [6.6](#66-two-gotchas-when-returning-entities-as-json) |
| `500` with `StackOverflowError` during JSON serialization | Bidirectional relationship recursion — see [6.6](#66-two-gotchas-when-returning-entities-as-json) |
| `415 Unsupported Media Type` | Missing `@Consumes`, or the client didn't send `Content-Type: application/json` |

## Reverse-engineering failures

| Symptom | Likely cause |
| --- | --- |
| `Could not create JDBC connection` | MySQL isn't running, or `hibernate.properties` has wrong credentials, or the plugin is missing its nested `mysql-connector-j` dependency |
| No files generated, `BUILD SUCCESS` | The reveng filter matched nothing, or you're connected to an empty database — check `hibernate.connection.url` names the right DB |
| Entities generated but no relationships between them | The tables have no `FOREIGN KEY` constraints ([3.5](#35-create-the-tables)) |
| Files appear but the IDE shows red errors | The `package` line doesn't match the folder you moved them into ([5.5](#55-run-the-generator)) |

---

# Glossary

| Term | Meaning |
| --- | --- |
| **Bean** | An object Spring creates and manages for you. You get one by declaring a field and annotating it `@Autowired`. |
| **Bean Validation** | The Jakarta standard for declaring rules on fields (`@NotBlank`, `@Size`, `@Email`) and checking them with `@Valid`. Hibernate Validator implements it. |
| **Classpath** | The set of compiled classes and JARs visible at runtime. "On the classpath" = available to your program. |
| **DTO** | Data Transfer Object — a small class shaped for one API request or response, keeping entities out of your public interface. |
| **Entity** | A Java class annotated `@Entity` that maps to a database table. |
| **Hibernate** | The ORM. Implements the JPA specification; the actual thing generating your SQL. |
| **HikariCP** | The connection pool Spring Boot uses. Keeps DB connections open and reuses them. |
| **JAX-RS / Jakarta REST** | The specification for `@Path`/`@GET`-style REST endpoints. **Jersey** implements it. |
| **JDBC** | The low-level Java API for talking to any SQL database. Everything above is built on it. |
| **JPA** | Jakarta Persistence API — the ORM *specification* (`@Entity`, `@Column`, …). Hibernate is one implementation. |
| **Lombok** | A library that generates boilerplate, such as getters and setters, from annotations (`@Getter`, `@Setter`) at compile time. |
| **Maven** | The build tool. Reads `pom.xml`, downloads dependencies, compiles, packages. |
| **ORM** | Object-Relational Mapping — the technique of mapping tables to classes. |
| **POM** | Project Object Model — the `pom.xml` file. |
| **Reverse engineering** | Generating code from an existing database schema (`hbm2java`). |
| **Starter** | A Spring Boot dependency bundle: one line that pulls in a coherent set of libraries at compatible versions. |

---

# Quick reference card

```bash
# ── Build & run ────────────────────────────────────────────────
./mvnw clean compile                # compile
./mvnw spring-boot:run              # run (Ctrl+C to stop)
./mvnw clean package                # build a runnable JAR
java -jar target/itemExchange-0.0.1-SNAPSHOT.jar

# ── Generate entities from the database ────────────────────────
./mvnw hibernate:hbm2java           # → target/generated-sources/

# ── MySQL ──────────────────────────────────────────────────────
brew services start mysql           # macOS
mysql -u exchange_app -p web_exchange
#   SHOW TABLES;
#   SHOW CREATE TABLE listings\G
#   SELECT * FROM users;

# ── Test the API ───────────────────────────────────────────────
curl -i http://localhost:9090/api/categories
curl -i http://localhost:9090/api/users/id/1
curl -i -X POST http://localhost:9090/api/users/add \
     -H "Content-Type: application/json" \
     -d '{"username":"alice","password":"x","email":"a@b.com"}'
```

| File | Purpose |
| --- | --- |
| `pom.xml` | Dependencies and build plugins |
| `src/main/resources/application.properties` | Runtime config — DB URL, port, JPA settings, CORS origin |
| `.env` | Secrets. **Never committed.** |
| `src/main/resources/hibernate.properties` | DB connection for the code generator. **Never committed.** |
| `hibernate-reverse-engineering.xml` | Which tables to generate, and into which package |
| `.../ItemExchangeApplication.java` | Entry point (`main`) |
| `.../config/JerseyConfig.java` | Mounts Jersey at `/api` and registers the endpoints, the CORS filter, and the error mappers |
| `.../config/*Mapper.java` | Turn exceptions into `{"error": "..."}` responses |
| `.../config/CorsFilter.java` | CORS headers, for a frontend served from another origin |
| `.../dataObjects/*.java` | Generated entities |
| `.../dataRepositories/*.java` | Spring Data repository interfaces |
| `.../dto/*.java` | Request and response bodies (DTOs) |
| `.../resource/*.java` | REST endpoints |
