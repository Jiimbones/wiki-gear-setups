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

## Raid example setups (2026-07-20)
- [x] Scrapped the shelved best-per-style idea (branch deleted) in favour of the wiki's own example loadouts
- [x] `ExampleSetupParser`: wikitable idiom (! header columns; parallel {{Equipment}}/{{Inventory}}/{{Rune pouch}} rows; positional column pairing; template-depth-aware line scan — bare "|" cells with next-line templates; multi-line headers; |+ captions skipped). Section headings (h2/h3) → setup path
- [x] `ExampleSetup` model (path, label, gear, paired inventory); real fixtures toa-strategies + tob-setups-guide
- [x] Panel example mode: cascading Mode → Tier → Label combos, shown only when the page has that level (ToA/CoX = single dropdown, ToB = all three); paired inventory imported automatically, inventory combo hidden; tag name from cascade with page words scrubbed
- [x] PageDiscovery probes "Guide:<base> setups" first (ToB); displayTitle strips Guide:/" setups"
- [x] 10 new/updated tests; 119 total green
- [x] Personalization restored in example mode: `ExampleGearEnricher` — each concrete example item anchors into the page's ranked ladder for that slot (anchor first: owned→placed, unowned→goal; ladder = owned fallback, style-correct implicitly). Source combo ("Example setups"/"Gear by style") shown when a page has both formats; tabber pages (Zulrah/Vorkath) unaffected (classic only)
- [x] 4 new tests; 123 total green
- [x] Adaptive inventory (all modes): unowned inventory item → (1) gear-ladder fallback when the item anchors into any ranked slot list (switch items, style-correct), (2) `ConsumableGroups` class substitution (food tiers, prayer restores), else missing/goal as before. Divine potions satisfy base potion recs (one-directional, resolver-level)
- [x] 4 new tests; 127 total green
- [ ] User visual verification in dev client

