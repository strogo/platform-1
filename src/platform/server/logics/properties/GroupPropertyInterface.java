package platform.server.logics.properties;

public class GroupPropertyInterface<T extends PropertyInterface> extends PropertyInterface<GroupPropertyInterface<T>> {
    PropertyInterfaceImplement<T> implement;

    public GroupPropertyInterface(int iID,PropertyInterfaceImplement<T> iImplement) {
        super(iID);
        implement =iImplement;
    }
}
