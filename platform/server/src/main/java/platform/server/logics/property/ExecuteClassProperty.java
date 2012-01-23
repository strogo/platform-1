package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.Map;
import java.util.Set;

public abstract class ExecuteClassProperty extends ExecuteProperty {

    public ExecuteClassProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    @Override
    protected void fillDepends(Set<Property> depends, boolean derived) {
        super.fillDepends(depends, derived);
        depends.add(getIsClassProperty().property);
    }

    @IdentityLazy
    private PropertyImplement<?, ClassPropertyInterface> getIsClassProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    protected QuickSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return getIsClassProperty().property.getUsedChanges(propChanges);
    }

    protected abstract Expr getValueExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement);
    
    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getValueExpr(joinImplement).and(getIsClassProperty().mapExpr(joinImplement, propChanges, changedWhere).getWhere());
    }
}
