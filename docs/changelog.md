# Changelog

## Unreleased
- 2026-07-19: Phase 2 — `items` package (ItemNames, ItemNameIndex, ClientItemNameIndex, ItemResolver, ResolvedItem); tier-aware variation filtering; plugin builds index on login; 29 tests total green. `.gitattributes` added.
- 2026-07-19: Phase 1 core — `wiki` package (EquipSlot, RankedItem, GearSetup, GearSetupParser, WikiClient, SearchResult); 5 frozen wikitext fixtures; 20 tests green. Discovered `|style=` template param (better than tabber-label inference).
- 2026-07-19: Toolchain — Temurin 17.0.19 via winget, Gradle 8.10 wrapper from example-plugin, first `gradlew build` green.
- 2026-07-19: Phase 0 scaffold — gradle project, plugin/config skeleton, docs nodes, git init. Pending: JDK install, gradle wrapper, first build.
