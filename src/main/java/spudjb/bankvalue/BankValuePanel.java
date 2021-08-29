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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

@Slf4j
class BankValuePanel extends PluginPanel
{
	private static final Color ODD_ROW = new Color(44, 44, 44);

	private final JPanel listContainer = new JPanel();

	private BankValueTableHeader countHeader;
	private BankValueTableHeader valueHeader;
	private BankValueTableHeader nameHeader;

	private SortOrder orderIndex = SortOrder.VALUE;
	private boolean ascendingOrder = false;

	private ArrayList<BankValueTableRow> rows = new ArrayList<>();
	private BankValuePlugin plugin;

	private String filterString = "";
	private List<CachedItem> cachedItems = new ArrayList<>();
	private JLabel bankValueLabel;
	private final JPanel totalValuePanel;

	BankValuePanel(BankValuePlugin plugin)
	{
		this.plugin = plugin;

		setBorder(null);
		setLayout(new DynamicGridLayout(0, 1));

		JPanel headerContainer = buildHeader();

		listContainer.setLayout(new GridLayout(0, 1));

		totalValuePanel = buildTotalValueBox();
		updateBankTotal();

		add(totalValuePanel);
		add(buildFilterBox());
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
					return Integer.compare(r1.getPrice(), r2.getPrice()) * (ascendingOrder ? 1 : -1);
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

	void populate()
	{
		rows.clear();

		for (int i = 0; i < cachedItems.size(); i++)
		{
			CachedItem item = cachedItems.get(i);

			if (item.getName().toLowerCase().contains(filterString.toLowerCase()))
			{
				rows.add(buildRow(item, i % 2 == 0));
			}
		}

		updateList();
	}

	void setItems(List<CachedItem> items)
	{
		this.cachedItems = items;
		updateBankTotal();
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
		valueHeader.setPreferredSize(new Dimension(BankValueTableRow.ITEM_VALUE_COLUMN_WIDTH, 0));
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

	private JPanel buildTotalValueBox() {
		BorderLayout layout = new BorderLayout(1, 1);
		JPanel totalValuePanel = new JPanel(layout);
		totalValuePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		bankValueLabel = new JLabel("Bank value: Not loaded");
		totalValuePanel.add(bankValueLabel, BorderLayout.LINE_START);

		return totalValuePanel;
	}

	private void updateBankTotal() {
		int value = cachedItems.stream().map(item -> item.getValue() * item.getQuantity()).mapToInt(Integer::intValue).sum();

		String valueString = value > 0 ? QuantityFormatter.quantityToStackSize(value) : "Not loaded";
		bankValueLabel.setText(String.format("Bank value: %s", valueString));
		totalValuePanel.repaint();
	}

	private JPanel buildFilterBox()
	{
		BorderLayout layout = new BorderLayout(1, 1);
		JPanel filterPanel = new JPanel(layout);
		filterPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		final JLabel uiLabel = new JLabel("Search: ");
		final JTextField uiInput = new JTextField();

		uiInput.addKeyListener(new java.awt.event.KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
				filterString = uiInput.getText();
				populate();
			}

			@Override
			public void keyPressed(KeyEvent e)
			{
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
			}
		});

		filterPanel.add(uiLabel, BorderLayout.LINE_START);
		filterPanel.add(uiInput, BorderLayout.CENTER);

		return filterPanel;
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
