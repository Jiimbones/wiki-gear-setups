# Architecture

## Flow
Panel UI → `WikiClient` (fetch wikitext) → `GearSetupParser` (→ `GearSetup`) → `ItemResolver` (names → ID sets) → `OwnershipResolver` (× `BankStateCache`) → `LayoutBuilder` → core BankTags write.

## Components
| Component | Responsibility | Key APIs / notes |
|---|---|---|
| `WikiClient` | MediaWiki API calls, async, cached | RuneLite-injected `OkHttpClient`; `api.php?action=parse&prop=wikitext`; custom User-Agent required by wiki policy |
| `GearSetupParser` | wikitext → `GearSetup` model | Hand-rolled bracket parser (hub whitelist forbids parser libs). See wiki-format.md |
| `ItemResolver` | wiki name → `Set<Integer>` acceptable item IDs | `ItemManager.search()`, `ItemVariationMapping` for variant canonicalization |
| `BankStateCache` | persisted per-profile bank snapshot | Bank only readable while open — snapshot on `ItemContainerChanged` (BANK); also read EQUIPMENT + INVENTORY (always available). Same approach as Bank Memory plugin |
| `OwnershipResolver` | per slot, walk ranks, pick best owned variant | Missing-slot behavior driven by config enum |
| `LayoutBuilder` | deterministic grid placement | Slot order top rows, alternatives below; stable across re-imports |
| BankTags integration | write tag + layout | Core BankTags plugin has native layouts; `@PluginDependency(BankTagsPlugin)`. Verify API surface before Phase 4. Fallback: clipboard import string `banktags,1,name,icon,layout,...` |
| Panel UI | search / pick / preview / import | Swing; all network off the client thread |

## Model
```
GearSetup { pageTitle, styleLabel, Map<EquipSlot, List<RankedItem>> }
RankedItem { wikiName, rank, Set<int> itemIds, note }
EquipSlot  { HEAD, CAPE, NECK, AMMO, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING, SPECIAL }
```

## Constraints
- Java 11 bytecode; deps limited to RuneLite-bundled (OkHttp, Gson, Guava, Lombok, JUnit4 test).
- Client thread = Swing thread discipline: game state reads on client thread, network async, UI updates via SwingUtilities.
