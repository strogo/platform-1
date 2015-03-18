package lsfusion.server.data.expr.query;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.InnerExprFollows;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.Where;

public class PartitionJoin extends QueryJoin<KeyExpr, PartitionJoin.Query, PartitionJoin, PartitionJoin.QueryOuterContext> {

    public static class Query extends QueryJoin.Query<KeyExpr, Query> {
        private final Where where;
        private final ImSet<Expr> partitions;

        public Query(InnerExprFollows<KeyExpr> follows, Where where, ImSet<Expr> partitions) {
            super(follows);
            this.where = where;
            this.partitions = partitions;
        }

        public Query(Query query, MapTranslate translate) {
            super(query, translate);
            where = query.where.translateOuter(translate);
            partitions = translate.translate(query.partitions);
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && partitions.equals(((Query) o).partitions) && where.equals(((Query) o).where);
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hashContext) {
            return (31 * super.hash(hashContext) + hashOuter(partitions, hashContext)) * 31 + where.hashOuter(hashContext);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return super.calculateOuterDepends().merge(SetFact.<OuterContext>merge(partitions, where));
        }
    }
    
    @IdentityLazy
    public Where getOrWhere() {
        return query.where.mapWhere(group);
    }

    public PartitionJoin(ImSet<KeyExpr> keys, ImSet<Value> values, InnerExprFollows<KeyExpr> innerFollows, Where inner, ImSet<Expr> partitions, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, new Query(innerFollows, inner, partitions), group);
    }

    private PartitionJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Query inner, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected PartitionJoin createThis(ImSet<KeyExpr> keys, ImSet<Value> values, Query query, ImMap<KeyExpr, BaseExpr> group) {
        return new PartitionJoin(keys, values, query, group);
    }

    public static class QueryOuterContext extends QueryJoin.QueryOuterContext<KeyExpr, PartitionJoin.Query, PartitionJoin, PartitionJoin.QueryOuterContext> {
        public QueryOuterContext(PartitionJoin thisObj) {
            super(thisObj);
        }

        public PartitionJoin translateThis(MapTranslate translator) {
            return new PartitionJoin(thisObj, translator);
        }
    }
    protected QueryOuterContext createOuterContext() {
        return new QueryOuterContext(this);
    }

    private PartitionJoin(PartitionJoin partitionJoin, MapTranslate translator) {
        super(partitionJoin, translator);
    }

    public StatKeys<KeyExpr> getStatKeys(KeyStat keyStat) {
        return query.where.getFullStatKeys(keys);
    }

    public Where getWhere() {
        return query.where;
    }

    public ImSet<Expr> getPartitions() {
        return query.partitions;
    }
}
