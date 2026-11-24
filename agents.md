Formatting and linting should be done with `./gradlew spotlessApply`.

Always use Java 8-compatible syntax and Java 8 standard-library APIs in new or modified Java code, including tests, even when building with a newer JDK. Do not introduce newer constructs or APIs such as `var`, `Map.of`, `List.of`, `Path.of`, `String.isBlank`, or `Stream.toList`.
