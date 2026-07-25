package com.wikigearsetups.layout;

import com.wikigearsetups.WikiGearSetupsConfig;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.tabs.Layout;

/**
 * Shrinks the bank's scroll range to the end of an imported layout.
 *
 * Core Bank Tags draws an empty slot into every remaining widget child of the
 * bank item container so items can be dragged anywhere ("Fill the remaining
 * slots with -1" in LayoutManager#layout). Those children are allocated per
 * item in the whole bank, so a player with a large bank scrolls through a
 * hundred-odd empty rows below a twelve-row layout. Core only ever clamps the
 * scroll *position* (LayoutManager#scrollLayout), never the range.
 *
 * Setting the range to the last populated row leaves the empty slots on screen
 * and draggable — a tall bank window still shows them under the layout — but
 * stops the scrollbar running off into nothing.
 *
 * Deliberately scoped to tabs this plugin created: a hand-made tab, or one
 * belonging to another plugin, keeps whatever behaviour its owner intended.
 */
@Slf4j
@Singleton
public class LayoutScrollTrimmer
{
	private static final int ROW_HEIGHT =
		BankTagsPlugin.BANK_ITEM_HEIGHT + BankTagsPlugin.BANK_ITEM_Y_PADDING;

	/** Breathing room under the last row, matching what Bank Tag Layouts leaves. */
	private static final int BOTTOM_PADDING = 8;

	private final Client client;
	private final ClientThread clientThread;
	private final BankTagsService bankTagsService;
	private final BankTagImporter importer;
	private final WikiGearSetupsConfig config;

	@Inject
	public LayoutScrollTrimmer(Client client, ClientThread clientThread,
		BankTagsService bankTagsService, BankTagImporter importer, WikiGearSetupsConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.bankTagsService = bankTagsService;
		this.importer = importer;
		this.config = config;
	}

	/**
	 * Call once the bank has finished building. Client thread only — reads and
	 * writes bank widgets.
	 */
	public void onBankBuilt()
	{
		if (!config.trimEmptySpace())
		{
			return;
		}

		Layout layout = bankTagsService.getActiveLayout();
		if (layout == null || !importer.wasImported(bankTagsService.getActiveTag()))
		{
			return;
		}

		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (items == null)
		{
			return;
		}

		int lastRow = lastUsedRow(layout);
		if (lastRow < 0)
		{
			return;
		}

		int height = (lastRow + 1) * ROW_HEIGHT + BOTTOM_PADDING;
		// only ever shrink: growing the range would invent space the bank has no widgets for
		if (height >= items.getScrollHeight())
		{
			return;
		}

		items.setScrollHeight(height);
		int scrollY = Math.min(items.getScrollY(), Math.max(0, height - items.getHeight()));

		// the script vm isn't reentrant, so the scrollbar refresh has to wait a tick
		clientThread.invokeLater(() -> client.runScript(ScriptID.UPDATE_SCROLLBAR,
			InterfaceID.Bankmain.SCROLLBAR, InterfaceID.Bankmain.ITEMS, scrollY));

		log.debug("Trimmed bank scroll to {}px ({} rows) for '{}'",
			height, lastRow + 1, bankTagsService.getActiveTag());
	}

	/** Row index of the last populated slot, or -1 when the layout is empty. */
	static int lastUsedRow(Layout layout)
	{
		for (int pos = layout.size() - 1; pos >= 0; pos--)
		{
			if (layout.getItemAtPos(pos) > -1)
			{
				return pos / BankTagsPlugin.BANK_ITEMS_PER_ROW;
			}
		}
		return -1;
	}
}
