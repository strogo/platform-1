package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.QuickMap;
import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.cases.CaseWhereInterface;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.expr.where.GreaterWhere;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.expr.where.LikeWhere;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.ClassReader;
import platform.server.data.where.CheckWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.HashMap;
import java.util.Map;


public abstract class BaseExpr extends Expr {

    public static Expr create(BaseExpr expr) {
        if(expr.getWhere().getClassWhere().isFalse())
            return NULL;
        else
            return expr;
    }

    // получает список ExprCase'ов
    public ExprCaseList getCases() {
        return new ExprCaseList(this);
    }

    protected abstract VariableExprSet calculateExprFollows();
    private VariableExprSet exprFollows = null;
    @ManualLazy
    public VariableExprSet getExprFollows() {
        if(exprFollows==null)
            exprFollows = calculateExprFollows();
        return exprFollows;
    }


    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        fillAndJoinWheres(joins, andWhere.and(getWhere()));
    }

    public ClassReader getReader(KeyType keyType) {
        return getType(keyType); // assert'ится что не null
    }

    public abstract BaseExpr translateOuter(MapTranslate translator);

    public abstract void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    public Expr followFalse(Where where, boolean pack) {
        if(getWhere().means(where))
            return NULL;
        else
            if(pack)
                return packFollowFalse(where);
            else
                return this;
    }

    // для linear'ов делает followFalse, известно что не means(where)
    public Expr packFollowFalse(Where where) {
        return this;
    }

    public abstract ClassExprWhere getClassWhere(AndClassSet classes);

    public Where compare(final Expr expr, final Compare compare) {
        if(expr instanceof BaseExpr) {
            switch(compare) {
                case EQUALS:
                    return EqualsWhere.create(this,(BaseExpr)expr);
                case GREATER:
                    return GreaterWhere.create(this,(BaseExpr)expr);
                case GREATER_EQUALS:
                    return GreaterWhere.create(this,(BaseExpr)expr).or(EqualsWhere.create(this,(BaseExpr)expr));
                case LESS:
                    return GreaterWhere.create((BaseExpr)expr,this);
                case LESS_EQUALS:
                    return GreaterWhere.create((BaseExpr)expr,this).or(EqualsWhere.create(this,(BaseExpr)expr));
                case NOT_EQUALS: // оба заданы и не равно
                    return getWhere().and(expr.getWhere()).and(EqualsWhere.create(this,(BaseExpr)expr).not());
                case LIKE:
                    return LikeWhere.create(this, (BaseExpr)expr); 
            }
            throw new RuntimeException("should not be");
        } else {
            return expr.getCases().getWhere(new CaseWhereInterface<BaseExpr>() {
                public Where getWhere(BaseExpr cCase) {
                    return compare(cCase,compare);
                }
/*                @Override
                public Where getElse() {
                    if(compare==Compare.EQUALS || compare==Compare.NOT_EQUALS)
                        return Where.FALSE;
                    else // если не equals то нас устроит и просто не null
                        return BaseExpr.this.getWhere();
                }*/
            });
        }
    }

    public BaseExpr scale(int coeff) {
        if(coeff==1) return this;

        LinearOperandMap map = new LinearOperandMap();
        map.add(this,coeff);
        return map.getExpr();
    }

    public Expr sum(BaseExpr expr) {
        if(getWhere().means(expr.getWhere().not())) // если не пересекаются то возвращаем case
            return nvl(expr);
        
        LinearOperandMap map = new LinearOperandMap();
        map.add(this,1);
        map.add(expr,1);
        return map.getExpr();
    }

    public Expr sum(Expr expr) {
        if(expr instanceof BaseExpr)
            return sum((BaseExpr)expr);
        else
            return expr.sum(this);
    }

    public boolean hasKey(KeyExpr key) {
        return enumKeys(this).contains(key);
    }

    // может возвращать null, оба метода для ClassExprWhere
    public abstract AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and);
    public abstract boolean addAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and, AndClassSet add);

    public static <K> Map<K, Expr> packFollowFalse(Map<K, BaseExpr> mapExprs, Where falseWhere) {
        Map<K, Expr> result = new HashMap<K, Expr>();
        for(Map.Entry<K,BaseExpr> groupExpr : mapExprs.entrySet()) {
            CheckWhere siblingWhere = Where.TRUE;
            for(Map.Entry<K,BaseExpr> sibling : mapExprs.entrySet())
                if(!BaseUtils.hashEquals(sibling.getKey(), groupExpr.getKey())) {
                    Expr siblingExpr = result.get(sibling.getKey());
                    if(siblingExpr==null) siblingExpr = sibling.getValue();
                    siblingWhere = siblingWhere.andCheck(siblingExpr.getWhere());
                }
            result.put(groupExpr.getKey(), groupExpr.getValue().packFollowFalse((Where) falseWhere.andCheck(siblingWhere)));
        }
        return result;
    }
}
