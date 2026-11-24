# Add an operation and a toolbar button

## Trigger and scope

Use this workflow when the user asks to add a CaDoodle operation with a UI button.
The work MUST include the operation, parser, parameter editing, toolbar button,
translations, theme assets, website documentation, and validation. A button in the
application alone does not complete the task.

The agent MUST follow repository instructions and preserve existing user changes
in each repository. The agent MUST NOT commit, push, or deploy unless requested.
If access or a required tool is unavailable, report the blocked step instead of
claiming completion.

## 1. Inspect the repositories

Check `git status --short` in each repository before editing. The kernel and JCSG
are nested repositories; inspect their status separately.

| Component | Location |
| --- | --- |
| Application | Current CaDoodle-Application repository |
| Operation kernel | `BowlerStudio/libraries/bowler-script-kernel` |
| Geometry library | `BowlerStudio/libraries/bowler-script-kernel/JCSG` |
| Documentation website | `/home/hephaestus/git/cadoodlecad.github.io` |
| Theme repository | `/home/hephaestus/Documents/CaDoodle-workspace/gitcache/github.com/CommonWealthRobotics/Style-Cadoodle` |

The external paths identify the checkouts used for this workflow. If a path is
absent on another machine, locate the checkout or ask the user. The agent MUST
request the required sandbox access before reading or editing an external path.
Request only the repository that needs access.

Read `agents.md` or `AGENTS.md` in each repository where present. Inspect the
website navigation and all current theme CSS files; do not assume that a saved
list of themes is complete.

## 2. Define the operation and defaults

Use `Sweep` as a reference for document-backed parametrics. Use `Simplify` as a
reference for an operation that modifies selected incoming objects and preserves
their names. Choose the base class for the required behavior instead of copying a
base class solely to obtain UI listeners.

For mesh operations, inspect the actual methods in `CSGManifold3d` and their native
requirements. The agent MUST confirm parameter types, units, valid values, and
method order. It MUST NOT reinterpret an API setting as a pass count without the
user's agreement.

Agree on genuinely unclear behavior before implementation:

- Which selected objects are affected, and how groups are handled.
- Which methods run, and in which order.
- Which values enable or disable a method.
- Initial defaults, toolbar position, and Basic or Advanced mode visibility.

For numeric method switches, use the requested rule: values at or below zero skip
the method; positive values apply it. Distinguish switches from supporting
settings. For example, a blend value of zero can be valid when smoothing is on.

Defaults MUST perform the requested primary action. Optional methods SHOULD start
disabled unless the user requests them. For reference, Simplify starts with a
`0.5` distance tolerance and smoothing/refinement disabled. Those values are
specific to Simplify; do not copy them to unrelated operations.

## 3. Implement the operation and parser

Operation classes live under:

`BowlerStudio/libraries/bowler-script-kernel/src/main/java/com/neuronrobotics/bowlerstudio/scripting/cadoodle/`

The implementation MUST:

- Use **Java 8 syntax**, even when the build or native bindings require a newer JDK.
  Use ordinary classes instead of records and explicit casts instead of
  pattern-matching `instanceof`. Avoid newer convenience APIs in added tests, such
  as `List.of`, `Set.of`, and `String.isBlank`.
- Keep the feature in a self-contained module; add a geometry helper only when
  the API requires one.
- Register the operation type in `CaDoodleJsonOperationAdapterFactory`.
- Serialize the required operation identity, selection, and initial settings.
- Modify only the intended incoming objects. Preserve ordering, names, appearance,
  and relevant properties; leave unselected objects untouched.
- Handle empty, missing, and duplicate selection names without adding unintended
  objects or applying the same step repeatedly.
- Recalculate parameter edits from the input to the operation, instead of
  accumulating changes on its previous output.
- Preserve document parameter values through save/load and replay.

For native geometry pipelines, verify whether intermediate normals, properties,
or tangents survive conversion to CSG. Keep dependent stages in one native
pipeline when necessary. Release native resources on success and failure. Test
that each enabled control has its intended effect. Document any required automatic
stage, such as the refinement used to represent a smoothed surface.

## 4. Build the parameter controls

Use the consumed API type to select the control:

| API type | Parameter/control |
| --- | --- |
| Floating-point number | `LengthParameter` with only lower and upper options; a typable numeric field |
| Integer | `LengthParameter` with an explicit list of integer options |
| Enum | `StringParameter` with explicit string options, parsed into the consumed enum |
| String | `StringParameter` with the supported string options |

Use stable, document-backed parameter keys. Prevent collisions between separate
operation instances. When one step affects several objects, state whether its
parameters are shared or per-object, and test that behavior.

Inspect `SelectionSession` instead of assuming that attaching parameters makes
them editable:

- Its parameter filter MUST admit the new operation's keys. Selected-object names
  can differ from the operation name. Simplify uses an explicit parameter namespace
  with `isParameterKey` and `ownsParameterKey` helpers.
- Its parameter listeners MUST regenerate the correct operation when a value
  changes. An operation outside `AbstractAddFrom` can need its own listener hookup.
- Listener registration MUST preserve upstream operations' listeners and avoid
  duplicate registration for parameters shared across selected objects.
- Two numeric bounds MUST produce a typable field; explicit integer option lists
  MUST produce choices. Verify the current UI implementation.
- Parameter changes during document initialization or regeneration MUST NOT cause
  recursive regeneration.

## 5. Add the toolbar button and application icon

Update these application files as needed:

