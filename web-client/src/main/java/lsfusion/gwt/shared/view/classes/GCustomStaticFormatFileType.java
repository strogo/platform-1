package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.shared.GwtSharedUtils;

public class GCustomStaticFormatFileType extends GFileType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeCustomStaticFormatFileCaption() + ": " + GwtSharedUtils.toString(",", extensions.toArray());
    }
}