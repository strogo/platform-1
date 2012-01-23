package platform.server.logics.property;

import platform.server.classes.ValueClass;

public class StoredDataProperty extends DataProperty {

    public StoredDataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes, value);

        finalizeInit();
    }

    public boolean isStored() {
        return true;
    }
}
