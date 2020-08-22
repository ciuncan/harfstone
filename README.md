# Harfstone

## Projects

- **`harfstone-core`**: Defines the core logic of game Harfstone which can be used with any fronted.A later addition might be to use ScalaJS to create a HTML/`canvas` based front-end and make it run in browsers.

- **`harfstone-console`**: Defines the console user interface, interactions and game sequence based on the logic defined in `harfstone-core`.

## Running

To run console game: 
```bash
sbt harfstone-console/run
```

To run tests with coverage:
```bash
sbt clean coverage test
sbt coverageReport
```