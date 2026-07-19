# Changelog

## Unreleased
- 2026-07-19: Feedback round 3 — placeholder item mapping (Cheap food etc.), wiki-disambiguator name fallback, OMIT→SHOW config migration, sidebar item-grid layout preview, icon dropdown, wiki page link, robust tag-name auto-refresh; 82 tests green.
- 2026-07-19: Feedback round 2 — positional {{Inventory}} params (fixes GotR + similar pages), {{Rune pouch}} → runes under equipment block, SHOW_BEST_UNOWNED default, inventory auto-select, editable tag name/icon fields; 78 tests green.
- 2026-07-19: v1.1 — inventory import ({{Inventory}} parsing → grid right half), hastemplate search filtering (no more "Venom"), "All styles" combined import for multi-style content; importer API now takes setup lists + optional inventory; 72 tests green.
- 2026-07-19: Feedback round — slayer-task-conditional skipping (config `assumeSlayerTask`, default off) and equipment-screen layout shape (config `layoutStyle`, default EQUIPMENT_SCREEN, cols 4-7 reserved for inventory); 57 tests green.
- 2026-07-19: Phase 5 — `ui` package (WikiGearSetupsPanel), PageDiscovery, sidebar NavigationButton + icon; dropped unused config item; 52 tests green.
- 2026-07-19: Phase 4 — `layout` package (TagNames, LayoutBuilder, BankTagImporter); writes through core BankTags managers (TagManager/LayoutManager/TabManager); @PluginDependency on BankTagsPlugin; import gated on Bank Tags enabled; 49 tests green.
- 2026-07-19: Phase 3 — `bank` package (BankStateCache, OwnershipResolver, SlotPick, SetupResolution); per-RS-profile bank snapshots; migrated to gameval InventoryID (old enum deprecated); FakeItemNameIndex extracted for test reuse; 39 tests green.
- 2026-07-19: Phase 2 — `items` package (ItemNames, ItemNameIndex, ClientItemNameIndex, ItemResolver, ResolvedItem); tier-aware variation filtering; plugin builds index on login; 29 tests total green. `.gitattributes` added.
- 2026-07-19: Phase 1 core — `wiki` package (EquipSlot, RankedItem, GearSetup, GearSetupParser, WikiClient, SearchResult); 5 frozen wikitext fixtures; 20 tests green. Discovered `|style=` template param (better than tabber-label inference).
- 2026-07-19: Toolchain — Temurin 17.0.19 via winget, Gradle 8.10 wrapper from example-plugin, first `gradlew build` green.
- 2026-07-19: Phase 0 scaffold — gradle project, plugin/config skeleton, docs nodes, git init. Pending: JDK install, gradle wrapper, first build.
