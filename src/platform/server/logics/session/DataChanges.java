package platform.server.logics.session;

import platform.server.logics.classes.DataClass;
import platform.server.logics.properties.DataProperty;

import java.util.Set;
import java.util.HashSet;

// изменения данных
public class DataChanges {
    public Set<DataProperty> properties = new HashSet<DataProperty>();

    public Set<DataClass> addClasses = new HashSet<DataClass>();
    public Set<DataClass> removeClasses = new HashSet<DataClass>();

    DataChanges copy() {
        DataChanges CopyChanges = new DataChanges();
        CopyChanges.properties.addAll(properties);
        CopyChanges.addClasses.addAll(addClasses);
        CopyChanges.removeClasses.addAll(removeClasses);
        return CopyChanges;
    }

    public boolean hasChanges() {
        return !(properties.isEmpty() && addClasses.isEmpty() && removeClasses.isEmpty());
    }
}
