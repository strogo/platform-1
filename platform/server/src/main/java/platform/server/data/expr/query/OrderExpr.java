package platform.server.data.expr.query;

import platform.server.data.expr.*;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.query.*;
import platform.server.data.type.Type;
import platform.server.caches.MapContext;
import platform.server.caches.HashContext;
import platform.server.caches.TranslateContext;
import platform.server.caches.AbstractTranslateContext;

import java.util.*;

public class OrderExpr extends QueryExpr<KeyExpr, OrderExpr.Query,OrderJoin> implements JoinData {

    public static class Query extends AbstractTranslateContext<Query> {
        public Expr expr;
        public List<Expr> orders;
        public Set<Expr> partitions;

        public Query(Expr expr, List<Expr> orders, Set<Expr> partitions) {
            this.expr = expr;
            this.orders = orders;
            this.partitions = partitions;
        }

        public Query translateDirect(KeyTranslator translator) {
            return new Query(expr.translateDirect(translator),translator.translate(orders),translator.translate(partitions));
        }

        public int hashContext(HashContext hashContext) {
            int hash = 0;
            for(Expr order : orders)
                hash = hash * 31 + order.hashContext(hashContext);
            hash = hash * 31;
            for(Expr partition : partitions)
                hash += partition.hashContext(hashContext);
            return hash * 31 + expr.hashContext(hashContext);
        }

        public Where getWhere() {
            return expr.getWhere().and(Expr.getWhere(orders)).and(Expr.getWhere(partitions));
        }

        public Type getType() {
            return expr.getType(getWhere());
        }

        @Override
        public boolean equals(Object obj) {
            return this==obj || obj instanceof Query && expr.equals(((Query) obj).expr) && orders.equals(((Query) obj).orders) && partitions.equals(((Query) obj).partitions);
        }

        @Override
        public String toString() {
            return "INNER(" + expr + "," + orders + "," + partitions + ")";
        }

        public SourceJoin[] getEnum() { // !!! Включим ValueExpr.TRUE потому как в OrderSelect.getSource - при проталкивании partition'а может создать TRUE
            return AbstractSourceJoin.merge(partitions,AbstractSourceJoin.merge(orders, expr, ValueExpr.TRUE));
        }
    }

    private OrderExpr(Map<KeyExpr,BaseExpr> group, Expr expr, List<Expr> orders, Set<Expr> partitions) {
        super(new Query(expr, orders, partitions),group);
    }

    public DataWhereSet getFollows() {
        return InnerExpr.getExprFollows(group);
    }

    // трансляция
    private OrderExpr(OrderExpr orderExpr,KeyTranslator translator) {
        super(orderExpr, translator);
    }

    public VariableClassExpr translateDirect(KeyTranslator translator) {
        return new OrderExpr(this,translator);
    }

    public Type getType(Where where) {
        return query.getType();
    }

    public Where getFullWhere() {
        return query.getWhere();
    }

    public Where calculateWhere() {
        return getFullWhere().map(group);
    }

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    @Override
    public String toString() {
        return "ORDER(" + query + "," + group + ")";
    }

    public final static boolean pushWhere = false; 

    @Override
    public OrderJoin getGroupJoin() {
        return new OrderJoin(getKeys(),getValues(),query.getWhere(),pushWhere?query.partitions:new HashSet<Expr>(),group);
    }

    protected Expr create(Map<KeyExpr, BaseExpr> group, Query query) {
        return createBase(group, query.expr, query.orders, query.partitions);
    }

    protected static Expr createBase(Map<KeyExpr, BaseExpr> group, Expr expr, List<Expr> orders, Set<Expr> partitions) {
        // проверим если в group есть ключи которые ссылаются на ValueExpr и они есть в partition'е - убираем их из partition'а
        Map<KeyExpr,BaseExpr> restGroup = new HashMap<KeyExpr, BaseExpr>();
        Set<Expr> restPartitions = new HashSet<Expr>(partitions);
        Map<KeyExpr,BaseExpr> translate = new HashMap<KeyExpr, BaseExpr>();
        for(Map.Entry<KeyExpr,BaseExpr> groupKey : group.entrySet())
            if(groupKey.getValue().isValue() && restPartitions.remove(groupKey.getKey()))
                translate.put(groupKey.getKey(), groupKey.getValue());
            else
                restGroup.put(groupKey.getKey(), groupKey.getValue());
        if(translate.size()>0) {
            QueryTranslator translator = new QueryTranslator(translate,false);
            expr = expr.translateQuery(translator);
            orders = translator.translate(orders);
            restPartitions = translator.translate(restPartitions);
        }

        return BaseExpr.create(new OrderExpr(restGroup,expr,orders,restPartitions));
    }

    public static Expr create(Expr expr, List<Expr> orders, Set<Expr> partitions, Map<KeyExpr, ? extends Expr> group) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<KeyExpr> mapCase : CaseExpr.pullCases(group))
            result.add(mapCase.where, createBase(mapCase.data, expr, orders,partitions));
        return result.getExpr();
    }
}
