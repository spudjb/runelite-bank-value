/*
 * Copyright (c) 2018, Psikoi <https://github.com/Psikoi>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package spudjb.bankvalue;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import spudjb.bankvalue.config.DataFormatSetting;

@Slf4j
class BankValuePanel extends PluginPanel
{
	private final BankValuePlugin plugin;
	private final ConfigManager configManager;
	private final BankValueConfig config;
	private Gson gson;
	private static final Color ODD_ROW = new Color(44, 44, 44);
	private final JPanel listContainer = new JPanel();
	private BankValueTableHeader countHeader;
	private BankValueTableHeader valueHeader;
	private BankValueTableHeader nameHeader;
	private SortOrder orderIndex = SortOrder.VALUE;
	private boolean ascendingOrder = false;
	private ArrayList<BankValueTableRow> rows = new ArrayList<>();

	BankValuePanel(BankValuePlugin plugin, BankValueConfig config, ConfigManager configManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.configManager = configManager;

		setBorder(null);
		setLayout(new DynamicGridLayout(0, 1));

		JPanel headerContainer = buildHeader();
		JPanel exportContainer = buildExportPanel();

		listContainer.setLayout(new GridLayout(0, 1));

		if (config.showExportBankButton()) {
			add(exportContainer);
		}

		add(headerContainer);
		add(listContainer);
	}

	void updateList()
	{
		rows.sort((r1, r2) ->
		{
			switch (orderIndex)
			{
				case NAME:
					return r1.getItemName().compareTo(r2.getItemName()) * (ascendingOrder ? 1 : -1);
				case COUNT:
					return Integer.compare(r1.getItemCount(), r2.getItemCount()) * (ascendingOrder ? 1 : -1);
				case VALUE:
					return Long.compare(r1.getPrice(), r2.getPrice()) * (ascendingOrder ? 1 : -1);
				default:
					return 0;
			}
		});

		listContainer.removeAll();

		for (int i = 0; i < rows.size(); i++)
		{
			BankValueTableRow row = rows.get(i);
			row.setBackground(i % 2 == 0 ? ODD_ROW : ColorScheme.DARK_GRAY_COLOR);
			listContainer.add(row);
		}

		listContainer.revalidate();
		listContainer.repaint();
	}

	void populate(List<CachedItem> items)
	{
		rows.clear();

		for (int i = 0; i < items.size(); i++)
		{
			CachedItem item = items.get(i);

			rows.add(buildRow(item, i % 2 == 0));
		}

		updateList();
	}

	private void orderBy(SortOrder order)
	{
		nameHeader.highlight(false, ascendingOrder);
		countHeader.highlight(false, ascendingOrder);
		valueHeader.highlight(false, ascendingOrder);

		switch (order)
		{
			case NAME:
				nameHeader.highlight(true, ascendingOrder);
				break;
			case COUNT:
				countHeader.highlight(true, ascendingOrder);
				break;
			case VALUE:
				valueHeader.highlight(true, ascendingOrder);
				break;
		}

		orderIndex = order;
		updateList();
	}

	private void exportToClipboard()
	{
		if (null == plugin.cachedItems) return;
		Object result;

		if (config.dataFormat() == DataFormatSetting.JSON) {
			result = new JsonArray();

			for (CachedItem item : plugin.cachedItems) {
				if (config.hideUnderValue() > (item.getValue() * item.getQuantity())) {
					continue;
				}
				((JsonArray) result).add(createJsonObject(item.getName(), item.getId(), item.getQuantity(), item.getValue() * item.getQuantity()));
			}
		} else {
			result = new StringBuilder();

			if (config.showDataTitles()) {
				((StringBuilder) result).append(addCSVLine("Item Name", "Item ID", "Quantity", "Value"));
			}

			for (CachedItem item : plugin.cachedItems) {
				if (config.hideUnderValue() > (item.getValue() * item.getQuantity())) {
					continue;
				}
				((StringBuilder) result).append(addCSVLine(item.getName(), item.getId(), item.getQuantity(), item.getValue() * item.getQuantity()));
			}
		}

		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(result.toString()), null);
	}

	/**
	 * Builds the entire table header.
	 */
	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout());
		JPanel leftSide = new JPanel(new BorderLayout());
		JPanel rightSide = new JPanel(new BorderLayout());

		nameHeader = new BankValueTableHeader("Name", orderIndex == SortOrder.NAME, ascendingOrder);
		nameHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (SwingUtilities.isRightMouseButton(mouseEvent))
				{
					return;
				}
				ascendingOrder = orderIndex != SortOrder.NAME || !ascendingOrder;
				orderBy(SortOrder.NAME);
			}
		});

		countHeader = new BankValueTableHeader("#", orderIndex == SortOrder.COUNT, ascendingOrder);
		countHeader.setPreferredSize(new Dimension(BankValueTableRow.ITEM_COUNT_COLUMN_WIDTH, 0));
		countHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (SwingUtilities.isRightMouseButton(mouseEvent))
				{
					return;
				}
				ascendingOrder = orderIndex != SortOrder.COUNT || !ascendingOrder;
				orderBy(SortOrder.COUNT);
			}
		});

		valueHeader = new BankValueTableHeader("$", orderIndex == SortOrder.VALUE, ascendingOrder);
		valueHeader.setPreferredSize(new Dimension(BankValueTableRow. ITEM_VALUE_COLUMN_WIDTH, 0));
		valueHeader.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (SwingUtilities.isRightMouseButton(mouseEvent))
				{
					return;
				}
				ascendingOrder = orderIndex != SortOrder.VALUE || !ascendingOrder;
				orderBy(SortOrder.VALUE);
			}
		});


		leftSide.add(nameHeader, BorderLayout.CENTER);
		leftSide.add(countHeader, BorderLayout.EAST);
		rightSide.add(valueHeader, BorderLayout.CENTER);

		header.add(leftSide, BorderLayout.CENTER);
		header.add(rightSide, BorderLayout.EAST);

		return header;
	}

	private JPanel buildExportPanel()
	{
		JPanel exportPanel = new JPanel(new BorderLayout(1, 0));
		JPanel buttonPanel = new JPanel(new BorderLayout(0, 1));
		final JButton exportToClipboardButton = new JButton();

		exportPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
		exportToClipboardButton.setText("Copy Bank Data to clipboard");
		exportToClipboardButton.setToolTipText("Export your bank data, copied to your clipboard. Configure in settings");
		exportToClipboardButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				exportToClipboardButton.setForeground(ColorScheme.BRAND_ORANGE);
				exportToClipboardButton.setText("Copy Bank Data to clipboard");
			}
			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				exportToClipboardButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
			@Override
			public void mouseClicked(MouseEvent mouseEvent)
			{
				exportToClipboard();
				exportToClipboardButton.setText("Copied!");
			}
		});

		buttonPanel.add(exportToClipboardButton, BorderLayout.CENTER);
		exportPanel.add(buttonPanel, BorderLayout.CENTER);
		return exportPanel;
	}

	private <T> StringBuilder addCSVLine(String nameVal, T idVal, T quantityVal, T gpVal)
	{
		String[] order = config.dataOrder().split(",");
		StringBuilder newLine = new StringBuilder();

		for (int i = 0; i < order.length; i++) {
			switch(order[i]) {
				case "name":
					newLine.append(nameVal);
					break;
				case "itemid":
					newLine.append(idVal);
					break;
				case "quantity":
					newLine.append(quantityVal);
					break;
				case "value":
					newLine.append(gpVal);
			}

			if (i != order.length - 1) {
				newLine.append(',');
			}
		}
		newLine.append('\n');
		return newLine;
	}

	private <T> JsonObject createJsonObject(String nameVal, T idVal, T quantityVal, T gpVal)
	{
		String[] order = config.dataOrder().split(",");
		JsonObject item = new JsonObject();
		Gson gson = new GsonBuilder().create();

		for (String property : order) {
			switch (property) {
				case "itemid":
					item.add("id", gson.toJsonTree(idVal));
					break;
				case "name":
					item.add("name", gson.toJsonTree(nameVal));
					break;
				case "quantity":
					item.add("quantity", gson.toJsonTree(quantityVal));
					break;
				case "value":
					item.add("value", gson.toJsonTree(gpVal));
			}
		}
		return item;
	}

	/**
	 * Builds a table row, that displays the bank's information.
	 */
	private BankValueTableRow buildRow(CachedItem item, boolean stripe)
	{
		BankValueTableRow row = new BankValueTableRow(item);
		row.setBackground(stripe ? ODD_ROW : ColorScheme.DARK_GRAY_COLOR);
		return row;
	}

	/**
	 * Enumerates the multiple ordering options for the bank list.
	 */
	private enum SortOrder
	{
		COUNT,
		VALUE,
		NAME
	}
}

