package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MaxUnionProperty extends IncrementUnionProperty {

    private final Collection<PropertyMapImplement<?,Interface>> operands;

    @Override
    protected boolean useSimpleIncrement() {
        return true;
    }

    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        throw new RuntimeException("not supported"); // используется simple increment
    }

    @Override
    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        throw new RuntimeException("not supported"); // используется simple increment
    }

    @Override
    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {

        Expr result = null;
        for(PropertyMapImplement<?, Interface> operand : operands) {
            Expr operandExpr = operand.mapExpr(joinImplement, propChanges, changedWhere);
            if(result==null)
                result = operandExpr;
            else
                result = result.max(operandExpr);
        }
        return result;
    }

    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return operands;
    }

    public MaxUnionProperty(String sID, String caption, List<Interface> interfaces, Collection<PropertyMapImplement<?, Interface>> operands) {
        super(sID, caption, interfaces);
        this.operands = operands;

        finalizeInit();
    }
}
