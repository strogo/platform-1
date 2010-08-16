package platform.server.form.instance.filter;

import platform.server.data.expr.Expr;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.Updated;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.form.instance.ObjectInstance;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public interface CompareValue extends Updated {

//    AndClassSet getValueClass(GroupObjectInstance ClassGroup) {return null;}

    Expr getExpr(Set<GroupObjectInstance> classGroup, Map<ObjectInstance, ? extends Expr> classSource, Modifier<? extends Changes> modifier) throws SQLException;
}
