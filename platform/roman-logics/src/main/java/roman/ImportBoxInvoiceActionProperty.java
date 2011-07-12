package roman;

import jxl.read.biff.BiffException;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 16:01
 */

public abstract class ImportBoxInvoiceActionProperty extends BaseImportActionProperty {

    protected ImportField invoiceSIDField, dateInvoiceField, boxNumberField, barCodeField, colorCodeField, sidField,
            colorNameField, sizeField, compositionField, countryField, customCodeField, customCode6Field,
            unitPriceField, unitQuantityField, unitNetWeightField, originalNameField, numberSkuField, RRPField;

    public ImportBoxInvoiceActionProperty(RomanLogicsModule RomanLM, ValueClass supplierClass) {
        super(RomanLM, "Импортировать инвойс", supplierClass);
    }

    public ImportBoxInvoiceActionProperty(RomanLogicsModule RomanLM, ValueClass supplierClass, String extensions) {
        super(RomanLM, "Импортировать инвойс", supplierClass, extensions);
    }

    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        return new ExcelInputTable(inFile);
    }

    protected abstract SingleSheetImporter createImporter(ImportInputTable inputTable);

    private void initFields() {
        invoiceSIDField = new ImportField(LM.sidDocument);
        dateInvoiceField = new ImportField(LM.baseLM.date);
        boxNumberField = new ImportField(LM.sidSupplierBox);
        barCodeField = new ImportField(LM.baseLM.barcode);
        colorCodeField = new ImportField(LM.sidColorSupplier);
        sidField = new ImportField(LM.sidArticle);
        colorNameField = new ImportField(LM.baseLM.name);
        sizeField = new ImportField(LM.sidSizeSupplier);
        compositionField = new ImportField(LM.mainCompositionOriginArticle);
        countryField = new ImportField(LM.baseLM.name);
        customCodeField = new ImportField(LM.sidCustomCategoryOrigin);
        customCode6Field = new ImportField(LM.sidCustomCategory6);
        unitPriceField = new ImportField(LM.priceDataDocumentItem);
        unitQuantityField = new ImportField(LM.quantityDataListSku);
        unitNetWeightField = new ImportField(LM.netWeightArticle);
        originalNameField = new ImportField(LM.originalNameArticle);
        numberSkuField = new ImportField(LM.numberDataListSku);
        RRPField = new ImportField(LM.RRPDocumentArticle);
    }

    protected boolean isSimpleInvoice() {
        return false;
    }

    ;

    public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        DataObject supplier = keys.get(supplierInterface);

        initFields();

        List<byte[]> fileList = valueClass.getFiles(value.getValue());

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> invoiceKey;

        if (!isSimpleInvoice()) {
            invoiceKey = new ImportKey(LM.boxInvoice, LM.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        } else {
            invoiceKey = new ImportKey(LM.simpleInvoice, LM.documentSIDSupplier.getMapping(invoiceSIDField, supplier));
        }
        properties.add(new ImportProperty(invoiceSIDField, LM.sidDocument.getMapping(invoiceKey)));
        properties.add(new ImportProperty(dateInvoiceField, LM.baseLM.date.getMapping(invoiceKey)));
        properties.add(new ImportProperty(supplier, LM.supplierDocument.getMapping(invoiceKey)));

        ImportKey<?> boxKey = null;
        if (!isSimpleInvoice()) {
            boxKey = new ImportKey(LM.supplierBox, LM.supplierBoxSIDSupplier.getMapping(boxNumberField, supplier));
            properties.add(new ImportProperty(invoiceSIDField, LM.boxInvoiceSupplierBox.getMapping(boxKey), LM.object(LM.boxInvoice).getMapping(invoiceKey)));
            properties.add(new ImportProperty(boxNumberField, LM.sidSupplierBox.getMapping(boxKey)));
            properties.add(new ImportProperty(boxNumberField, LM.baseLM.barcode.getMapping(boxKey)));
        }

        ImportKey<?> articleKey = new ImportKey(LM.articleComposite, LM.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, LM.sidArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(supplier, LM.supplierArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(compositionField, LM.mainCompositionOriginArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(unitNetWeightField, LM.netWeightArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(originalNameField, LM.originalNameArticle.getMapping(articleKey)));

        ImportKey<?> itemKey = new ImportKey(LM.item, LM.baseLM.barcodeToObject.getMapping(barCodeField));
        properties.add(new ImportProperty(barCodeField, LM.baseLM.barcode.getMapping(itemKey)));
        properties.add(new ImportProperty(unitNetWeightField, LM.netWeightDataSku.getMapping(itemKey)));
        properties.add(new ImportProperty(sidField, LM.articleCompositeItem.getMapping(itemKey), LM.object(LM.articleComposite).getMapping(articleKey)));

        ImportKey<?> countryKey = new ImportKey(LM.countrySupplier, LM.countryNameSupplier.getMapping(countryField, supplier));
        properties.add(new ImportProperty(countryField, LM.baseLM.name.getMapping(countryKey)));
        properties.add(new ImportProperty(supplier, LM.supplierCountrySupplier.getMapping(countryKey)));
        properties.add(new ImportProperty(countryField, LM.countrySupplierOfOriginArticle.getMapping(articleKey), LM.object(LM.countrySupplier).getMapping(countryKey)));

        ImportKey<?> customCategoryKey = new ImportKey(LM.customCategoryOrigin, LM.sidToCustomCategoryOrigin.getMapping(customCodeField));
        properties.add(new ImportProperty(customCodeField, LM.sidCustomCategoryOrigin.getMapping(customCategoryKey)));
        properties.add(new ImportProperty(customCodeField, LM.customCategoryOriginArticle.getMapping(articleKey),
                LM.object(LM.customCategoryOrigin).getMapping(customCategoryKey)));

        ImportKey<?> customCategory6Key = new ImportKey(LM.customCategory6, LM.sidToCustomCategory6.getMapping(customCode6Field));
        properties.add(new ImportProperty(customCode6Field, LM.sidCustomCategory6.getMapping(customCategory6Key)));

        ImportKey<?> colorKey = new ImportKey(LM.colorSupplier, LM.colorSIDSupplier.getMapping(colorCodeField, supplier));
        properties.add(new ImportProperty(colorCodeField, LM.sidColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(supplier, LM.supplierColorSupplier.getMapping(colorKey)));
        properties.add(new ImportProperty(colorNameField, LM.baseLM.name.getMapping(colorKey)));
        properties.add(new ImportProperty(colorCodeField, LM.colorSupplierItem.getMapping(itemKey), LM.object(LM.colorSupplier).getMapping(colorKey)));

        ImportKey<?> sizeKey = new ImportKey(LM.sizeSupplier, LM.sizeSIDSupplier.getMapping(sizeField, supplier));
        properties.add(new ImportProperty(sizeField, LM.sidSizeSupplier.getMapping(sizeKey)));
        properties.add(new ImportProperty(supplier, LM.supplierSizeSupplier.getMapping(sizeKey)));
        properties.add(new ImportProperty(sizeField, LM.sizeSupplierItem.getMapping(itemKey), LM.object(LM.sizeSupplier).getMapping(sizeKey)));

        if (!isSimpleInvoice()) {
            properties.add(new ImportProperty(numberSkuField, LM.numberListArticle.getMapping(boxKey, articleKey)));
            properties.add(new ImportProperty(numberSkuField, LM.numberDataListSku.getMapping(boxKey, itemKey)));
            properties.add(new ImportProperty(unitQuantityField, LM.quantityDataListSku.getMapping(boxKey, itemKey)));
        } else {
            properties.add(new ImportProperty(numberSkuField, LM.numberListArticle.getMapping(invoiceKey, articleKey)));
            properties.add(new ImportProperty(numberSkuField, LM.numberDataListSku.getMapping(invoiceKey, itemKey)));
            properties.add(new ImportProperty(unitQuantityField, LM.quantityDataListSku.getMapping(invoiceKey, itemKey)));
        }

        properties.add(new ImportProperty(unitPriceField, LM.priceDataDocumentItem.getMapping(invoiceKey, itemKey)));
        properties.add(new ImportProperty(RRPField, LM.RRPDocumentArticle.getMapping(invoiceKey, articleKey)));
        properties.add(new ImportProperty(unitPriceField, LM.priceDocumentArticle.getMapping(invoiceKey, articleKey)));

        for (byte[] file : fileList) {
            ImportTable table;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream(file);
                table = createImporter(createTable(inFile)).getTable();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            ImportKey<?>[] keysArray;
            if (!isSimpleInvoice()) {
                keysArray = new ImportKey<?>[]{invoiceKey, boxKey, articleKey, itemKey, colorKey, sizeKey, countryKey, customCategoryKey, customCategory6Key};
            } else {
                keysArray = new ImportKey<?>[]{invoiceKey, articleKey, itemKey, colorKey, sizeKey, countryKey, customCategoryKey, customCategory6Key};
            }
            new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);
        }

        actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
    }
}
