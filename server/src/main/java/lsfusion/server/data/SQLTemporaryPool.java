package lsfusion.server.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.data.expr.query.Stat;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

// выделяется в отдельный объект так как синхронизироваться должен
public class SQLTemporaryPool {
    private final Map<FieldStruct, Set<String>> tables = MapFact.mAddRemoveMap();
    private final Map<String, Object> stats = MapFact.mAddRemoveMap();
    private final Map<String, FieldStruct> structs = MapFact.mAddRemoveMap(); // чтобы удалять таблицы, не имея структры
    private int counter = 0;

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public boolean isEmpty() {
        return tables.isEmpty();
    }
    
    public void checkAliveTables(SQLSession session, Map<String, WeakReference<TableOwner>> used) throws SQLException {
        try {
            System.out.println("START " + SQLSession.getCurrentTimeStamp() + " " + session);
            for(Map.Entry<FieldStruct, Set<String>> table : tables.entrySet())
                for(String tab : table.getValue()) {
//                    if(!used.containsKey(tab)) {
                        System.out.println("CHECK "  + SQLSession.getCurrentTimeStamp() + " " + tab + " " + session);
                        session.debugExecute("INSERT INTO " + session.syntax.getSessionTableName(tab) + " SELECT * FROM " + session.syntax.getSessionTableName(tab) + " WHERE 1 > 2");
                    }
            System.out.println("FINISHED " + SQLSession.getCurrentTimeStamp() + " " + session);
        } catch (SQLException e) {
            e = e;
        }
    }

    @AssertSynchronized
    public String getTable(SQLSession session, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count, Map<String, WeakReference<TableOwner>> used, Result<Boolean> isNew, TableOwner owner, OperationOwner opOwner) throws SQLException { //, Map<String, String> usedStacks
        FieldStruct fieldStruct = new FieldStruct(keys, properties, count);

        Set<String> matchTables = tables.get(fieldStruct);
        if(matchTables==null) {
            matchTables = SetFact.mAddRemoveSet();
            tables.put(fieldStruct, matchTables);
        }

        for(String matchTable : matchTables) // ищем нужную таблицу
            if(!used.containsKey(matchTable)) { // если не используется
//                session.truncate(matchTable); // удаляем старые данные
//                if(session.getCount(matchTable, opOwner) != 0) {
//                    ServerLoggers.assertLog(false, "TEMPORARY TABLE NOT EMPTY");
//                    session.truncateSession(matchTable, opOwner, TableOwner.none);
//                }
                assert session.getSessionCount(matchTable, opOwner) == 0;
                assert !used.containsKey(matchTable);
                used.put(matchTable, new WeakReference<TableOwner>(owner));
//                SQLSession.addUsed(matchTable, owner, used, usedStacks);
                session.unlockTemporary();
                isNew.set(false);
                return matchTable;
            }

        // если нет, "создаем" таблицу
        String table = getTableName(counter);
        try {
            session.createTemporaryTable(table, keys, properties, opOwner);
            counter++;
            assert !used.containsKey(table);
            used.put(table, new WeakReference<TableOwner>(owner));
            //        SQLSession.addUsed(table, owner, used, usedStacks);
            matchTables.add(table);
        } finally {
            session.unlockTemporary();
        }
        isNew.set(true);
        structs.put(table, fieldStruct);
        return table;
    }

    public String getTableName(int count) {
        return "t_" + count;
    }

    public void fillData(SQLSession session, FillTemporaryTable data, Integer count, Result<Integer> resultActual, String table, OperationOwner owner) throws SQLException, SQLHandledException {

        Integer actual = data.fill(table); // заполняем
        assert (actual!=null)==(count==null);
        if(session.syntax.supportsAnalyzeSessionTable()) {
            if (Settings.get().isAutoAnalyzeTempStats())
                session.vacuumAnalyzeSessionTable(table, owner);
            else {
                Object actualStatistics = getDBStatistics(actual);
                Object currentStat = stats.get(table);
                if (!actualStatistics.equals(currentStat)) {
                    session.vacuumAnalyzeSessionTable(table, owner);
                    stats.put(table, actualStatistics);
                }
            }
        }
        if(count==null)
            resultActual.set(actual);
        else
            resultActual.set(count);
    }

    public void removeTable(String table) {
        synchronized (tables) {
            if(!Settings.get().isAutoAnalyzeTempStats())
                stats.remove(table);
            FieldStruct fieldStruct = structs.remove(table);
            Set<String> structTables = tables.get(fieldStruct);
            structTables.remove(table);
            if(structTables.isEmpty())
                tables.remove(fieldStruct);
        }
    }

    private static class FieldStruct {

        public final ImOrderSet<KeyField> keys;
        public final ImSet<PropertyField> properties;

        private final Object statistics;

        public FieldStruct(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count) {
            this.keys = keys;
            this.properties = properties;

            if(Settings.get().isAutoAnalyzeTempStats() || count==null)
                this.statistics = null;
            else
                this.statistics = getDBStatistics(count);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof FieldStruct && keys.equals(((FieldStruct) o).keys) && properties.equals(((FieldStruct) o).properties) && BaseUtils.nullEquals(statistics, ((FieldStruct) o).statistics);

        }

        @Override
        public int hashCode() {
            return 31 * (31 * keys.hashCode() + properties.hashCode()) + BaseUtils.nullHash(statistics);
        }
    }

    public static Object getDBStatistics(int count) {
        return new Stat(count);
    }
}
