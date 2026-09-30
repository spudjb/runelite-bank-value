package spudjb.bankvalue;

import net.runelite.client.config.*;
import spudjb.bankvalue.config.DataFormatSetting;

@ConfigGroup("bank-value")
public interface BankValueConfig extends Config {
	@ConfigSection(
			name = "Export bank data",
			description = "Export your bank data to CSV or JSON via your clipboard",
			position = 0
	)
	String exportBankData = "exportBankData";

	@ConfigItem(
			position = 0,
			keyName = "showExportBankButton",
			name = "Show export bank button",
			description = "Show the export bank data button",
			section = exportBankData
	)
	default boolean showExportBankButton()
	{
		return true;
	}

	@ConfigItem(
			position = 2,
			keyName = "hideUnderValue",
			name = "Hide under value",
			description = "Remove items under the specified value"
	)
	default int hideUnderValue()
	{
		return 0;
	}

	@ConfigItem(
			position = 3,
			keyName = "dataOrder",
			name = "Order of data",
			description = "Change the order of the exported data, or remove entries entirely. E.G.: name,value,quantity"
	)
	default String dataOrder()
	{
		return "name,itemid,quantity,value";
	}

	@ConfigItem(
			position = 3,
			keyName = "showDataTitles",
			name = "Add titles to export",
			description = "Add titles to your CSV export, e.g. Name, Item ID, Quantity, Value (only for CSV export)"
	)
	default boolean showDataTitles()
	{
		return true;
	}

	@ConfigItem(
			position = 4,
			keyName = "dataFormat",
			name = "Data Format",
			description = "Choose how to format your exported data"
	)
	default DataFormatSetting dataFormat()
	{
		return DataFormatSetting.CSV;
	}
}
