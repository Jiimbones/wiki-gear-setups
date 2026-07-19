# Wiki Gear Setups — RuneLite plugin

Imports recommended gear setups from the OSRS Wiki (`{{Recommended equipment}}` template) and writes them as bank tag layouts, personalized to what the player owns.

## Doc nodes (read only what the task needs)
- [docs/build-plan.md](docs/build-plan.md) — phases, status, next task. **Start here each session.**
- [docs/architecture.md](docs/architecture.md) — components, data flow, key RuneLite APIs.
- [docs/wiki-format.md](docs/wiki-format.md) — wikitext template format + parsing rules.
- [docs/decisions.md](docs/decisions.md) — locked decisions; don't re-litigate.
- [docs/changelog.md](docs/changelog.md) — append entry per meaningful change.

## Conventions
- Target: RuneLite Plugin Hub (Java 11 bytecode, hub dependency whitelist — no external libs beyond what RuneLite bundles: OkHttp, Gson, Guava, Lombok).
- Package: `com.wikigearsetups`. Plugin class: `WikiGearSetupsPlugin`.
- After finishing a task: tick it in build-plan.md, append one line to changelog.md.
- Keep doc nodes short and dense; new long-form reference material gets its own node, linked here.
- Never call the wiki API on the client thread; always async via OkHttp.