## Backup / restore point (2026-07-21)
- [x] Tag `v1.0-backup-2026-07-21` + git bundle + full zip (incl. `.git`) in `Desktop\WikiGearSetups-backups\` with RESTORE.md. Verified by cloning the bundle and running the suite
- [x] Found via that verification: fresh clones failed `InventoryParserTest` — `.gitattributes` `* text=auto` rewrote fixtures to CRLF and the line-anchored tabber pattern captured `\r`. Fixed with `*.wikitext -text` **and** line-ending normalization at all three parser entry points (defence in depth); CRLF regression tests added

## Three-feature round (2026-07-21)
- [x] **Show wiki BIS mode** — `PickStrategy` (BEST_OWNED / TOP_RECOMMENDATION) on both resolvers; suspends only ownership-derived reasoning (set gating, set-worn fixpoint, inventory substitution, missing-gear config); checkbox drives preview **and** import; `-bis` tag suffix + relabelled button prevent overwriting the adaptive tab; extras suppressed. See decisions #7/#8
- [x] **Source auto-scoring** — `SourceScorer` (parse-time only: slot coverage, inventory/runes presence, ranked depth); winner preselected, labelled only when margin ≥ 2. Prerequisite: `ExampleSetupParser.withoutExampleInventories` stops example-owned inventories leaking into the ranked dropdown (they also poisoned the inventory signal)
- [x] **First-run disclaimer** — `DisclaimerPanel` on a `CardLayout` card outside the scroll pane (pinned header/footer behaviour untouched); gated on config key `disclaimerAccepted`; footer "Developed by Jiimbones" re-opens it; banner icon scaled once and shared
- [x] 15 new tests; 142 total green
- [ ] User visual verification in dev client

## Round 8 (2026-07-21)
- [x] Disclaimer copy: explains that pages list several options per slot so a layout can occasionally include an ill-fitting item (off-hand beside a 2h weapon) — no blame on the wiki or the plugin; bug line reduced to one sentence
- [x] **God capes** (investigated, two causes): (a) CoX/ToA write `{{plink|God capes#Imbuing|txt=…|pic=…}}` — a `#` now means the target is a page, so `txt=` becomes the name; `txt`/`pic` also kept as `RankedItem.fallbackNames`, tried only when the primary fails. (b) Every example setup names the saradomin cape, so `ItemAliases` gained bidirectional same-tier `equivalent()` groups: 3 imbued capes + 3 imbued max capes, and the 3 plain capes. Plain still never satisfies imbued
- [x] **Slayer checkbox**: `assumeSlayerTask` removed from config; contextual checkbox above the BIS toggle, shown only when the resolved setup has a task-conditional recommendation (`OwnershipResolver.hasSlayerConditional`), session-scoped, defaults off. Footnote rule unchanged
- [x] **Icon picker**: `IconPicker` (button + popup sprite grid, names in tooltips) replaces the named combo; `DropsParser` reads Uniques/Tertiary `{{DropsLine}}` rows from the base page (fetched only when the loaded page has no drop table), `WikiClient.petNames()` identifies the pet via `Category:Pets`, and the pet is preselected when found
- [x] 19 new tests; 166 total green
- [ ] User visual verification in dev client

## 39-page live sweep + fixes (2026-07-22)
Method: 39 untested `/Strategies` pages fetched through the plugin's own PageDiscovery order, run through the real parsers; all 793 extracted names checked against the wiki namespace; OwnershipResolver driven with five synthetic banks. Held up: 39/39 parsed, 0 failures, 38/40 shorthand searches correct, SourceScorer right on all 39.
- [x] **Rune pouch quantities** — `{{Rune pouch}}` writes `Fire rune\16000`; `InventoryParser.stripQuantity` drops it. Was 94 unresolvable values across 9 pages (Cerberus, Colosseum, Zilyana, Graardor, Giant Mole, Inferno, KQ, Kree'arra, Tempoross)
- [x] **Inventory labels** — pages head every inventory `==Inventory==`, so `labelFor` stepped over generic headings ("inventory"/"equipment"/"gear"/"setup"/"loadout"/"items") to the enclosing tabber pane label; PANE_LABEL also relaxed to cross blank lines after `|-|`. Duplicate-label pages 19 → 3
- [x] **`SetupPairing`** — pairs gear↔inventory by page order (interleaved `EIEI`, or blocked `EEEIII` with equal counts); count mismatch with the parsed models disables it rather than mispairing. Found on 18 pages; drives `autoSelectInventory` ahead of `InventoryMatcher`
- [x] **Duplicate labels numbered** — `uniquify` on both style and inventory labels ("Melee", "Melee (2)"). Was 6 tag-name collisions across 4 pages where the second import silently replaced the first tab
- [x] **Choice templates** — `{{MinPrice|format=item|link=n|Manta ray|Tuna potato|…}}` (17 uses on Phantom Muspah, 13 in one inventory) expands to its unnamed params, joined by `CHOICE_SEPARATOR` and split by `PlaceholderItems` so the existing owned-first path handles it
- [x] **Generic names** — `ItemAliases`: god d'hide × 6 slots × every spelling the pages use, blessings, halos, diary reward tiers (highest first), Wintertodt warm clothing; `ItemNames.normalize` maps `_` → space (`{{plink|Blessed_bracers}}`). All 85 concrete names verified to exist on the wiki
- [x] **Overwrite confirmation** — `BankTagImporter.existing()` returns NONE/OURS/FOREIGN (imported tags recorded in config); panel checks on the client thread, asks on the EDT, defaults to Cancel and warns harder for a tab the plugin didn't create
- [x] 32 new tests; 202 green. Fixtures: general-graardor-strategies, phantom-muspah-strategies
- [ ] User visual verification in dev client

### Left from the sweep (not done)
- [ ] No record of tabs the plugin created — no list, refresh, or remove; every return trip starts at the search box
- [ ] "Show wiki BIS" resets each session (maxed accounts re-tick it on every page)
- [ ] Big dropdowns are flat — Yama's 13 inventories mix per-style and per-mechanic loadouts with no grouping
- [ ] Search: "gwd" finds nothing; "nightmare" ranks Nightmare Zone above The Nightmare

## Live-test round (2026-07-22)
- [x] **`+` notation** — `precededByPlus` dropped every item after a plus; pages use a *spaced* plus for "and bring this too". Narrowed to the attached form (`+{{plinkp|Trouver parchment}}`). Colosseum Budget was losing Bow of faerdhinen, Sanguinesti staff, Venator bow, Crystal helm and Crystal body
- [x] **Plain-text conditions** — `parseItems` only read `{{...}}`, so "(on task)", "(if on task)", "(task only)" and `<ref>` bodies never became notes. `annotationAfter` captures balanced parentheticals and ref bodies between templates; separators ("/", "<br>") and the "(or:{{plinkp|…}}" opener still yield nothing. `isSlayerTaskConditional` also accepts the short phrasings (it required the word "slayer", which the parentheticals omit)
- [x] **Weapon set-requirements** — met by owning a listed weapon rather than it winning the slot: only one weapon is worn but a loadout swaps between several, so crystal armour survives a carried bowfa. Armour groups unchanged (blood moon gate intact)
- [x] 9 new tests; 211 green
- [ ] User re-verification in dev client

### Known, not fixed
- Inventory substitution can swap a crossbow for a bow (Muspah: unowned Zaryte crossbow → Twisted bow) when the wiki lists them in one ladder. Bolt-proc content makes the bow unsuitable, but the wiki's own ladder says otherwise and a name-based crossbow/bow filter would be crude — left alone deliberately
- A `+` switch loadout collapses into one slot's alternatives, so only one of "fang + bowfa + sang" is worn and the rest surface as owned alternates below the layout. Closest fit to a one-item-per-slot model

## Audit + heavy-page plan implemented (2026-07-23)
Sequenced per the dev plan; conflicts were measured before coding, not assumed. See decisions #12 (BIS fidelity).
- [x] **W1** off-task guards on slayer gating (3 guards; 4 corpus false positives removed; also makes BIS more wiki-faithful); 215 tests
- [x] **W2** resolution residue: `;n` marker, `Placeholder` blank, trailing-number + open-state candidates, `God blessing` alias (corpus unresolvable 14→3, rest are index gaps); 220 tests
- [x] **W3** thin annotation setups → extras (`GearSetups.partition`, ≥3-slot threshold in the empty band; CoX 15→3 etc.); scored/paired on the unfiltered list then remapped (conflicts 1 & 2); UIM-style all-thin pages left whole; 227 tests
- [x] **W4** pane-label fallback for weak `|style=` (`SectionLabels` shared, `GearSetup.paneLabel`); **changes tag names on 13 pages — pre-release only**; 233 tests
- [x] **W5** row-count on "All styles" (`LayoutBuilder.estimatedRows`); 233 tests
- [x] **W6** page-kind tag on search results (`PageKind`, title-only); 239 tests
- [x] **W7** wiki task wording in checkbox tooltip, label → "On this task"; 241 tests
- [ ] User visual verification in dev client (W3 changes what's tagged — check extras volume/layout height; W4 label changes; W6 result tags)

### Deferred (from the heavy-page study, not scheduled)
- [ ] F3 pane-scoped pairing (+13 pages) / F7 content-derived inventory labels — overlap; decide together after W3 settles how much the dropdowns still hurt
- [ ] Section-grouped inventory dropdown (Yama's 13) — the only real answer to inventory proliferation
- [ ] Gating gap: bare "Only on <monster> task" (no "slayer") — needs its own false-positive pass like W1

## Hub submission (2026-07-25)
- [x] Scroll-range trim under imported tabs (`LayoutScrollTrimmer`, config `trimEmptySpace`); guarded to `importedTags` and shrink-only; 257 tests
- [x] Artwork: banner 418×381 wordmark, 16×16 nav icon, root `icon.png` 48×48 for the hub listing (wordless art — the wordmark is illegible below ~100px)
- [x] Version stamp in the disclaimer footer (`WikiGearSetupsPlugin.VERSION`, hard-coded: `build=standard` replaces build.gradle at package time)
- [x] Manifest: `version=1.0`, `build=standard`, support URL
- [x] Identity scrub — all 68 commits rewritten to `Jiimbones <249501775+Jiimbones@users.noreply.github.com>`, `Co-Authored-By` trailers dropped, personal address removed from historical file contents (it was in `WikiClient.USER_AGENT`, i.e. on every outbound wiki request); `CLAUDE.md` untracked. Backup: `WikiGearSetups-backups/pre-history-rewrite-20260725-163355.zip`
- [x] Pushed to https://github.com/Jiimbones/wiki-gear-setups (`main`, 68 commits, 3 tags)
- [x] Clean-clone build from the published commit: 257 tests, Java 11 bytecode (major 55), resources packed
- [x] User visual verification in dev client — artwork, version stamp, "On Slayer task", scroll trim incl. drag-below-last-row, foreign tabs unaffected, fresh import (User-Agent change) all confirmed working
- [x] plugin-hub branch pushed: `Jiimbones/plugin-hub:wiki-gear-setups`, one file `plugins/wiki-gear-setups` pinning `6958fc13`
- [x] PR opened (#14281); merged 2026-08-21, pinning `e836b8d`

## 1.0.1 (2026-11-25)
Found by re-running the parsers over the live wiki — the fixtures were four months stale.
- [x] Search tags `bis`, `best in slot`, `loadout`, `boss`, `pvm`, `raids` (committed 2026-09-30; the update PR was never opened, so it ships here)
- [x] `2h#` two-handed column read into the weapon slot; same-rank merge instead of overwrite; `EquipSlot` alternate names
- [x] `{{Loadout}}` inventories + runes (Zulrah); `SetupPairing` shares `InventoryParser.isInventoryTemplate`
- [x] Skips made visible: `unrecognisedParams`, `unrankedSlotParams`, `unrecognisedInventoryTemplates`, `TemplateCoverageTest`, `./gradlew corpusCheck`
- [x] Fixtures: Doom of Mokhaiotl, Kree'arra, Zulrah (Loadout); 277 tests; RuneLite 1.13.8, no deprecation warnings
- [x] Version 1.0.1 in manifest, `build.gradle`, `WikiGearSetupsPlugin.VERSION`
- [ ] Not verified in a live client: dev login was removed for security (2026-07). Parser-level change; downstream resolver, pairing and layout code is unchanged
- [ ] User to open the plugin-hub update PR

## Next
- [ ] README screenshots (still the weakest part of the listing — the README describes the plugin without showing it)
- [ ] Before every release: `./gradlew corpusCheck`
- [ ] Wiki fix anyone can make: Thermonuclear smoke devil/Strategies `|weapon =` should be `|weapon2 =` (the page never shows the Ghrazi rapier)
- [ ] Auto-layout bank button (planned 2026-08; parked until after the first merge, now unblocked)
