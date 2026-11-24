# OSRS Wiki gear setup format

Source: strategy pages (usually `<Boss>/Strategies`), template `{{Recommended equipment}}`.

## Verified structure (Zulrah/Strategies, 2026-07)
```
|head1 = {{plink|Slayer helmet (i)}}{{efn|If on a Zulrah slayer boss task}} / {{plink|Ancestral hat}}
|head2 = {{plink|Virtus mask}}
|neck1 = {{plink|Occult necklace}}
```
- Param pattern: `<slot><rank>` — slot ∈ head/cape/neck/ammo/weapon/body/shield/legs/hands/feet/ring/special/**2h**, rank 1..5 (1 = best). Verified against the template's own TemplateData (2026-11): those 13 slots × ranks 1–5, plus `style` and `bucket`, are the whole documented set.
- **`2h#` is the two-handed column** (since the Dec 2022 rework). It fills the weapon slot: `weapon1` and `2h1` are both rank-one weapons, merged in page order (page order breaks the tie). Whether a shield fits is decided later from the item's own stats, not the column. Pages leaning on it: Doom of Mokhaiotl, Kree'arra, Maggot King, UIM guide.
- A slot name with **no rank** (`|weapon = ...`) is never shown: `Module:Recommended equipment` only reads `args[slot .. i]` for i = 1..5. Skip it — importing it would put on the tab what the page doesn't show. Reported by `unrankedSlotParams` (seen: Thermonuclear smoke devil, a typo for `weapon2`).
- Layout-only params, never gear: `style`, `bucket`, `hidecol` (also numbered — `hidecol5 = yes` hides rank column 5), `align`, `rows`, `class`, `buttons`.
- Alternate slot names accepted: `torso`→body, `gloves`→hands, `boots`→feet (the `{{Loadout}}`/`{{Equipment}}` spellings; a few pages carry them on this template).
- **`|style = Magic`** — the template names its own combat style. Use it as the setup label (fallback: "Setup N"). Observed: "Magic", "Ranged", "Magic (Diary kill)".
- Item refs: `{{plink|Item name}}` (also `{{plinkp}}`, `{{plinkt}}`); may carry named params (`pic=`, `txt=`) — item name = first *positional* param, **except** when it contains `#` (a page section link, e.g. `God capes#Imbuing` on CoX/ToA) — then `txt=` is the item name. `txt=`/`pic=` are kept as `RankedItem.fallbackNames` and tried only if the primary name doesn't resolve. Extract ONLY plink-family targets; bare `[[links]]` at top level are prose, not items — skip them (false-positive risk outweighs coverage).
- Same-rank separators observed: ` / `, ` or <br> `, ` > ` (preference order). No need to parse them — collect plinks in appearance order.
- `{{efn|text|name=x}}` = footnote → attach as `note` to preceding item. Variants: `{{efn|name=x}}` / `{{efn||name=x}}` are back-references with no text — ignore. Note text may contain `[[links]]` (strip to display text) and nested templates (drop).
- `N/A` or empty param → slot intentionally empty at that rank.

## Example-setups idiom (raid pages, verified 2026-07-20)
Complete concrete loadouts in a wikitable: `!` header cells label columns; parallel rows hold `{{Equipment}}` (params head/cape/neck/ammo/weapon/torso|body/shield/legs/gloves|hands/boots|feet/ring), `{{Inventory}}`, `{{Rune pouch}}` per column. Column pairing is positional across rows.
- ToA/CoX: one table, tier columns ("Minimum (0-100 Invocation)" — header labels continue on following lines).
- ToB: `Guide:Theatre of Blood setups` — h2 mode sections (Normal/Hard), h3 tier subsections, role columns (Magic/Melee/Range). Section headings become the setup path → cascading UI dropdowns.
- Cells may be a bare `|` line with the template starting on the next line — table markup is only meaningful at template depth 0.
- `ExampleSetupParser`; discovery probes `Guide:<base> setups` before other candidates (missing guide = fast API error).

## `{{Loadout}}` (verified 2026-11-25)
One template holding gear, a 28-slot inventory and a rune pouch: `loadoutname`, `loadouticon`, gear (`head cape neck ammo ammo2 weapon shield torso legs gloves boots ring`), inventory `1`–`28` (numbered exactly like `{{Inventory}}`), runes `rune1`–`rune4`, `spell1`–`spell5`. Created Oct 2024; on only 3 mainspace pages (vs ~280 for `{{Inventory}}`), but **Zulrah/Strategies** moved its five inventories into it between July and November 2026 and they silently stopped importing.
- `InventoryParser` reads the grid and runes; **gear is not read** — the `{{Equipment}}` panels it replaced were never read either, and the page's `{{Recommended equipment}}` is where the gear comes from.
- Label: the page's own pane/heading label first, then `loadoutname`, then "Inventory N". On Zulrah the panes are unchanged since July, so labels and runes match the old `{{Inventory}}` version exactly.
- `SetupPairing` counts it as an inventory via `InventoryParser.isInventoryTemplate` — one rule, so the two counts can't drift and silently disable pairing.

## Drop tables (tab icons, verified 2026-07-20)
`{{DropsLine|name=X|quantity=1|rarity=1/1024|...}}` rows grouped under `===` section headings on the **base** page (not `/Strategies`). Only "Uniques" and "Tertiary" are read — the rest is runes/herbs/seeds noise. Pets have no naming pattern (Olmlet, Lil' zik, Vorki), so they're identified against `Category:Pets` via `list=categorymembers` (one memoised call per session; failure just means no pet default). `DropsParser`; base title derived by `WikiGearSetupsPanel.basePageTitle`.

## Fixture inventory (frozen 2026-07-19, src/test/resources/fixtures/)
| fixture | templates | notes |
|---|---|---|
| zulrah-strategies | 4 | tabber, style param, efn variants, `or <br>`/`>` separators, `pic=` |
| vorkath-strategies | 2 | |
| cox-strategies | 15 | stress test |
| alchemical-hydra-strategies | 2 | slayer boss |
| dragon-slayer-ii | 0 | negative path (quest page, no template) |
| toa-strategies | 5+ example columns | example-setups wikitable, multi-line headers, separate pouch row |
| tob-setups-guide | 12+ role columns | mode/tier/role hierarchy (frozen 2026-07-20) |
| zulrah-page | 49 DropsLine rows | base page drop table: Uniques + Tertiary/pet (frozen 2026-07-21) |
| doom-of-mokhaiotl-strategies | 3 | `2h#` column; Ranged and Melee punish are 2h-only (frozen 2026-11-25) |
| kreearra-strategies | 5 | `2h#`; Magic (Tumeken's shadow) and "Tbow / Bowfa" are 2h-only (frozen 2026-11-25) |
| zulrah-strategies-loadout | 4 + 5 loadouts | same page as `zulrah-strategies` after its `{{Loadout}}` conversion (frozen 2026-11-25) |

## Parsing rules
1. Locate `{{Recommended equipment` occurrences via bracket-depth scan (NOT regex-only — templates nest).
2. A page may contain several instances (Melee/Ranged/Magic tabs, tabber markup) — return all, each with a style label inferred from surrounding headers/tabber titles.
3. Setups may be transcluded from subpages — if a section transcludes (`{{/Subpage}}` or `{{:Page}}`), fetch and parse the transclusion target.
4. Per-slot error tolerance: a malformed slot value logs + skips that slot, never aborts the page.
5. Wiki template may evolve — parser failures must degrade to a user-visible "couldn't parse" state, never a crash.
6. **Tolerant must not mean silent.** Both 1.0.1 bugs (`2h#`, `{{Loadout}}`) were template features skipped without a word. `GearSetupParser.unrecognisedParams` and `InventoryParser.unrecognisedInventoryTemplates` report what a page would lose; `TemplateCoverageTest` fails if any fixture has one; `./gradlew corpusCheck` runs the same check over every live page the plugin can open — run it before each release.

## API
- Wikitext: `https://oldschool.runescape.wiki/api.php?action=parse&page=<title>&prop=wikitext&format=json`
- Search: `api.php?action=opensearch` / `action=query&list=search`
- Policy: descriptive User-Agent header mandatory; cache responses (15 min+); low request volume.
