package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.cellview.client.DataGrid;
import platform.gwt.form2.client.form.dispatch.GEditPropertyDispatcher;
import platform.gwt.form2.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.GridDataRecord;
import platform.gwt.form2.shared.view.classes.GType;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.grid.GridEditableCell;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;

import java.util.Arrays;

public class GSinglePropertyTable extends DataGrid implements EditManager, GEditPropertyHandler {

    private final GFormController form;
    private final GEditPropertyDispatcher editDispatcher;
    private final GPropertyDraw property;
    private Object value;

    private GridEditableCell editCell;
    private Cell.Context editContext;
    private Element editCellParent;
    private GType editType;

    public GSinglePropertyTable(GFormController iform, GPropertyDraw iproperty) {
        this.form = iform;
        this.property = iproperty;

        this.editDispatcher = new GEditPropertyDispatcher(form);

        addColumn(property.createGridColumn(this, form));
        setRowData(Arrays.asList(new Object()));
    }


    public void setValue(Object value) {
        this.value = value;
        setRowData(Arrays.asList(new GridDataRecord(property.sID, value)));
    }

    public void setBackgroundColor(String color) {
        //todo:
    }

    public void setForegroundColor(String color) {
        //todo:
    }

    @Override
    public GPropertyDraw getProperty(int column) {
        return property;
    }

    @Override
    public void requestValue(GType valueType, Object oldValue) {
        editType = valueType;

        GridCellEditor cellEditor = valueType.createGridCellEditor(this, getProperty(editContext.getColumn()), oldValue);
        if (cellEditor != null) {
            editCell.startEditing(editContext, editCellParent, cellEditor);
        } else {
            cancelEditing();
        }
    }

    @Override
    public void updateEditValue(Object value) {
        //todo:
    }

    @Override
    public boolean isCurrentlyEditing() {
        //todo: возвращать true, если любая таблица редактируется, чтобы избежать двойного редактирования...
        return editType != null;
    }

    @Override
    public void executePropertyEditAction(GridEditableCell editCell, Cell.Context editContext, Element parent) {
        this.editCell = editCell;
        this.editContext = editContext;
        this.editCellParent = parent;
        editDispatcher.executePropertyEditAction(this, property, value, editContext);
    }

    @Override
    public void commitEditing(Object value) {
        clearEditState();
        editDispatcher.commitValue(value);
    }

    @Override
    public void cancelEditing() {
        clearEditState();
        editDispatcher.cancelEdit();
    }

    private void clearEditState() {
        editCell.finishEditing(editContext, editCellParent, value);

        editCell = null;
        editContext = null;
        editCellParent = null;
        editType = null;
    }

}
