# Changelog

## Unreleased
- 2026-07-19: Phase 4 — `layout` package (TagNames, LayoutBuilder, BankTagImporter); writes through core BankTags managers (TagManager/LayoutManager/TabManager); @PluginDependency on BankTagsPlugin; import gated on Bank Tags enabled; 49 tests green.
- 2026-07-19: Phase 3 — `bank` package (BankStateCache, OwnershipResolver, SlotPick, SetupResolution); per-RS-profile bank snapshots; migrated to gameval InventoryID (old enum deprecated); FakeItemNameIndex extracted for test reuse; 39 tests green.
- 2026-07-19: Phase 2 — `items` package (ItemNames, ItemNameIndex, ClientItemNameIndex, ItemResolver, ResolvedItem); tier-aware variation filtering; plugin builds index on login; 29 tests total green. `.gitattributes` added.
- 2026-07-19: Phase 1 core — `wiki` package (EquipSlot, RankedItem, GearSetup, GearSetupParser, WikiClient, SearchResult); 5 frozen wikitext fixtures; 20 tests green. Discovered `|style=` template param (better than tabber-label inference).
- 2026-07-19: Toolchain — Temurin 17.0.19 via winget, Gradle 8.10 wrapper from example-plugin, first `gradlew build` green.
- 2026-07-19: Phase 0 scaffold — gradle project, plugin/config skeleton, docs nodes, git init. Pending: JDK install, gradle wrapper, first build.
