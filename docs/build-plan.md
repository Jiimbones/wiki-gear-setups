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
- [x] `BankStateCache`: snapshot on bank `ItemContainerChanged`, persisted via RS-profile-scoped config (Gson map); reloaded on `RuneScapeProfileChanged` (handles alts/leagues); skips qty-0 placeholder rows
- [x] Live inventory + equipment merged in `ownedItemIds()` — client thread only (gameval `InventoryID.INV`/`WORN`/`BANK`; old enum is deprecated)
- [x] `OwnershipResolver`: pure; walks rank-ascending alternatives, first satisfiable wins; places the specific owned variant id (exact match beats variant); goal-item path per `MissingGearBehavior`; unresolved names recorded per slot
- [x] 10 tests (8 ownership + 2 snapshot serialization); 39 total green
- [ ] "No snapshot yet — open your bank" prompt → panel (Phase 5); `hasSnapshot()` ready

## Phase 4 — Bank tag layout output
- [x] API verified against runelite master: `TagManager` (addTag/removeTag, public @Singleton), `LayoutManager` (saveLayout/loadLayout/removeLayout), `TabManager` (add/find/save), `TagTab` (@Data, public no-args ctor + setters; 2-arg ctor package-private), `Layout(String tag)` + setItemAtPos. `BankTagsService` is read/open-only — writes go via managers.
- [x] `@PluginDependency(BankTagsPlugin.class)` added; importer refuses to run if Bank Tags disabled (TabManager.save() on an unloaded list would wipe the user's real tabs)
- [x] `TagNames`: deterministic page+style slug ("zulrah-magic-diary-kill"), ≤50 chars
- [x] `LayoutBuilder`: pure; packs one item per slot in EquipSlot order, deterministic across input order
- [x] `BankTagImporter`: clears old tag → tags picks (variation=false; we picked exact ids deliberately) → saves layout → creates/updates tab (icon = weapon pick, else first pick). Client thread only.
- [x] 10 new tests (TagNames, LayoutBuilder, iconFor); 49 total green
- [ ] Runtime verification in dev client — blocked on Phase 5 UI to trigger an import
- [x] ~~Fallback import string~~ dropped: manager API sufficient

## Phase 5 — UI + ship
- [ ] Side panel: search box → page pick → setup/style pick → preview (owned / substituted / missing) → import button
- [ ] Config UI polish; icon.png for hub listing
- [ ] README screenshots + hub submission (fork runelite/plugin-hub, add manifest)

## v1.1 (out of scope for v1)
- Suggested inventory (potions/runes/food) parsing → layout inventory section
