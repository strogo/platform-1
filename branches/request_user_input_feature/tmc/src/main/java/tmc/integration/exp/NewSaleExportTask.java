package tmc.integration.exp;

import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.filter.NotFilterInstance;
import platform.server.form.instance.filter.NotNullFilterInstance;
import tmc.VEDBusinessLogics;

import java.sql.SQLException;

public class NewSaleExportTask extends AbstractSaleExportTask {

    protected NewSaleExportTask(VEDBusinessLogics BL, String path, Integer store) {
        super(BL, path, store);
    }

    protected String getDbfName() {
        return "datacur.dbf";
    }

    protected void setRemoteFormFilter(FormInstance formInstance) {
        PropertyDrawInstance<?> exported = formInstance.getPropertyDraw(BL.VEDLM.checkRetailExported);
        exported.toDraw.addTempFilter(new NotFilterInstance(new NotNullFilterInstance(exported.propertyObject)));
    }

    protected void updateRemoteFormProperties(FormInstance formInstance) throws SQLException {
        PropertyDrawInstance propertyDraw = formInstance.getPropertyDraw(BL.VEDLM.checkRetailExported);
        formInstance.changeProperty(propertyDraw.propertyObject, true, propertyDraw.toDraw);
    }
}
