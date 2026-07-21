package com.wikigearsetups.bank;

/**
 * Which item a slot should hold.
 *
 * BEST_OWNED walks the wiki's ranked alternatives and takes the best one the
 * player can actually field — the plugin's normal, adaptive behaviour.
 *
 * TOP_RECOMMENDATION takes the wiki's own first choice regardless of
 * ownership, still flagging whether the player owns it, so the preview and
 * import can show the literal recommendation (a BIS / shopping-list view).
 * Reasoning that is *derived from ownership* — set-bonus gating and the
 * inventory's substitution chain — is suspended under this strategy;
 * everything else (slayer-task gating, two-handed shield handling,
 * variation and upgrade acceptance) applies to both.
 */
public enum PickStrategy
{
	BEST_OWNED,
	TOP_RECOMMENDATION
}
