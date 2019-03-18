package lsfusion.client.form.property.classes.renderer.link;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

public class CSVLinkPropertyRenderer extends LinkPropertyRenderer {

    public CSVLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon("csv"));
        }
        super.setValue(value);
    }
}