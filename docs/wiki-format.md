# OSRS Wiki gear setup format

Source: strategy pages (usually `<Boss>/Strategies`), template `{{Recommended equipment}}`.

## Verified structure (Zulrah/Strategies, 2026-07)
```
|head1 = {{plink|Slayer helmet (i)}}{{efn|If on a Zulrah slayer boss task}} / {{plink|Ancestral hat}}
|head2 = {{plink|Virtus mask}}
|neck1 = {{plink|Occult necklace}}
```
- Param pattern: `<slot><rank>` — slot ∈ head/cape/neck/ammo/weapon/body/shield/legs/hands/feet/ring/special, rank 1..N (1 = best).
- **`|style = Magic`** — the template names its own combat style. Use it as the setup label (fallback: "Setup N"). Observed: "Magic", "Ranged", "Magic (Diary kill)".
- Item refs: `{{plink|Item name}}` (also `{{plinkp}}`, `{{plinkt}}`); may carry named params (`pic=`, `txt=`) — item name = first *positional* param. Extract ONLY plink-family targets; bare `[[links]]` at top level are prose, not items — skip them (false-positive risk outweighs coverage).
- Same-rank separators observed: ` / `, ` or <br> `, ` > ` (preference order). No need to parse them — collect plinks in appearance order.
- `{{efn|text|name=x}}` = footnote → attach as `note` to preceding item. Variants: `{{efn|name=x}}` / `{{efn||name=x}}` are back-references with no text — ignore. Note text may contain `[[links]]` (strip to display text) and nested templates (drop).
- `N/A` or empty param → slot intentionally empty at that rank.

## Fixture inventory (frozen 2026-07-19, src/test/resources/fixtures/)
| fixture | templates | notes |
|---|---|---|
| zulrah-strategies | 4 | tabber, style param, efn variants, `or <br>`/`>` separators, `pic=` |
| vorkath-strategies | 2 | |
| cox-strategies | 15 | stress test |
| alchemical-hydra-strategies | 2 | slayer boss |
| dragon-slayer-ii | 0 | negative path (quest page, no template) |

## Parsing rules
1. Locate `{{Recommended equipment` occurrences via bracket-depth scan (NOT regex-only — templates nest).
2. A page may contain several instances (Melee/Ranged/Magic tabs, tabber markup) — return all, each with a style label inferred from surrounding headers/tabber titles.
3. Setups may be transcluded from subpages — if a section transcludes (`{{/Subpage}}` or `{{:Page}}`), fetch and parse the transclusion target.
4. Per-slot error tolerance: a malformed slot value logs + skips that slot, never aborts the page.
5. Wiki template may evolve — parser failures must degrade to a user-visible "couldn't parse" state, never a crash.

## API
- Wikitext: `https://oldschool.runescape.wiki/api.php?action=parse&page=<title>&prop=wikitext&format=json`
- Search: `api.php?action=opensearch` / `action=query&list=search`
- Policy: descriptive User-Agent header mandatory; cache responses (15 min+); low request volume.
