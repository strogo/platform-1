package platform.gwt.form2.client.form.ui;

import com.google.gwt.user.client.ui.*;
import platform.gwt.form2.shared.view.GGrid;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;

import java.util.Map;

public class GGridController {
    private GGrid grid;
    private CellPanel gridView;
    private GGridTable table;

    public GGridController(GGrid igrid, GFormController iformController, GGroupObjectController igroupObject) {
        grid = igrid;
        gridView = new VerticalPanel();
        table = new GGridTable(iformController, igroupObject);

        ResizeLayoutPanel panel = new ResizeLayoutPanel();
        panel.setStyleName("gridResizePanel");
        panel.setSize("100%", "100%");
        gridView.setSize("100%", "100%");
        panel.setWidget(table);
        gridView.add(panel);

        gridView.setCellHeight(panel, "100%");
        gridView.add(igroupObject.getGridToolbar());
    }

    public GGridTable getTable() {
        return table;
    }

    public void update() {
        table.update();
    }

    public void addToLayout(GFormLayout formLayout) {
        formLayout.add(grid, gridView);
//        formLayout.setTableCellSize(grid.container, gridView, "100%", false);
    }

    public void hide() {
        gridView.setVisible(false);
    }

    public void show() {
        gridView.setVisible(true);
    }

    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updateCellBackgroundValues(propertyDraw, values);
    }

    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updateCellForegroundValues(propertyDraw, values);
    }

    public void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {
        table.updatePropertyCaptions(propertyDraw, values);
    }

    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        table.updateRowBackgroundValues(values);
    }

    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        table.updateRowForegroundValues(values);
    }
}
