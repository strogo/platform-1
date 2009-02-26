package platform.server.logics.properties.linear;

import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;

import java.util.ArrayList;
import java.util.List;

public class LP<T extends PropertyInterface,P extends Property<T>> {

    LP(P iProperty) {
        this(iProperty, new ArrayList<T>());
    }

    public LP(P iProperty, List<T> iListInterfaces) {
        property =iProperty;
        listInterfaces = iListInterfaces;
    }

    public P property;
    public List<T> listInterfaces;
}
