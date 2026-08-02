# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`CONTEXT-MAP.md`** at the repo root — it points at one `CONTEXT.md` per context. Read each one relevant to the topic.
- **`CONTEXT.md`** at the repo root (if it exists) — shared domain glossary across all contexts.
- **`docs/adr/`** at the repo root — system-wide architectural decisions that apply to all contexts.
- **`<context>/docs/adr/`** — context-specific decisions. In this repo: `app/docs/adr/` and `svc/docs/adr/`.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

Multi-context repo (presence of `CONTEXT-MAP.md` at the root):

```
/
├── CONTEXT-MAP.md                    ← maps context names to paths
├── CONTEXT.md                        ← optional: shared cross-context glossary
├── docs/adr/                         ← system-wide ADRs
├── app/
│   ├── CONTEXT.md                    ← app-specific domain glossary
│   └── docs/adr/                     ← app-specific ADRs
└── svc/
    ├── CONTEXT.md                    ← svc-specific domain glossary
    └── docs/adr/                     ← svc-specific ADRs
```

### Contexts

| Context | Path | Description |
| ------- | ---- | ----------- |
| `app`   | `app/` | 前端 |
| `svc`   | `svc/` | 后端 |

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in the relevant `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders) — but worth reopening because…_
