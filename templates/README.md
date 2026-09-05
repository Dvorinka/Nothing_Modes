# Community Templates

Pre-built routines and modes that install directly into Nothing Modes.
The app reads `index.json` in this directory; each entry points to an
`ExportBundle` JSON file - the same format the app produces when you share
a routine.

## Adding a template

1. Build the routine in the app, then use **Share** on the routine detail
   screen to export its JSON bundle.
2. Copy the bundle here as `<your-template-id>.json`.
3. Add an entry to `index.json`:

```json
{
  "id": "my-template",
  "name": "My Template",
  "description": "One sentence on what it does and when it fires.",
  "creator": "your-github-username",
  "emoji": "✨",
  "tags": ["tag-one", "tag-two"],
  "minSchemaVersion": 1,
  "file": "my-template.json"
}
```

4. Open a pull request. Keep one template per file and keep ids in
   `kebab-case`.

## Rules

- Templates must import cleanly: valid `ExportBundle` JSON,
  `schemaVersion: 1`, trigger `tz` may be any valid `ZoneId` (the app
  re-localizes on install).
- Declare `requiredCapabilities` honestly - the app shows them as
  compatibility warnings before install.
- No personal data, no secrets, no device-specific identifiers.
- Give each automation an id prefixed with `tmpl-` to avoid collisions.
- Keep `enabled: false`; imported templates always start disabled.
