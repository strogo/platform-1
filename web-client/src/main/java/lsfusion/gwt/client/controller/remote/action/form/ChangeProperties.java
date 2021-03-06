package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.io.Serializable;

public class ChangeProperties extends FormRequestCountingAction<ServerResponseResult>{
    public int[] propertyIds;
    public GGroupObjectValue[] fullKeys;
    public Serializable[] values;
    public Long[] addedObjectsIds;

    public ChangeProperties() {
    }

    public ChangeProperties(int[] propertyIds, GGroupObjectValue[] fullKeys, Serializable[] values, Long[] addedObjectsIds) {
        this.propertyIds = propertyIds;
        this.fullKeys = fullKeys;
        this.values = values;
        this.addedObjectsIds = addedObjectsIds;
    }
}
