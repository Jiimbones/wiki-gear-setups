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
- [x] `PageDiscovery`: candidate order [explicit subpage] → base/Strategies → base; tried until one parses non-empty
- [x] `WikiGearSetupsPanel`: search → results → setup combo → preview (green owned / yellow alt / red goal / gray empty+unrecognized, summary counts, "open your bank" hint) → import. Threading: OkHttp async → parse off-EDT → ownership on client thread → UI on EDT
- [x] NavigationButton + generated 16x16 tag icon (`resources/com/wikigearsetups/icon.png`)
- [x] Removed unused maxAlternativesPerSlot config (hub-review noise)
- [x] Runtime verification 2026-07-19: live Jagex account in dev client (credentials via `--insecure-write-credentials` flow, RuneLite.exe at G:\Runelite\RuneLite). Bank snapshot 883 items; imports verified on ToA, Hespori, Vorkath, GotR, Duke Sucellus, Zulrah ×3 styles; re-import overwrite exercised. No plugin errors.
- [ ] README screenshots + hub submission (fork runelite/plugin-hub, add manifest; icon.png for listing)

## Post-verification feedback (2026-07-19, user tested live)
- [x] Slayer-task-conditional items: efn note matching "slayer"+"task" → skipped unless `assumeSlayerTask` config (default off). Applies to ownership AND goal fallback. Informational notes unaffected.
- [x] `LayoutStyle` config (default EQUIPMENT_SCREEN): mirrors worn-equipment interface in bank grid cols 0-3 (head@1; cape/neck/ammo@8-10; weapon/body/shield/special@16-19; legs@25; hands/feet/ring@32-34); cols 4-7 reserved for v1.1 inventory. COMPACT = old packed rows.
- [ ] User to visually verify equipment-screen layout + slayer skip in dev client

## v1.1 (2026-07-19)
- [x] Search filter: `hastemplate:"Recommended equipment"` CirrusSearch keyword — status/lore pages (e.g. "Venom") never appear; results display with "/Strategies" trimmed
- [x] `InventoryParser`: `{{Inventory}}` templates (28 numbered slots, plain names); labels from nearest tabber pane/heading backscan; placeholder templates ({{Cheap food}}) surface as their name → land in "unrecognized". {{Rune pouch}} and {{Equipment}} example-loadout templates deliberately ignored
- [x] `InventoryResolver`: per-slot owned/missing/unrecognized (no ranks); dupes within inventory kept (wiki repeats food slots deliberately)
- [x] Multi-style content (ToA/CoX/ToB): "All styles (one tab)" combo option — one tab, one 5-row equipment block per style stacked vertically, gear shared across styles deduped, tag "<page>-all"
- [x] `LayoutBuilder` multi-block: inventory maps to grid cols 4-7 rows 0-6 in wiki order; inventory item already placed as gear (a switch) skipped; COMPACT = blocks as rows with blank-row separators
- [x] Inventory combo in panel ("No inventory" default) + inventory preview summary
- [x] 15 new tests; 72 total green
- [x] User visual verification of inventory + all-styles import (2026-07-19, screenshots)

## Feedback round 2 (2026-07-19)
- [x] GotR-style inventories fixed: {{Inventory}} accepts POSITIONAL params ("|Giant pouch |Large pouch |") counting empty cells, plus numbered; tabber pane labels may be same-line ("|-|Entry-level="). gotr-strategies fixture added (4 inventories)
- [x] {{Rune pouch}} parsing: attaches to preceding inventory; runes placed directly under the last equipment block (row blocks*5, cols 0-3); resolved for ownership like inventory slots
- [x] Default missing behavior → SHOW_BEST_UNOWNED (recommended items as placeholders, gear + inventory). NOTE: user's existing profile stored OMIT — flip "Missing gear" in config once
- [x] Inventory auto-select: single inventory → selected; multiple → label⊃style match; manual pick sticks per page
- [x] Editable tag name + tab icon item fields, prefilled with recommendation, user edits respected until page/style change; custom tag sanitized via TagNames.sanitize
- [x] 6 new tests; 78 total green

