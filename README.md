# Wiki Gear Setups

A RuneLite plugin that imports recommended gear setups from the [OSRS Wiki](https://oldschool.runescape.wiki) and turns them into Bank Tags layouts — personalised to the gear you actually own.

Search for a boss or activity, pick a combat style, and the plugin reads the wiki's ranked equipment recommendations, works out the best item you own for each slot (falling back through the alternatives when you don't own the top pick), and writes a ready-to-use bank tag tab. One click in your bank grabs the whole setup.

## What it does

- **Searches** OSRS Wiki strategy pages from the side panel. Typing a shorthand (`zammy`, `arma`, `dks`, `thermy`) usually lands the right page first, and results are tagged by kind (`· Slayer task`, `· Guide`).
- **Parses** the wiki's `{{Recommended equipment}}` tables, `{{Inventory}}` templates and rune pouches, per slot and per combat style.
- **Adapts to your bank.** For each slot it picks the best item you own from the wiki's ranked list; unowned inventory switches fall back through the gear ladders, and unowned consumables substitute the best of their class you hold.
- **Handles item variants** so an owned upgrade counts: charged/imbued states, ornament kits `(or)`, gilded/trimmed/heraldic trims, Echo recolours, dyes (Sanguine, Shadow/Blood/Ice), interchangeable god capes, and more. Ownership resolves through RuneLite's own item variation mapping plus a curated set for the cases it doesn't cover.
- **Set-bonus aware.** A piece that's only best as part of a worn set (blood moon, void, crystal…) falls through to the next best alternative when the rest of the set isn't in the layout.
- **Raids** (CoX, ToB, ToA) read the wiki's own example loadout tables into cascading Mode → Tier → Role dropdowns with the matching inventory paired automatically.
- **Two views:** the adaptive setup built from your bank, or **Show wiki BIS** — the wiki's ideal gear ignoring what you own, for planning upgrades.
- **Safe imports.** Re-importing asks before it replaces a tab, and warns harder if the tab wasn't one the plugin created.

## Configuration

| Option | Default | What it does |
|---|---|---|
| Unowned recommendations | Show as a goal | For a slot you own nothing for: show the wiki's pick as a goal placeholder, or hide it and keep only gear you own |
| Layout shape | Equipment screen | Arrange the tab like the worn-equipment interface, or as packed rows |
| Start in Wiki BIS view | Off | Open each page with "Show wiki BIS" already on |
| Confirm before overwriting | On | Ask before an import replaces an existing tab |
| Extra owned gear | On | Place owned alternatives and situational switches below the main layout |
| Tag name prefix | *(blank)* | Prepend a slug (e.g. `wgs`) to every created tab so they group together |

## Requires

The core **Bank Tags** plugin (bundled with RuneLite) must be enabled — this plugin writes through it, and refuses to import if it's disabled rather than risk your tabs.

## Your data stays yours

Your bank, inventory and equipment are read **locally only**. The bank snapshot is stored in your own per-profile RuneLite config and is never sent anywhere. The only network traffic is read-only requests to the public OSRS Wiki API (with a descriptive User-Agent, as the wiki's policy asks), plus links you click that open in your browser (the wiki page, a bug report, the donate link). No telemetry, no third-party services.

## Account types

Ownership is scoped **per RuneScape profile**, and the item index is a full item-definition scan (not a tradeables-only search), so the plugin works across:

- Main, Ironman, Hardcore
- Alts and multiple accounts (each profile's bank is kept separate)
- Free-to-play

A fresh account that hasn't opened its bank yet is prompted to do so once, rather than guessing.

**Known limitation — Group Ironman:** only your *personal* bank, inventory and worn gear are counted as owned. Gear held in shared **group storage** is not read, so it will show as unowned.

## Development

Requires JDK 11+.

```
./gradlew test    # run the test suite
./gradlew run     # launch a dev-mode client with the plugin loaded
```

## License

BSD 2-Clause — see [LICENSE](LICENSE).
