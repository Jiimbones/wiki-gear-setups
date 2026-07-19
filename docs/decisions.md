# Decisions (locked — don't re-litigate)

| # | Date | Decision | Why |
|---|---|---|---|
| 1 | 2026-07-19 | Distribution: RuneLite Plugin Hub | User choice. Implies: BSD-2 license, dependency whitelist, code-review-defensive quality |
| 2 | 2026-07-19 | Missing gear behavior: configurable enum (`OMIT` default / `SHOW_BEST_UNOWNED`) | User choice; OMIT = "best owned only" default |
| 3 | 2026-07-19 | v1 scope: equipment slots only; suggested inventory deferred to v1.1 | User choice; halves parser surface for v1 |
| 4 | 2026-07-19 | Pure client-side plugin, no backend; parse wikitext in-plugin | Bank read requires in-process; hub disallows heavy deps; MediaWiki API is open |
| 5 | 2026-07-19 | Output via core BankTags layouts API, clipboard import string as fallback | Native tab behavior; API surface to be verified in Phase 4 |
| 6 | 2026-07-19 | Docs: noded (CLAUDE.md index + small dense nodes) for token efficiency | User requirement |
