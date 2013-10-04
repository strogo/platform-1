package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.InsertPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.ResizableHorizontalPanel;
import lsfusion.gwt.base.client.ui.ResizableVerticalPanel;
import lsfusion.gwt.form.client.form.ui.layout.SplitPanelBase;

public class TableSplitPanel extends SplitPanelBase<CellPanel> {

    public TableSplitPanel(boolean vertical) {
        super(vertical, vertical ? new ResizableVerticalPanel() : new ResizableHorizontalPanel());
    }

    @Override
    protected void addSplitterImpl(Splitter splitter) {
        panel.add(splitter);
        if (vertical) {
            splitter.setWidth("100%");
        } else {
            splitter.setHeight("100%");
        }
        setCellSize(!vertical, splitter, "100%");
    }

    @Override
    protected void addFirstWidgetImpl(Widget w) {
        ((InsertPanel)panel).insert(w, 0);
        w.setSize("100%", "100%");
        setCellSize(!vertical, w, "100%");
    }

    @Override
    protected void addSecondWidgetImpl(Widget w) {
        int index = firstWidget == null ? 1 : 2;
        ((InsertPanel)panel).insert(w, index);
        w.setSize("100%", "100%");
        setCellSize(!vertical, w, "100%");
    }

    @Override
    protected void setChildrenRatio(double ratio) {
        assert ratio >=0 && ratio <= 1;
        int prc1 = (int) (100 * ratio);
        if (firstWidget != null) {
            setCellSize(vertical, firstWidget, prc1 + "%");
        }
        if (secondWidget != null) {
            int prc2 = 100 - prc1;
            setCellSize(vertical, secondWidget, prc2 + "%");
        }
    }

    private void setCellSize(boolean height, Widget w, String size) {
        if (height) {
            panel.setCellHeight(w, size);
        } else {
            panel.setCellWidth(w, size);
        }
    }
}