- `src/main/resources/com/commonwealthrobotics/MainWindow.fxml`
- `src/main/java/com/commonwealthrobotics/MainController.java`
- `src/main/java/com/commonwealthrobotics/controls/SelectionSession.java`
- `src/main/resources/com/commonwealthrobotics/stylesheet.css`

Connect the FXML action to the controller and selection operation. Use the existing
operation-running checks and focus behavior. Disable selection-dependent actions
when no objects are selected. Add the requested separators and verify the button's
mode visibility.

Create an original, recognizable SVG in `imageSource/` and export a transparent PNG
under `src/main/resources/com/commonwealthrobotics/light/`. Connect the PNG through
an ImageView CSS class so the FXML sizing remains effective. Inspect the image at
toolbar size; a passing resource test alone does not establish visual quality.

## 6. Translate the tooltip and operation label

The tooltip MUST use a translation key, for example `text="%SimplifyMesh"` in FXML.
The timeline operation label MUST also resolve through the translation system;
check whether it uses the operation class name.

Add real translations to **every existing** `Messages*.properties` bundle in
`src/main/resources/lang/`. The inventory for this feature was English plus
60 non-English languages, for 61 bundles total. Discover the current inventory
instead of silently omitting new locales.

Verify that each required key occurs exactly once per bundle and has a nonempty
value. Preserve existing translations and file content. Do not substitute English
for all non-English translations.

## 7. Update every theme

The bundled application stylesheet is only one part of the icon work. Update the
external Style-Cadoodle repository too.

- Inspect every root-level theme CSS file and its actual toolbar background.
- Add the new ImageView selector to **every theme**.
- Add light and dark PNG variants and preserve editable SVG sources.
- Design the dark variant for dark backgrounds, with visible outlines, faces, and
  action marks. Do not reuse a low-contrast light icon unchanged.
- Shared asset directories are appropriate; each theme MUST explicitly reference
  the correct asset.
- Inspect custom theme sets, including Clinical and contributed themes.

For Simplify, Clinical Dark, Dark-Blue, and High-Contrast use `dark/simplify.png`.
The other six themes use `light/simplify.png`. Recheck this mapping when themes
change.

Verify that all referenced assets exist, decode, and have the intended dimensions
and transparency. Review previews on the themes' actual background colors. Report
separately whether the running application was manually checked.

## 8. Add website documentation

The website uses MkDocs with the Read the Docs theme. A kernel developer note
alone does not satisfy this step.

In the website repository:

1. Add a user-facing guide under `docs/docs/tutorial/`.
2. Add the guide to the Tutorial navigation in `docs/mkdocs.yml`.
3. Add a relevant link from Getting Started or an existing tutorial.
4. Add the button image or useful screenshots under `docs/docs/img/`.

The guide MUST explain the button location and mode, selection behavior, defaults,
parameter meanings and units, disabled values, and practical adjustments. Include
shape/detail tradeoffs and any automatic processing that affects the result.
Use the actual UI labels. Keep implementation details in developer documentation
instead of the user guide.

Build locally with:

```sh
mkdocs build --strict -f docs/mkdocs.yml
```

Check the generated page's local links and images. Do not deploy unless requested.

## 9. Test and format

Add dedicated behavior tests for:

- Parser round-trip, saved document parameters, and default settings.
- Selected versus unselected objects, multiple targets, duplicate names, and an
  empty selection.
- Zero and negative method switches, positive values, and invalid integer inputs.
- Regeneration from the original input and parameter ownership across operations.
- Each geometry method and useful combined pipelines, using real geometry where
  possible. Test meaningful surface or volume changes instead of only nonempty output.
- Button wiring, placement/separator requirements, translated resources, and icon
  loading.
- Parameter listener registration, upstream listener preservation, and regeneration
  scheduling.

Use the project's required JDK for builds; Java 8 syntax does not imply that the
native libraries run on Java 8. Discover a suitable `JAVA_HOME` on the current
machine. Do not change the project's runtime target merely to enforce syntax.

Run formatting with `./gradlew spotlessApply`. Nested projects can require their
own invocation. For example, from the kernel checkout:

```sh
./gradlew :spotlessApply :JCSG:spotlessApply
./gradlew :test --tests junit.bowler.SimplifyTest
```

From the application checkout:

```sh
./gradlew :spotlessApply
./gradlew :test --tests '*Simplify*'
```

Replace the example test names for the new feature. Use `:test` for a targeted
project test; unqualified `test --tests ...` can apply the filter to subprojects
that have no matching tests. Use `--offline` only when dependencies are available.

Inspect formatting changes and preserve unrelated work. Run `git diff --check`
in every edited repository. Verify Java 8 syntax in all added or modified feature
code, including tests.

## Completion checklist

The final report MUST distinguish completed work, failed checks, blocked work,
and manual UI checks that were not performed.

- [ ] Operation and parser registration complete.
- [ ] Defaults and method-switch behavior tested.
- [ ] Parameters use controls appropriate to API types.
- [ ] Parameter visibility, editing, regeneration, and save/load verified.
- [ ] Java 8 syntax used in feature code and tests.
- [ ] Button, separator, selection state, and mode visibility wired.
- [ ] Application SVG/PNG and CSS selector added.
- [ ] Tooltip and operation name translated in every language bundle.
- [ ] Every external theme has the selector and correct light/dark asset.
- [ ] Light and dark icon previews reviewed.
- [ ] Website guide, navigation, cross-link, and images added.
- [ ] Strict website build and generated resource checks passed.
- [ ] Targeted tests, formatting, and diff checks passed.
- [ ] Changed repositories and any remaining limitations reported.
- [ ] No unrequested commit, push, or deployment performed.
