# Harfstone

A simple card game written in Scala using ZIO.

Harfstone is a word game on Hearthstone, a deck building card game, where "harf" means "letter" in Turkish, hence the gross simplification of the complex game Hearthstone. Cards are identified by their mana costs (0-8) and the damage they do is equal to their mana cost.

[![Scala CI](https://github.com/ciuncan/harfstone/workflows/Scala%20CI/badge.svg)](https://github.com/ciuncan/harfstone/actions)

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

## Creating Binaries

`harfstone-console` uses `sbt-native-image` plugin to create native images.

```bash
sbt harfstone-console/nativeImage
```

Run:

```bash
./harfstone-console/target/native-image/harfstone-console
```
