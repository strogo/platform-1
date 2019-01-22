package lsfusion.gwt.client.form.ui.grid.editor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.ui.ResizableVerticalPanel;
import lsfusion.gwt.client.form.ui.cellview.cell.Cell;
import lsfusion.gwt.client.form.ui.grid.EditEvent;
import lsfusion.gwt.client.form.ui.grid.EditManager;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.changes.dto.ColorDTO;
import net.auroris.ColorPicker.client.ColorPicker;

public class ColorGridCellEditor extends PopupBasedGridCellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private ColorPicker colorPicker;

    public ColorGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected Widget createPopupComponent() {
        colorPicker = new ColorPicker();

        Button btnOk = new Button(messages.ok());
        btnOk.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                commitEditing(new ColorDTO(colorPicker.getHexColor()));
            }
        });

        Button btnCancel = new Button(messages.cancel());
        btnCancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onCancel();
            }
        });

        FlowPanel bottomPane = new FlowPanel();
        bottomPane.add(btnOk);
        bottomPane.add(btnCancel);

        ResizableVerticalPanel mainPane = new ResizableVerticalPanel();
        mainPane.add(colorPicker);
        mainPane.add(bottomPane);
        mainPane.setCellHorizontalAlignment(bottomPane, HasAlignment.ALIGN_RIGHT);

        return mainPane;
    }

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        if (oldValue != null) {
            try {
                colorPicker.setHex(((ColorDTO)oldValue).value);
            } catch (Exception e) {
                throw new IllegalStateException("can't convert string value to color");
            }
        }

        super.startEditing(editEvent, context, parent, oldValue);
    }
}