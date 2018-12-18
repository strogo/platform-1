package lsfusion.utils.word;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.RawFileData;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.impl.values.XmlValueDisconnectedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;

public class ProcessTemplateActionProperty extends ScriptingActionProperty {
    public final ClassPropertyInterface templateInterface;

    public ProcessTemplateActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        templateInterface = i.next();

    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataObject templateObject = context.getDataKeyValue(templateInterface);

            if (templateObject != null) {

                ObjectValue fileObjectValue = findProperty("file[Template]").readClasses(context, templateObject);
                if (fileObjectValue instanceof DataObject) {

                    DataObject wordObject = (DataObject)fileObjectValue;
                    List<List<Object>> templateEntriesList = new ArrayList<>();

                    KeyExpr templateEntryExpr = new KeyExpr("TemplateEntry");
                    ImRevMap<Object, KeyExpr> templateEntryKeys = MapFact.singletonRev((Object) "TemplateEntry", templateEntryExpr);

                    QueryBuilder<Object, Object> templateEntryQuery = new QueryBuilder<>(templateEntryKeys);
                    templateEntryQuery.addProperty("keyTemplateEntry", findProperty("key[TemplateEntry]").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("valueTemplateEntry", findProperty("value[TemplateEntry]").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("typeTemplateEntry", findProperty("idType[TemplateEntry]").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("firstRowTemplateEntry", findProperty("firstRow[TemplateEntry]").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("columnSeparatorTemplateEntry", findProperty("columnSeparator[TemplateEntry]").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("rowSeparatorTemplateEntry", findProperty("rowSeparator[TemplateEntry]").getExpr(context.getModifier(), templateEntryExpr));

                    templateEntryQuery.and(findProperty("template[TemplateEntry]").getExpr(context.getModifier(), templateEntryQuery.getMapExprs().get("TemplateEntry")).compare(templateObject.getExpr(), Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> templateEntryResult = templateEntryQuery.execute(context);

                    for (ImMap<Object, Object> templateEntry : templateEntryResult.values()) {

                        String keyTemplateEntry = (String) templateEntry.get("keyTemplateEntry");
                        String valueTemplateEntry = (String) templateEntry.get("valueTemplateEntry");
                        String type = (String) templateEntry.get("typeTemplateEntry");
                        Integer firstRowTemplateEntry = (Integer) templateEntry.get("firstRowTemplateEntry");
                        String columnSeparatorTemplateEntry = (String) templateEntry.get("columnSeparatorTemplateEntry");
                        String rowSeparatorTemplateEntry = (String) templateEntry.get("rowSeparatorTemplateEntry");

                        if (keyTemplateEntry != null && valueTemplateEntry != null)
                            templateEntriesList.add(Arrays.asList((Object) keyTemplateEntry, valueTemplateEntry.replace('\n', '\r'), 
                                                                  type, firstRowTemplateEntry, columnSeparatorTemplateEntry, rowSeparatorTemplateEntry));
                    }

                    RawFileData fileObject = (RawFileData) fileObjectValue.getValue();
                    byte[] bytes = fileObject.getBytes();
                    boolean isDocx = bytes.length > 2 && bytes[0] == 80 && bytes[1] == 75;

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                    if (isDocx) {
                        XWPFDocument document = new XWPFDocument(((RawFileData) wordObject.object).getInputStream());
                        List<XWPFParagraph> docParagraphs = new ArrayList<>(document.getParagraphs()); //copy of mutable list
                        XWPFNumbering numbering = document.getNumbering();
                        for (List<Object> entry : templateEntriesList) {
                            String key = (String) entry.get(0);
                            String value = (String) entry.get(1);
                            String type = (String) entry.get(2);
                            boolean isTable = type != null && type.endsWith("table");
                            boolean isList = type != null && type.endsWith("list");
                            Integer firstRow = (Integer) entry.get(3);
                            String columnSeparator = (String) entry.get(4);
                            String rowSeparator = (String) entry.get(5);
                            if(isList) {
                                replaceListDataDocx(document, docParagraphs, numbering, key, value, rowSeparator);
                            } else {
                                for (XWPFTable tbl : document.getTables()) {
                                    replaceTableDataDocx(tbl, key, value, isTable, firstRow, columnSeparator, rowSeparator);
                                }
                                replaceInParagraphs(document, key, value);
                            }
                        }
                        document.write(outputStream);
                    } else {
                        HWPFDocument document = new HWPFDocument(new POIFSFileSystem(((RawFileData) wordObject.object).getInputStream()));
                        Range range = document.getRange();
                        for (List<Object> entry : templateEntriesList) {
                            range.replaceText((String) entry.get(0), (String) entry.get(1));
                        }
                        document.write(outputStream);
                    }

                    findProperty("resultTemplate[]").change(new RawFileData(outputStream), context);
                }
            }
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private void replaceTableDataDocx(XWPFTable tbl, String key, String value, boolean isTable, Integer firstRow, String columnSeparator, String rowSeparator) {
        if(isTable) {
            XWPFTableRow row = tbl.getRow(firstRow);
            if (row == null) return;
            XWPFTableCell cell = row.getCell(0);
            String text = cell.getText();
            if (text != null && text.contains(key)) {
                String[] tableRows = value.split(rowSeparator);
                int i = firstRow;
                for (String tableRow : tableRows) {
                    if (i == firstRow) {
                        XWPFTableRow newRow = tbl.getRow(i);
                        int j = 0;
                        for (String tableCell : tableRow.split(columnSeparator)) {
                            XWPFTableCell newCell = newRow.getTableICells().size() > j ? newRow.getCell(j) : newRow.createCell();
                            if (newCell.getText().isEmpty())
                                newCell.setText(tableCell);
                            else {
                                newCell.getParagraphs().get(0).getRuns().get(0).setText(tableCell, 0);
                            }
                            j++;
                        }
                    } else {
                        XWPFTableRow newRow = tbl.createRow();
                        int j = 0;
                        for (String tableCell : tableRow.split(columnSeparator)) {
                            XWPFTableCell newCell = newRow.getTableICells().size() > j ? newRow.getCell(j) : newRow.createCell();
                            newCell.setText(tableCell);
                            j++;
                        }
                    }
                    i++;
                }
            }
        } else {
            for (XWPFTableRow row : tbl.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        for (XWPFRun r : p.getRuns()) {
                            String text = r.getText(0);
                            if (text != null && text.contains(key)) {
                                text = text.replace(key, value);
                                r.setText(text, 0);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void replaceInParagraphs(XWPFDocument document, String find, String repl) {

        Set<XWPFParagraph> toDelete = new HashSet<XWPFParagraph>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();

            TextSegement found = paragraph.searchText(find, new PositionInParagraph());
            if (found != null) {
                if (found.getBeginRun() == found.getEndRun()) {
                    // whole search string is in one Run
                    XWPFRun run = runs.get(found.getBeginRun());
                    String runText = run.getText(run.getTextPosition());
                    String replaced = runText.replace(find, repl);
                    setText(run, replaced);
                } else {
                    // The search string spans over more than one Run
                    // Put the Strings together
                    StringBuilder b = new StringBuilder();
                    for (int runPos = found.getBeginRun(); runPos <= found.getEndRun(); runPos++) {
                        XWPFRun run = runs.get(runPos);
                        b.append(run.getText(run.getTextPosition()));
                    }
                    String connectedRuns = b.toString();
                    String replaced = connectedRuns.replace(find, repl);

                    // The first Run receives the replaced String of all connected Runs
                    XWPFRun partOne = runs.get(found.getBeginRun());
                    setText(partOne, replaced);
                    // Removing the text in the other Runs.
                    for (int runPos = found.getBeginRun() + 1; runPos <= found.getEndRun(); runPos++) {
                        XWPFRun partNext = runs.get(runPos);
                        partNext.setText("", 0);
                    }
                }
                if (paragraph.getText().isEmpty())
                    toDelete.add(paragraph);
            }
        }
        for (XWPFParagraph paragraph : toDelete) {
            document.removeBodyElement(document.getPosOfParagraph(paragraph));
        }
    }

    private void replaceListDataDocx(XWPFDocument document, List<XWPFParagraph> docParagraphs, XWPFNumbering numbering, String key, String value, String rowSeparator) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < docParagraphs.size(); i++) {
            XWPFParagraph p = docParagraphs.get(i);
            BigInteger numID = getNumID(p);
            if (numID != null) {
                String pText = p.getText();//getText(p);
                if (pText != null && pText.equals(key)) {
                    XmlCursor cursor = p.getCTP().newCursor();
                    for (String row : value.split(rowSeparator)) {
                        XWPFParagraph newParagraph = document.createParagraph();
                        newParagraph.getCTP().setPPr(p.getCTP().getPPr());
                        XWPFRun newRun = newParagraph.createRun();
                        newRun.getCTR().setRPr(p.getRuns().get(0).getCTR().getRPr());
                        newRun.setText(row, 0);
                        XmlCursor newCursor = newParagraph.getCTP().newCursor();
                        newCursor.moveXml(cursor);
                        newCursor.dispose();
                    }
                    cursor.removeXml(); // Removes replacement text paragraph
                    cursor.dispose();

                }
            }
        }
    }

    private BigInteger getNumID(XWPFParagraph p) {
        try {
            //due to bug we can't read already changed paragraphs
            //https://stackoverflow.com/questions/8253653/exception-when-writing-to-the-xlsx-document-several-times-using-apache-poi-3-7
            return p.getNumID();
        } catch (XmlValueDisconnectedException e) {
            return null;
        }
    }
    
    private void setText(XWPFRun run, String newText) {
        List<String> splitted = BaseUtils.split(newText,"\r");
        for (int j = 0; j < splitted.size(); j++) {
            if (j > 0) {
                run.addBreak();
                run.setText(splitted.get(j));
            } else
                run.setText(splitted.get(j), 0);
        }
    }
}