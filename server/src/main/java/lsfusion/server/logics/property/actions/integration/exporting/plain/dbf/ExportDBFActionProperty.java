package lsfusion.server.logics.property.actions.integration.exporting.plain.dbf;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.JDBFException;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.interop.FormExportType;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormSelector;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectSelector;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportPlainActionProperty;
import lsfusion.server.logics.property.actions.integration.exporting.plain.ExportPlainWriter;

import java.io.IOException;

public class ExportDBFActionProperty<O extends ObjectSelector> extends ExportPlainActionProperty<O> {

    public ExportDBFActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormExportType staticType, LCP singleExportFile, ImMap<GroupObjectEntity, LCP> exportFiles, String charset) {
        super(caption, form, objectsToSet, nulls, staticType, singleExportFile, exportFiles, charset);
    }

    @Override
    protected ExportPlainWriter getWriter(ImOrderMap<String, Type> fieldTypes) throws IOException {
        try {
            return new ExportDBFWriter(fieldTypes, charset);
        } catch (JDBFException e) {
            throw Throwables.propagate(e);
        }
    }
}