# Wiki Gear Setups

A RuneLite plugin that imports recommended gear setups from the [OSRS Wiki](https://oldschool.runescape.wiki) and turns them into bank tag layouts — built from the gear you actually own.

Search for a boss or activity, pick a combat style, preview which recommended items you have (with automatic fallback to the best alternative you own), and import a ready-made bank tag tab.

## Features (v1)
- Search OSRS Wiki strategy pages from a side panel
- Parses the wiki's ranked equipment recommendations per slot
- Checks your bank (snapshotted when you open it), inventory, and equipment
- Picks the best item you own per slot; configurable handling of slots where you own nothing
- Writes a bank tag with a layout for one-click gear grabbing

## Development
Requires JDK 11+. Run a dev-mode client with:

```
./gradlew run
```

## License
BSD 2-Clause — see [LICENSE](LICENSE).
