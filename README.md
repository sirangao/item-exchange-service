# itemExchange

The REST API behind **CampusExchange**, a secondhand marketplace where college students buy, sell, and swap things like textbooks, furniture, and bikes.

This repository is the backend: a Spring Boot service that stores users, listings, and categories in MySQL and serves them as JSON under `/api`. The user interface is a separate React app, **[web-exchange](https://github.com/sirangao/web-exchange)**, which calls this API. Start this one first, then the frontend.

> [!TIP]
> **Want to see how it was built?** [GUIDE.md](GUIDE.md) is a beginner-friendly walkthrough that takes this project from an empty folder to a running API: installing the tools, Spring Initializr, MySQL, generating the entity classes with Hibernate, and writing the Jersey endpoints.

## What it does

- **Accounts**: register, sign in, look up users, and edit profile contact details
- **Listings**: post items to sell or exchange, edit or delete them, browse by type and category, and see everything one user has posted
- **Categories**: a fixed list (Textbooks, Electronics, Furniture, …) for organizing and filtering listings
- **Readable errors**: validation failures and conflicts come back as `{"error": "..."}`, ready for the frontend to display

## Architecture

```
                  Browser
                     │
                     ▼
┌─────────────────────────────────────────┐
│ web-exchange (React frontend)           │
│ http://localhost:3000                   │
└────────────────────┬────────────────────┘
                     │ dev-server proxy: /api/* → :9090
                     ▼
┌─────────────────────────────────────────┐
│ itemExchange (this repo)                │
│ http://localhost:9090/api               │
│ Jersey → Spring Data JPA → Hibernate    │
└────────────────────┬────────────────────┘
                     │ JDBC
                     ▼
┌─────────────────────────────────────────┐
│ MySQL · localhost:3306 · web_exchange   │
└─────────────────────────────────────────┘
```

A request arrives at a Jersey resource class in `resource/`, which validates the JSON body against a DTO in `dto/`. It then loads or saves entities (`dataObjects/`) through Spring Data repositories (`dataRepositories/`) and returns a response DTO. Entities are never serialized directly; every response is a DTO.

## Tech stack

| Layer | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 4.0.7 |
| REST endpoints | Jersey (Jakarta REST / JAX-RS) |
| Data access | Spring Data JPA + Hibernate |
| Database | MySQL 8 |
| Validation | Jakarta Bean Validation |
| Build | Maven, through the included wrapper (`./mvnw`) |
| Tooling | Hibernate Tools (generates entities from the schema), Lombok |

## Getting started

### Prerequisites

- **JDK 17 or newer**: check with `java -version`
- **MySQL 8**, running on `localhost:3306`
- **Node.js**, only needed for the web-exchange frontend

You don't need to install Maven: `./mvnw` downloads the right version the first time it runs. If you're setting up any of these for the first time, [GUIDE.md Part 0](GUIDE.md#part-0--install-the-tools) has install steps for macOS, Windows, and Linux.

### 1. Clone both repositories

The database schema lives in web-exchange, so you need both:

```bash
git clone https://github.com/sirangao/item-exchange-service.git
git clone https://github.com/sirangao/web-exchange.git
```

### 2. Create the database

Both projects are built against one schema, kept in web-exchange's [`database/schema.sql`](https://github.com/sirangao/web-exchange/blob/master/database/schema.sql). It creates the `web_exchange` database, its six tables, and a starter set of categories. Run it once, from the web-exchange folder:

```bash
mysql -u root -p < database/schema.sql
```

If `mysql` isn't on your `PATH`, the macOS installer puts it at `/usr/local/mysql/bin/mysql`.

Optionally, give the app its own MySQL user that can only touch this database. Log in with `mysql -u root -p` and run:

```sql
CREATE USER 'exchange_app'@'localhost' IDENTIFIED BY 'choose-a-password';
GRANT ALL PRIVILEGES ON web_exchange.* TO 'exchange_app'@'localhost';
```

### 3. Add your database credentials

In the root of this repository (next to `pom.xml`), create a file named `.env`:

```properties
MYSQL_USER=exchange_app
MYSQL_PASSWORD=choose-a-password
```

Use the user from step 2, or `root` if you skipped it. `application.properties` loads this file at startup, and `.gitignore` keeps it out of Git.

### 4. Start the API

```bash
./mvnw spring-boot:run
```

On Windows, run `mvnw.cmd spring-boot:run`. The first run downloads dependencies, so it takes a while. Once the log shows `Started ItemExchangeApplication`, the API is listening on port 9090. Check it:

```bash
curl http://localhost:9090/api/categories
```

You should get the categories back as JSON: Bikes, Clothing, Electronics, and so on.

At startup Hibernate checks every entity class against its table (`ddl-auto=validate`). If the database is missing a table or column, the app stops with an error that names it instead of starting in a broken state.

### 5. Start the frontend

In the web-exchange folder:

```bash
npm install
npm start
```

This opens http://localhost:3000. Register an account and post a listing: it goes through this API into MySQL.

## Working with web-exchange

The two repositories are one application split in two: this one owns the data and the HTTP API, and [web-exchange](https://github.com/sirangao/web-exchange) owns the UI.

| | itemExchange (this repo) | web-exchange |
| --- | --- | --- |
| What it is | Spring Boot REST API | React 18 single-page app |
| URL | http://localhost:9090/api | http://localhost:3000 |
| Start it with | `./mvnw spring-boot:run` | `npm start` |

How they fit together:

- **The dev server proxies API calls.** web-exchange sends its requests to `/api/...` on its own origin, and its `package.json` sets `"proxy": "http://localhost:9090"`, so the React dev server forwards them here. The browser only ever talks to port 3000, so CORS doesn't come into play during development.
- **CORS is ready for deployment.** If the built frontend is served from a different origin, `CorsFilter` (registered in `JerseyConfig`) adds CORS headers for the origin in `app.cors.allowed-origin`.
- **One error format.** Error messages always come back as `{"error": "..."}`, and web-exchange's `apiError()` helper shows that text to the user. New endpoints should keep the same field name.
- **One schema.** The schema lives in web-exchange's `database/schema.sql`. This app never changes it; Hibernate only validates against it. Changing a table means updating that file, your database, and the matching entity class in `dataObjects/`.
- **Shared values.** The allowed values for `listingType`, `status`, and `conditionGrade` come from MySQL `ENUM` columns. The `@Pattern` checks in `ListingInput` and the options in web-exchange's forms have to use exactly the same strings, or a value passes validation and then fails on insert.

## API reference

All paths are relative to `http://localhost:9090/api`, and request and response bodies are JSON. Any path with an `{id}`, `{userId}`, or `{username}` returns `404` if that record doesn't exist.

### Users

| Method | Path | Body | Response |
| --- | --- | --- | --- |
| `POST` | `/users/add` | `UserInput` | `201` with the new user, or `409` if the username or email is taken |
| `POST` | `/users/login` | `username`, `password` | The user, or `401` if they don't match |
| `GET` | `/users/{username}` | | The user |
| `GET` | `/users/id/{id}` | | The user |
| `PUT` | `/users/profile/{id}` | `email`, `phone`, `college` | The updated user, or `409` if the email is taken |
| `PUT` | `/users/updateById/{id}` | `UserInput` | The updated user. Replaces every field, including the password. |

`UserInput` fields: `username` (required, max 50 characters), `password` (required), `email` (required, max 100), `phone` (max 20), `college` (max 100).

A user response has `id`, `username`, `email`, `phone`, `college`, `createdAt`, and `updatedAt`. The password is never sent back.

### Listings

| Method | Path | Body | Response |
| --- | --- | --- | --- |
| `GET` | `/listings` | | Listings, newest first (filters below) |
| `GET` | `/listings/{id}` | | The listing |
| `GET` | `/listings/user/{userId}` | | Every listing that user has posted, in any status |
| `POST` | `/listings/add` | `ListingInput` | `201` with the new listing |
| `PUT` | `/listings/updateById/{id}` | `ListingInput` | The updated listing |
| `DELETE` | `/listings/deleteById/{id}` | | `200` with no body |

`GET /listings` returns only `available` listings unless you pass `?status=`. You can also filter with `?type=` (`sell`, `exchange`, or `both`) and `?category=`, which takes a category **name** such as `Textbooks`, not an ID.

`ListingInput` fields:

| Field | Required | Allowed values |
| --- | --- | --- |
| `userId` | yes | ID of an existing user |
| `categoryId` | no | ID of an existing category |
| `title` | yes | Up to 150 characters |
| `description` | no | Any text (saved as `none` if left out) |
| `listingType` | yes | `sell`, `exchange`, `both` |
| `price` | no | 0 or more, up to 2 decimal places (saved as `0` if left out) |
| `conditionGrade` | yes | `new`, `like_new`, `good`, `fair`, `poor` |
| `status` | no | `available` (default), `pending`, `sold`, `exchanged` |
| `imageUrl` | no | A link to an image, up to 500 characters |

`PUT` replaces the whole listing, so send every field, not just the ones that changed. Anything you leave out goes back to its default.

A listing response looks like this. `categoryName` and the `seller*` fields are included so a listing card or detail page needs only one request:

```json
{
  "id": 12,
  "userId": 3,
  "categoryId": 1,
  "title": "Calculus: Early Transcendentals, 8th ed.",
  "description": "Light highlighting in chapter 3",
  "listingType": "sell",
  "price": 40.00,
  "conditionGrade": "good",
  "status": "available",
  "imageUrl": null,
  "categoryName": "Textbooks",
  "sellerUsername": "alice",
  "sellerEmail": "alice@example.edu",
  "sellerPhone": null,
  "createdAt": "2026-09-18T20:15:00Z",
  "updatedAt": "2026-09-18T20:15:00Z"
}
```

### Categories

| Method | Path | Response |
| --- | --- | --- |
| `GET` | `/categories` | Every category (`id`, `name`), sorted by name |

### Errors

When a request fails with a message, the body is `{"error": "..."}`:

| Status | When | Example |
| --- | --- | --- |
| `400` | A field fails validation, or points at a user or category that doesn't exist | `{"error": "title: must not be blank"}` |
| `401` | Wrong username or password at login | `{"error": "Invalid username or password"}` |
| `409` | The username or email is already in use | `{"error": "That username is already taken"}` |

A `404` for a missing record is the one exception. The resource sends it without a body, so Spring Boot fills in its standard error JSON instead: `{"timestamp": "...", "status": 404, "error": "Not Found", "path": "..."}`. It still has an `error` field.

## Configuration

Settings are in `src/main/resources/application.properties`:

| Property | Value | Purpose |
| --- | --- | --- |
| `server.port` | `9090` | Port the API listens on. web-exchange's proxy points here. |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/web_exchange` | Which MySQL database to use |
| `spring.datasource.username` / `password` | `${MYSQL_USER}` / `${MYSQL_PASSWORD}` | Filled in from `.env` |
| `app.cors.allowed-origin` | `http://localhost:3000` | Origin allowed to call the API directly. Only matters when the frontend isn't going through the dev proxy. |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Check the entities against the tables at startup, and never modify the schema |
| `spring.jpa.show-sql` | `true` | Log every SQL statement Hibernate runs |

## Project structure

```
.
├── .env                                  your database credentials (you create this; not committed)
├── pom.xml                               dependencies and build plugins
├── hibernate-reverse-engineering.xml     settings for regenerating entities from the database
├── GUIDE.md                              how this project was built, step by step
└── src/main/
    ├── resources/application.properties  runtime configuration
    └── java/com/siran/itemExchange/
        ├── ItemExchangeApplication.java  entry point
        ├── config/                       JerseyConfig (mounts /api), CorsFilter, exception-to-JSON mappers
        ├── resource/                     REST endpoints: users, listings, categories
        ├── dto/                          request and response bodies
        ├── dataObjects/                  JPA entities, generated from the MySQL schema
        └── dataRepositories/             Spring Data repositories
```

## Useful commands

| Command | What it does |
| --- | --- |
| `./mvnw spring-boot:run` | Start the API |
| `./mvnw test` | Run the tests. The test starts the whole app, so MySQL has to be running. |
| `./mvnw clean package` | Build a runnable jar in `target/`. Add `-DskipTests` to build without MySQL. |
| `java -jar target/itemExchange-0.0.1-SNAPSHOT.jar` | Run the built jar. Start it from the repository root so it finds `.env`. |

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `Access denied for user '${MYSQL_USER}'` | `.env` wasn't loaded. It must be in the repository root, and the app must be started from there. In an IDE, set the run configuration's working directory to the project root. |
| `Access denied for user 'exchange_app'@'localhost'` | The username or password in `.env` is wrong. Test them with `mysql -u exchange_app -p`. |
| `Communications link failure` | MySQL isn't running. |
| `Schema-validation: missing table` or `missing column` | Your database doesn't match the entity classes. Create it from web-exchange's `database/schema.sql`. |
| `Port 9090 was already in use` | Another copy of the API is still running. Stop it, or change `server.port` and web-exchange's `proxy` to match. |
| web-exchange shows errors and its terminal says `Proxy error: Could not proxy request` | The API isn't running on port 9090. |

[GUIDE.md](GUIDE.md#troubleshooting) has a longer list.

## Limitations

> [!WARNING]
> **There is no real authentication.** Passwords are stored and compared as plain text, login returns the user record without issuing a token, and no endpoint checks who is calling, so a client can send any `userId`. That's fine on localhost, but not on anything reachable from the internet.

- **Some tables have no endpoints yet.** `meetups`, `messages`, and `listing_payment_methods` are mapped to entity classes, but nothing exposes them over HTTP, so those parts of web-exchange are placeholders.
- **Images are links.** `imageUrl` stores a URL. There's no file upload.