## Feedback round 3 (2026-07-19)
- [x] Empty-slot root causes fixed: (a) one-time config migration OMIT→SHOW_BEST_UNOWNED (marker `migratedShowBestDefault` — user's stored old default was suppressing placeholders); (b) `PlaceholderItems`: {{Cheap food}}/{{High healing food}}/{{Cheap prayer}} → concrete candidate lists, first-owned wins, goal fallback; (c) ItemResolver retries without trailing parenthetical for wiki disambiguators ("Teleport to house (tablet)")
- [x] Stale tag name fixed: explicit user-edit tracking (DocumentListener + focus) instead of text-equality heuristic; resets on page/style/inventory change
- [x] Tab icon: editable dropdown of picked items (auto = weapon) instead of free-text
- [x] "Open wiki page" button (LinkBrowser)
- [x] Sidebar grid preview: item-icon grid mirroring the exact bank layout (ItemManager AsyncBufferedImage, scaled 26x24, unowned rendered dimmed/disabled). Known limits: icons load async (pop-in), 8 cols squeezed into 225px panel, no quantities shown
- [x] 7 new tests; 82 total green

## Feedback round 4 (2026-07-19)
- [x] `ItemAliases`: generic wiki names → concrete items ("Imbued god cape" → the 3 capes + imbued max capes; "God cape"; "God book") and cross-group upgrade acceptance ("Rune pouch" ← Divine rune pouch + (l) variants)
- [x] ItemResolver fallback chain widened: quantity markers ("2 x Shark", "Shark x28"), potion dose retry (name+"(4)": "Stamina potion" → "Stamina potion(4)"), plural retry ("Sharks") — biggest unrecognized-item sources
- [x] Two-handed weapon drops the shield pick: `OwnershipResolver.dropShieldForTwoHanded` + `ItemStats.getEquipment().isTwoHanded()` via ItemManager (unknown stats = keep shield)
- [x] Extras below layout: `OwnershipResolver.ownedAlternatives` (owned, unpicked wiki alternates in table order) → LayoutBuilder places them after a blank row under max(gear+runes, inventory); tagged + imported too
- [x] Inventory auto-select upgraded to combat-style tokens (`StyleTokens`: mage/magic, range/ranged/ranging, melee) with substring fallback
- [x] Tab icon picker renders item sprites (IconOption + DefaultListCellRenderer, non-editable, repaint on async load)
- [x] Panel polish: banner slot (resources/com/wikigearsetups/banner.png, graceful when absent — **user must drop the logo file in**), item sprites on preview rows, styled search/import buttons, footer "Developed by Jiimbones" + Donate button (placeholder URL)
- [x] runelite-plugin.properties author=Jiimbones
- [x] 14 new tests; 96 total green

## Feedback round 5 (2026-07-19)
- [x] Logo bundled: G:\Downloads\wikibanktagslogo.png → resources banner.png (418x279, 111KB, transparent bg, HiDPI 2x of panel width)
- [x] UI overhaul: PluginPanel(false) + BorderLayout — header (logo/search/status) pinned top, middle content in its own JScrollPane (height-resize safe), footer (dev + donate) pinned to true bottom; `RoundedButton` (custom paint, hover/press, accent variant for Import); dark-styled text fields/combos/results list; tooltips
- [x] Normalize kills space-before-paren both sides ("Salve amulet(i)" == "Salve amulet (i)") — whole-class fix for game/wiki naming divergence
- [x] Upgrade chains: blowpipe (empty/Blazing), assembler lineage (accumulator→assembler→masori/Dizana's quiver tiers, transitive), tridents ↔ uncharged names, toxic staff, black mask→slayer helm (tier-safe), void→elite, salve amulet tiers; trouver "(l)" direct-name lookup fixed for normalize
- [x] 5 new tests; 101 total green
- [x] UI fixes after screenshots: SlimScrollBarUI (8px, no arrows), Scrollable viewport-width wrapper (combo clipping), FlatTextField search+tag with forced white inner text, rounded ✕ clear button, DarkListRenderer for list/combo popups

## Set-bonus gating (2026-07-20)
- [x] `ItemSets`: piece → requirement (any-of groups per other slot + optional paired weapons). always-gated: void (worthless setless), crystal helm/body/legs (need bowfa/crystal bow). note-gated (footnote contains "full"/"complete"+"set"): barrows ×6, justiciar, obsidian, moon sets ×3 (armour + paired weapon both directions)
- [x] OwnershipResolver.setSatisfied: gated candidate skipped → next-bis alternative wins (Amoxliatl blood moon helm case); per-recommendation via note so rank-3 standalone Dual macuahuitl stays ungated while rank-2 flagged one is gated; extras gated too
- [x] Fully-gated slot + SHOW → top piece still shown as goal (bestAnyResolvable fallback)
- [x] 6 new tests; 107 total green
- [x] User feedback: still saw blood moon helm (owns the set) → gate upgraded from owned to WORN-in-layout: PieceGroup carries its EquipSlot; requirement met per group iff piece is the slot's owned pick, or slot has no owned pick and piece is owned; fixpoint re-resolution with per-slot exclusions; parser test for named-efn note ({{efn|...|name=X}})
- [x] 3 new tests; 110 total green

## Feedback round 6 (2026-07-20)
- [x] Trouver parchment fix: "+{{plinkp|X}}" companion notation skipped in gear cells (precededByPlus) + "Trouver parchment" name-ignored; Fighter torso resolves normally, fallthrough to next rank works (Calvar'ion)
- [x] Ring of suffering upgrade chain: (ri) satisfies plain/(r)/(i); plain never satisfies (i)
- [x] InventoryResolution.missingNames (deduped wiki names) — panel shows "— Missing items —" red list separate from inventory summary
- [x] Panel: Name/Icon/Import moved directly under the grid (labels shortened); detail list after; donation link → real PayPal URL
- [x] 3 new tests; 113 total green
- [x] Report-bug button (GitHub issue, prefilled page title/body), wiki resilience (15s timeout, stale-cache fallback, friendly errors), support= property
- [x] User verified rounds 5-6 in dev client

## Shelved
- Raid loadouts ("best per style" one-tab option): built, reverted after user testing. Full implementation preserved on branch `shelf/raid-loadouts` (commit ccf4638) for a future update.

## Next
- [ ] GitHub repo creation + push (user to create repo), then hub submission prep (README screenshots, plugin-hub manifest PR)
