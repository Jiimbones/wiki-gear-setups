# Build plan

Status legend: [ ] todo · [x] done · [~] in progress

## Phase 0 — Scaffold
- [x] Gradle project from runelite example-plugin template (build.gradle, settings.gradle)
- [x] Plugin skeleton: `WikiGearSetupsPlugin`, `WikiGearSetupsConfig`, test runner
- [x] runelite-plugin.properties, LICENSE (BSD-2), README, .gitignore, git init
- [x] JDK: Temurin 17.0.19 installed via winget (`C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`; set JAVA_HOME when building)
- [x] Gradle wrapper 8.10 copied from runelite/example-plugin; gradlew exec bit set
- [x] `gradlew build` verified green
- [ ] `gradlew run` (dev-mode client) — deferred until there's UI to look at

## Phase 1 — Wiki fetch + parser
- [x] `WikiClient`: async (CompletableFuture over OkHttp enqueue), custom User-Agent, 15-min page cache; JSON extraction split into testable statics
- [~] Page discovery: search API done; `/Strategies` subpage probing heuristic still todo (wire into panel flow, Phase 5)
- [x] `GearSetupParser`: bracket-matching parse → `GearSetup`; uses template's own `|style=` param as label
- [x] 20 unit tests green: 5 frozen fixtures (see wiki-format.md fixture inventory) + synthetic edge cases (efn variants, pic=, N/A, unclosed template, nesting)
- [x] Per-slot failure tolerance (try/catch per param, log + skip)
- [ ] Transclusion following ({{/Subpage}}) — not needed by any fixture; defer until a real page requires it

## Phase 2 — Item resolution
- [x] `ClientItemNameIndex`: full item-definition scan (chunked 4000/frame on client thread, skips noted+placeholders), published as immutable maps; built on LOGGED_IN. NOT ItemManager.search (that only covers tradeables — misses void, imbues, barrows states)
- [x] `ItemResolver`: name → primary id + acceptable id set via ItemVariationMapping, **name-filtered** because variation groups merge upgrade tiers (black mask group holds plain AND imbued). Rules: stripped-charge equality / startsWith (upgrades) / endsWith (recolors). Lower tier never satisfies higher-tier rec.
- [x] 9 tests using real item ids against real item_variations.json: barrows decay, charged-imbued vs plain, recolors, LMS same-name dupes
- [ ] Unresolvable-name surfacing → preview panel (Phase 5)

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
