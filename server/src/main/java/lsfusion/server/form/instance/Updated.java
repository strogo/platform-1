package lsfusion.server.form.instance;

import lsfusion.base.FunctionSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;
import java.util.Set;

public interface Updated {

    // изменилось что-то влияющее на isInInterface/getClassSet (класс верхних объектов или класс grid'а)
    boolean classUpdated(ImSet<GroupObjectInstance> gridGroups);
    // изменилось что-то использующее в getExpr конкретные value (один из верхних объектов)
    boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups);
    boolean dataUpdated(ChangedData changedProps, ReallyChanged reallyChanged, Modifier modifier, boolean hidden) throws SQLException, SQLHandledException;
    
    void fillProperties(MSet<CalcProperty> properties);

    boolean isInInterface(GroupObjectInstance classGroup);
}
