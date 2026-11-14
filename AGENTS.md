## Agent skills

### Issue tracker

Issues are stored as local markdown files under `.scratch/<feature-slug>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default label mapping — the five canonical role names are used as-is. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout — one `GLOSSARY.md` plus `docs/adr/` at the repo root. Read before naming a new concept or overturning an existing decision. See `docs/agents/domain.md`.

## Code style

Read the guide for the language before writing code; check package layout, naming, and comments against it before finishing.

- Java / Spring / Maven modules → `docs/guides/spring-boot-code-style.md`
- Vue components, stores, views, styles → `docs/guides/vue-code-style.md`

## Commits

Read `docs/guides/git-commit.md` before committing — type and scope tables; the format is Angular Conventional Commits.
