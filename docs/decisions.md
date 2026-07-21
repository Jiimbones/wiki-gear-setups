# Decisions (locked — don't re-litigate)

| # | Date | Decision | Why |
|---|---|---|---|
| 1 | 2026-07-19 | Distribution: RuneLite Plugin Hub | User choice. Implies: BSD-2 license, dependency whitelist, code-review-defensive quality |
| 2 | 2026-07-19 | Missing gear behavior: configurable enum (`OMIT` default / `SHOW_BEST_UNOWNED`) | User choice; OMIT = "best owned only" default |
| 3 | 2026-07-19 | v1 scope: equipment slots only; suggested inventory deferred to v1.1 | User choice; halves parser surface for v1 |
| 4 | 2026-07-19 | Pure client-side plugin, no backend; parse wikitext in-plugin | Bank read requires in-process; hub disallows heavy deps; MediaWiki API is open |
| 5 | 2026-07-19 | Output via core BankTags layouts API, clipboard import string as fallback | Native tab behavior; API surface to be verified in Phase 4 |
| 6 | 2026-07-19 | Docs: noded (CLAUDE.md index + small dense nodes) for token efficiency | User requirement |
| 7 | 2026-07-21 | `PickStrategy.TOP_RECOMMENDATION` ("Show wiki BIS") suspends **only ownership-derived reasoning**: set-bonus gating, the set-worn fixpoint pass, inventory substitution, and the `Missing gear` config (unowned always placed). Slayer gating, two-handed shield dropping, variation/upgrade acceptance and example enrichment apply in both modes. Ownership still sets the `owned` flag and the placed variant id | Resolving against an empty owned set would be simpler but loses the owned/unowned marking that makes the view worth having |
| 8 | 2026-07-21 | Recommended mode appends `-bis` to the auto tag name; import button relabels | A BIS tab is a different artifact from the adaptive tab; without this, importing in the mode silently overwrites the tab imported minutes earlier under the same generated name |
