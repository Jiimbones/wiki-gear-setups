# Build plan

Status legend: [ ] todo · [x] done · [~] in progress

## Phase 0 — Scaffold
- [x] Gradle project from runelite example-plugin template (build.gradle, settings.gradle)
- [x] Plugin skeleton: `WikiGearSetupsPlugin`, `WikiGearSetupsConfig`, test runner
- [x] runelite-plugin.properties, LICENSE (BSD-2), README, .gitignore, git init
- [ ] **Manual (user):** install JDK 11+ (e.g. Temurin) — nothing builds until then
- [ ] Gradle wrapper (run `gradle wrapper --gradle-version 8.5` once gradle available, or copy wrapper from runelite/example-plugin)
- [ ] Verify `gradlew build` and `gradlew run` (dev-mode client) succeed

## Phase 1 — Wiki fetch + parser
- [ ] `WikiClient`: MediaWiki API via OkHttp, custom User-Agent, response caching
- [ ] Page discovery: search API + `/Strategies` subpage probing
- [ ] `GearSetupParser`: recursive bracket-matching parse of `{{Recommended equipment}}` → `GearSetup` model (see wiki-format.md)
- [ ] Unit tests against saved wikitext fixtures: Zulrah, Vorkath, CoX, a slayer monster, a quest page (varied on purpose)
- [ ] Per-slot failure tolerance (bad slot ≠ dead page)

## Phase 2 — Item resolution
- [ ] `ItemResolver`: wiki item name → set of acceptable item IDs (`ItemManager` + `ItemVariationMapping`)
- [ ] Variant traps covered by tests: charges, ornament kits, barrows decay, imbues, poisons
- [ ] Unresolvable-name reporting (surface in preview, don't fail silently)

## Phase 3 — Ownership
- [ ] `BankStateCache`: snapshot bank `ItemContainer` on bank open, persist per profile
- [ ] Include equipped + inventory containers (always readable)
- [ ] `OwnershipResolver`: per slot, walk ranked alternatives, pick best owned (place the specific owned variant ID)
- [ ] Handle: no snapshot yet (prompt user), stale snapshot, missing-gear behavior per config

## Phase 4 — Bank tag layout output
- [ ] Verify current core BankTags API surface (`@PluginDependency`, `TagManager`, layout API) against runelite master — **do this before coding**
- [ ] `LayoutBuilder`: deterministic grid (equipment rows in slot order; alternatives below)
- [ ] Write tag + layout; re-import policy (overwrite same tag name)
- [ ] Fallback path: generate `banktags,1,...` import string if API unsuitable

## Phase 5 — UI + ship
- [ ] Side panel: search box → page pick → setup/style pick → preview (owned / substituted / missing) → import button
- [ ] Config UI polish; icon.png for hub listing
- [ ] README screenshots + hub submission (fork runelite/plugin-hub, add manifest)

## v1.1 (out of scope for v1)
- Suggested inventory (potions/runes/food) parsing → layout inventory section
