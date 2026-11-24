# Repository instructions

Formatting and linting SHOULD use `./gradlew spotlessApply`.

## Add an operation and a button

When asked to add a CaDoodle operation with a UI button, the agent MUST read and
follow [the operation-and-button workflow](docs/workflows/add-operation-and-button.md).
The workflow includes the kernel/parser, Java 8 syntax, parametric controls and
regeneration, application UI, all translations, light/dark icons in every theme,
website documentation, and validation. The application change alone does not
complete the task.
Formatting and linting should be done with `./gradlew spotlessApply`.

Always use Java 8-compatible syntax and Java 8 standard-library APIs in new or modified Java code, including tests, even when building with a newer JDK. Do not introduce newer constructs or APIs such as `var`, `Map.of`, `List.of`, `Path.of`, `String.isBlank`, or `Stream.toList`.
