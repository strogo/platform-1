package roman;

import platform.base.BaseUtils;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.text.ParseException;

/**
 * User: DAle
 * Date: 28.02.11
 * Time: 19:30
 */

public class HugoBossInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = 35;

    private final static int CUSTOM_NUMBER = 15, CUSTOM_NUMBER_6 = 16, EAN = 0;

    public HugoBossInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, EAN).trim().matches("^(0\\d{13}|\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        if (column == LAST_COLUMN + 1) {
            return "";
        }
        return super.getCellString(field, row, column);
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {
            case CUSTOM_NUMBER:
                if (value.length() < 10) {
                    value = value + BaseUtils.replicate('0', 10 - value.length());
                }
                return value.substring(0, 10);

            case CUSTOM_NUMBER_6:
                return value.substring(0, 6);

            case EAN:
                if (value.length() == 14)
                    return value.substring(1);
                else
                    return value;

            default: return value;
        }
    }
}
