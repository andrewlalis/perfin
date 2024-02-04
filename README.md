# Perfin

![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/andrewlalis/perfin/run-tests.yaml?style=flat-square&logo=github)
![GitHub release (with filter)](https://img.shields.io/github/v/release/andrewlalis/perfin?style=flat-square)

A personal accounting desktop app to track your finances using an approachable
interface and interoperable file formats for maximum compatibility.

![](design/main-view-screenshot.png "main view screenshot")

## Download

Head to the [releases](https://github.com/andrewlalis/perfin/releases) page and
find the installer that's compatible for your system.

> No compatible release for your system? [Report it here.](https://github.com/andrewlalis/perfin/issues)

## About Perfin

Perfin is a desktop app built with Java 21 and JavaFX. It's intended to be used
by individuals to track their finances across multiple accounts (savings,
checking, credit, etc.).

Because the app lives and works entirely on your local computer, you can rest
assured that your data remains completely private.

## Release Procedure

Platform-specific package installers are generated automatically via GitHub
Actions (see `.github/workflows/make-release.yaml`), which is triggered by a
new tag being pushed to the `main` branch. Follow these steps to push a release:

1. Run `java scripts/SetVersion.java 1.2.3` (replacing `1.2.3` with the new version number)
to set the version everywhere that it needs to be.
2. Add a tag to the `main` branch with `git tag v1.2.3`.
3. Push the tag to GitHub with `git push origin v1.2.3`.

Once that's done, the workflow will start, and you should see a release appear
in the next few minutes.

## Migration Procedure

Because this application relies on a structured relational database schema,
changes to the schema must be handled with care to avoid destroying users' data.
Specifically, when changes are made to the schema, a *migration* must be defined
which provides instructions for Perfin to safely apply changes to an old schema.

The database schema is versioned using whole-number versions (1, 2, 3, ...), and
a migration is defined for each transition from version to version, such that
any older version can be incrementally upgraded, step by step, to the latest
schema version.

Perfin only supports the latest schema version, as defined by `JdbcDataSourceFactory.SCHEMA_VERSION`.
When the app loads a profile, it'll check that profile's schema version by
reading a `.jdbc-schema-version.txt` file in the profile's main directory. If
the profile's schema version is **less than** the current, Perfin will
ask the user if they want to upgrade. If the profile's schema version is
**greater than** the current, Perfin will tell the user that it can't load a
schema from a newer version, and will prompt the user to upgrade.

### Writing a Migration

1. Write your migration. This can be plain SQL (placed in `resources/sql/migration`), or Java code.
2. Add your migration to `com.andrewlalis.perfin.data.impl.migration.Migrations#getMigrations()`.
3. Increment the schema version defined in `JdbcDataSourceFactory`.
4. Test the migration yourself on a profile with data.
