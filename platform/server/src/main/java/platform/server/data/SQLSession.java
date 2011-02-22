package platform.server.data;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.MutableObject;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.SQLExecute;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseInterface;
import platform.server.data.type.Reader;
import platform.server.data.type.Type;
import platform.server.data.type.TypeObject;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.lang.ref.WeakReference;
import java.sql.*;
import java.util.*;

public class SQLSession extends MutableObject {
    private final static Logger logger = Logger.getLogger(SQLSession.class);

    public SQLSyntax syntax;

    private ConnectionPool connectionPool;

    private Connection getConnection() throws SQLException {
        return privateConnection !=null ? privateConnection : connectionPool.getCommon(this);
    }

    private void returnConnection(Connection connection) throws SQLException {
        if(privateConnection !=null)
            assert privateConnection == connection;
        else
            connectionPool.returnCommon(this, connection);
    }

    private Connection privateConnection = null;

    public final static String userParam = "adsadaweewuser";
    public final static String isServerRestartingParam = "sdfisserverrestartingpdfdf";
    public final static String sessionParam = "dsfreerewrewrsf";
    public final static String computerParam = "fjruwidskldsor";

    public SQLSession(DataAdapter adapter) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        syntax = adapter;
        connectionPool = adapter;
    }

    private void needPrivate() throws SQLException { // получает unique connection
        if(privateConnection ==null)
            privateConnection = connectionPool.getPrivate(this);
    }

    private void tryCommon() throws SQLException { // пытается вернуться к
        removeUnusedTemporaryTables();
        if(!inTransaction && sessionTablesMap.isEmpty()) { // вернемся к commonConnection'у
            connectionPool.returnPrivate(this, privateConnection);
            privateConnection = null;
        }
    }

    private boolean inTransaction;

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void startTransaction() throws SQLException {
        needPrivate();

        privateConnection.setAutoCommit(false);
        inTransaction = true;
    }

    private void endTransaction() throws SQLException {
        privateConnection.setAutoCommit(true);
        inTransaction = false;

        tryCommon();
    }

    public void rollbackTransaction() throws SQLException {
        privateConnection.rollback();
        endTransaction();
    }

    public void commitTransaction() throws SQLException {
        privateConnection.commit();
        endTransaction();
    }

    // удостоверивается что таблица есть
    public void ensureTable(Table table) throws SQLException {
        Connection connection = getConnection();

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, table.name, new String[]{"TABLE"});
        if (!tables.next()) {
            createTable(table.name, table.keys);
            for (PropertyField property : table.properties)
                addColumn(table.name, property);
        }

        returnConnection(connection);
    }

    public void addExtraIndices(String table, List<KeyField> keys) throws SQLException {
        List<String> keyStrings = new ArrayList<String>();
        for(KeyField key : keys)
            keyStrings.add(key.name);
        for(int i=1;i<keys.size();i++)
            addIndex(table, keyStrings.subList(i, keys.size()));
    }

    private String getConstraintName(String table) {
        return "PK_" + table;
    }

    private String getConstraintDeclare(String table, List<KeyField> keys) {
        String keyString = "";
        for (KeyField key : keys)
            keyString = (keyString.length() == 0 ? "" : keyString + ',') + key.name;
        return "CONSTRAINT " + getConstraintName(table) + " PRIMARY KEY " + syntax.getClustered() + " (" + keyString + ")";
    }

    public void createTable(String table, List<KeyField> keys) throws SQLException {
        logger.info("Идет создание таблицы " + table + "... ");
        String createString = "";
        for (KeyField key : keys)
            createString = (createString.length() == 0 ? "" : createString + ',') + key.getDeclare(syntax);
        if (createString.length() == 0)
            createString = "dumb integer";
        else
            createString = createString + "," + getConstraintDeclare(table, keys);

//        System.out.println("CREATE TABLE "+Table.Name+" ("+CreateString+")");
        execute("CREATE TABLE " + table + " (" + createString + ")");
        addExtraIndices(table, keys);                
        logger.info(" Done");
    }

    public void dropTable(String table) throws SQLException {
        logger.info("Идет удаление таблицы " + table + "... ");
        execute("DROP TABLE " + table);
        logger.info(" Done");
    }

    static String getIndexName(String table, Collection<String> fields) {
        String name = table + "_idx";
        for (String indexField : fields)
            name = name + "_" + indexField;
        return name;
    }

    public void addIndex(String table, List<String> fields) throws SQLException {
        logger.info("Идет создание индекса " + getIndexName(table, fields) + "... ");
        String columns = "";
        for (String indexField : fields)
            columns = (columns.length() == 0 ? "" : columns + ",") + indexField;

        execute("CREATE INDEX " + getIndexName(table, fields) + " ON " + table + " (" + columns + ")");
        logger.info(" Done");
    }

    public void dropIndex(String table, Collection<String> fields) throws SQLException {
        logger.info("Идет удаление индекса " + getIndexName(table, fields) + "... ");
        execute("DROP INDEX " + getIndexName(table, fields));
        logger.info(" Done");
    }

    public void addKeyColumns(String table, Map<KeyField, Object> fields, List<KeyField> keys) throws SQLException {
        if(fields.isEmpty())
            return;

        logger.info("Идет добавление ключа " + table + "." + fields + "... ");

        String constraintName = getConstraintName(table);
        String tableName = syntax.getSessionTableName(table);
        String addCommand = ""; String dropDefaultCommand = "";
        for(Map.Entry<KeyField, Object> field : fields.entrySet()) {
            addCommand = addCommand + "ADD COLUMN " + field.getKey().getDeclare(syntax) + " DEFAULT " + field.getKey().type.getString(field.getValue(), syntax) + ",";
            dropDefaultCommand = (dropDefaultCommand.length()==0?"":dropDefaultCommand + ",") + " ALTER COLUMN " + field.getKey().name + " DROP DEFAULT";
        }

        execute("ALTER TABLE " + tableName + " " + addCommand + " ADD " + getConstraintDeclare(table, keys) +
                (keys.size()==fields.size()?"":", DROP CONSTRAINT " + constraintName));
        execute("ALTER TABLE " + tableName + " " + dropDefaultCommand);

        logger.info(" Done");
    }

    public void addTemporaryColumn(String table, PropertyField field) throws SQLException {
        addColumn(table, field);
//        execute("CREATE INDEX " + "idx_" + table + "_" + field.name + " ON " + table + " (" + field.name + ")"); //COLUMN
    }

    public void addColumn(String table, PropertyField field) throws SQLException {
        logger.info("Идет добавление колонки " + table + "." + field.name + "... ");
        execute("ALTER TABLE " + table + " ADD " + field.getDeclare(syntax)); //COLUMN
        logger.info(" Done");
    }

    public void dropColumn(String table, String field) throws SQLException {
        logger.info("Идет удаление колонки " + table + "." + field + "... ");
        execute("ALTER TABLE " + table + " DROP COLUMN " + field);
        logger.info(" Done");
    }

    public void modifyColumn(String table, PropertyField field, Type oldType) throws SQLException {
        logger.info("Идет изменение типа колонки " + table + "." + field.name + "... ");
        execute("ALTER TABLE " + table + " ALTER COLUMN " + field.name + " TYPE " +
                field.type.getDB(syntax) + " " + syntax.typeConvertSuffix(oldType, field.type, field.name));
        logger.info(" Done");
    }

    public void packTable(Table table) throws SQLException {
        logger.info("Идет упаковка таблицы " + table + "... ");
        String dropWhere = "";
        for (PropertyField property : table.properties)
            dropWhere = (dropWhere.length() == 0 ? "" : dropWhere + " AND ") + property.name + " IS NULL";
        execute("DELETE FROM " + table.getName(syntax) + (dropWhere.length() == 0 ? "" : " WHERE " + dropWhere));
        logger.info(" Done");
    }

    private final Map<String, WeakReference<Object>> sessionTablesMap = new HashMap<String, WeakReference<Object>>();
    private int sessionCounter = 0;
    
    public String createTemporaryTable(List<KeyField> keys, Collection<PropertyField> properties, Object owner) throws SQLException {
        needPrivate();

        removeUnusedTemporaryTables();

        synchronized(sessionTablesMap) {
            String name = "t_" + (sessionCounter++);

            String createString = "";
            for (KeyField key : keys)
                createString = (createString.length() == 0 ? "" : createString + ',') + key.getDeclare(syntax);
            for (PropertyField prop : properties)
                createString = (createString.length() == 0 ? "" : createString + ',') + prop.getDeclare(syntax);
            if (keys.size()>0)
                createString = createString + "," + getConstraintDeclare(name, keys);
            if (createString.length() == 0)
                createString = "dumb integer";
            execute(syntax.getCreateSessionTable(name, createString));

            sessionTablesMap.put(name, new WeakReference<Object>(owner));
            return name;
        }
    }

    public void vacuumSessionTable(String table) throws SQLException {
        if(inTransaction)
            execute("ANALYZE " + table);
        else
            execute("VACUUM ANALYZE " + table);
    }

    public void vacuumTemporaryTables() throws SQLException {
        synchronized (sessionTablesMap) {
            removeUnusedTemporaryTables();

            for(String sessionTable : sessionTablesMap.keySet())
                vacuumSessionTable(sessionTable);
        }

    }

    private void removeUnusedTemporaryTables() throws SQLException {
        synchronized (sessionTablesMap) {
            for (Iterator<Map.Entry<String, WeakReference<Object>>> iterator = sessionTablesMap.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, WeakReference<Object>> entry = iterator.next();
                if (entry.getValue().get() == null) {
                    dropTemporaryTableFromDB(entry.getKey());
                    iterator.remove();
                }
            }
        }
    }

    private void dropTemporaryTableFromDB(String tableName) throws SQLException {
        execute(syntax.getDropSessionTable(tableName));
    }

    public void dropTemporaryTable(SessionTable table, Object owner) throws SQLException {
        synchronized (sessionTablesMap) {
            assert sessionTablesMap.containsKey(table.name);
            WeakReference<Object> removed = sessionTablesMap.remove(table.name);
            assert removed.get()==owner;

            dropTemporaryTableFromDB(table.name);
        }

        tryCommon();
    }

    private void execute(String executeString) throws SQLException {
        Connection connection = getConnection();

        logger.info(executeString);

        Statement statement = connection.createStatement();
        try {
            statement.execute(executeString);
        } catch (SQLException e) {
            logger.info(statement.toString());
            throw e;
        } finally {
            statement.close();

            returnConnection(connection);
        }
    }

    private int executeDML(SQLExecute execute) throws SQLException {
        return executeDML(execute.command, execute.params);
    }

    private int executeDML(String command, Map<String, ParseInterface> paramObjects) throws SQLException {
        Connection connection = getConnection();

        PreparedStatement statement = getStatement(command, paramObjects, connection, syntax);

        int result;
        try {
            result = statement.executeUpdate();
        } catch (SQLException e) {
            logger.info(statement.toString());
            throw e;
        } finally {
            statement.close();

            returnConnection(connection);
        }

        return result;
    }

    public <K,V> OrderedMap<Map<K, Object>, Map<V, Object>> executeSelect(String select, Map<String, ParseInterface> paramObjects, Map<K, String> keyNames, Map<K, ? extends Reader> keyReaders, Map<V, String> propertyNames, Map<V, ? extends Reader> propertyReaders) throws SQLException {
        Connection connection = getConnection();

        logger.info(select);

        OrderedMap<Map<K,Object>,Map<V,Object>> execResult = new OrderedMap<Map<K, Object>, Map<V, Object>>();
        PreparedStatement statement = getStatement(select, paramObjects, connection, syntax);
        try {
            ResultSet result = statement.executeQuery();
            try {
                while(result.next()) {
                    Map<K,Object> rowKeys = new HashMap<K, Object>();
                    for(Map.Entry<K,String> key : keyNames.entrySet())
                        rowKeys.put(key.getKey(), keyReaders.get(key.getKey()).read(result.getObject(key.getValue())));
                    Map<V,Object> rowProperties = new HashMap<V, Object>();
                    for(Map.Entry<V,String> property : propertyNames.entrySet())
                        rowProperties.put(property.getKey(),
                                propertyReaders.get(property.getKey()).read(result.getObject(property.getValue())));
                     execResult.put(rowKeys,rowProperties);
                }
            } finally {
                result.close();
            }
        } finally {
            statement.close();

            returnConnection(connection);
        }

        return execResult;
    }

    private void insertParamRecord(Table table, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields) throws SQLException {
        String insertString = "";
        String valueString = "";

        int paramNum = 0;
        Map<String, ParseInterface> params = new HashMap<String, ParseInterface>();

        // пробежим по KeyFields'ам
        for (KeyField key : table.keys) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + key.name;
            DataObject keyValue = keyFields.get(key);
            if (keyValue.isString(syntax))
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + keyValue.getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + prm;
                params.put(prm, new TypeObject(keyValue));
            }
        }

        for (Map.Entry<PropertyField, ObjectValue> fieldValue : propFields.entrySet()) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + fieldValue.getKey().name;
            if (fieldValue.getValue().isString(syntax))
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + fieldValue.getValue().getString(syntax);
            else {
                String prm = "qxprm" + (paramNum++) + "nx";
                valueString = (valueString.length() == 0 ? "" : valueString + ',') + prm;
                params.put(prm, new TypeObject((DataObject) fieldValue.getValue()));
            }
        }

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        executeDML("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") VALUES (" + valueString + ")", params);
    }

    public void insertRecord(Table table, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields) throws SQLException {

        boolean needParam = false;

        for (Map.Entry<KeyField, DataObject> keyField : keyFields.entrySet())
            if (!keyField.getKey().type.isSafeString(keyField.getValue())) {
                needParam = true;
            }

        for (Map.Entry<PropertyField, ObjectValue> fieldValue : propFields.entrySet())
            if (!fieldValue.getKey().type.isSafeString(fieldValue.getValue())) {
                needParam = true;
            }

        if (needParam) {
            insertParamRecord(table, keyFields, propFields);
            return;
        }

        String insertString = "";
        String valueString = "";

        // пробежим по KeyFields'ам
        for (KeyField key : table.keys) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + key.name;
            valueString = (valueString.length() == 0 ? "" : valueString + ',') + keyFields.get(key).getString(syntax);
        }

        // пробежим по Fields'ам
        for (PropertyField prop : propFields.keySet()) {
            insertString = (insertString.length() == 0 ? "" : insertString + ',') + prop.name;
            valueString = (valueString.length() == 0 ? "" : valueString + ',') + propFields.get(prop).getString(syntax);
        }

        if(insertString.length()==0) {
            assert valueString.length()==0;
            insertString = "dumb";
            valueString = "0";
        }

        execute("INSERT INTO " + table.getName(syntax) + " (" + insertString + ") VALUES (" + valueString + ")");
    }

    public boolean isRecord(Table table, Map<KeyField, DataObject> keyFields) throws SQLException {

        // по сути пустое кол-во ключей
        Query<KeyField, String> query = new Query<KeyField, String>(keyFields.keySet());
        query.putKeyWhere(keyFields);

        // сначала закинем KeyField'ы и прогоним Select
        query.and(table.joinAnd(query.mapKeys).getWhere());

        return query.execute(this).size() > 0;
    }

    public void ensureRecord(Table table, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields) throws SQLException {
        if (!isRecord(table, keyFields))
            insertRecord(table, keyFields, propFields);
    }

    public void updateInsertRecord(Table table, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields) throws SQLException {

        if (isRecord(table, keyFields)) {
            if(!propFields.isEmpty()) {
                Query<KeyField, PropertyField> updateQuery = new Query<KeyField, PropertyField>(table);
                updateQuery.putKeyWhere(keyFields);
                updateQuery.properties.putAll(ObjectValue.getMapExprs(propFields));

                // есть запись нужно Update лупить
                updateRecords(new ModifyQuery(table, updateQuery));
            }
        } else
            // делаем Insert
            insertRecord(table, keyFields, propFields);
    }

    public Object readRecord(Table table, Map<KeyField, DataObject> keyFields, PropertyField field) throws SQLException {
        // по сути пустое кол-во ключей
        Query<KeyField, String> query = new Query<KeyField, String>(keyFields.keySet());

        // сначала закинем KeyField'ы и прогоним Select
        Expr fieldExpr = table.joinAnd(query.mapKeys).getExpr(field);
        query.putKeyWhere(keyFields);
        query.properties.put("result", fieldExpr);
        query.and(fieldExpr.getWhere());
        OrderedMap<Map<KeyField, Object>, Map<String, Object>> result = query.execute(this);
        if (result.size() > 0)
            return result.singleValue().get("result");
        else
            return null;
    }

    public void deleteKeyRecords(Table table, Map<KeyField, ?> keys) throws SQLException {
        String deleteWhere = "";
        for (Map.Entry<KeyField, ?> deleteKey : keys.entrySet())
            deleteWhere = (deleteWhere.length() == 0 ? "" : deleteWhere + " AND ") + deleteKey.getKey().name + "=" + deleteKey.getValue();

        execute("DELETE FROM " + table.getName(syntax) + (deleteWhere.length() == 0 ? "" : " WHERE " + deleteWhere));
    }

    public int updateRecords(ModifyQuery modify) throws SQLException {
        return executeDML(modify.getUpdate(syntax));
    }

    public int insertSelect(ModifyQuery modify) throws SQLException {
        return executeDML(modify.getInsertSelect(syntax));
    }

    // сначала делает InsertSelect, затем UpdateRecords
    public int modifyRecords(ModifyQuery modify) throws SQLException {
        if (modify.table.isSingle()) {// потому как запросом никак не сделаешь, просто вкинем одну пустую запись
            if (!isRecord(modify.table, new HashMap<KeyField, DataObject>()))
                insertSelect(modify);
        } else
            executeDML(modify.getInsertLeftKeys(syntax));
        return updateRecords(modify);
    }

    public void close() throws SQLException {
        if(privateConnection !=null)
            privateConnection.close();
    }

    private static PreparedStatement getStatement(String command, Map<String, ParseInterface> paramObjects, Connection connection, SQLSyntax syntax) throws SQLException {

        char[][] params = new char[paramObjects.size()][];
        ParseInterface[] values = new ParseInterface[params.length];
        int paramNum = 0;
        for (Map.Entry<String, ParseInterface> param : paramObjects.entrySet()) {
            params[paramNum] = param.getKey().toCharArray();
            values[paramNum++] = param.getValue();
        }

        // те которые isString сразу транслируем
        List<ParseInterface> preparedParams = new ArrayList<ParseInterface>();
        char[] toparse = command.toCharArray();
        String parsedString = "";
        char[] parsed = new char[toparse.length + params.length * 100];
        int num = 0;
        for (int i = 0; i < toparse.length;) {
            int charParsed = 0;
            for (int p = 0; p < params.length; p++) {
                if (BaseUtils.startsWith(toparse, i, params[p])) { // нашли
                    if (values[p].isSafeString()) { // если можно вручную пропарсить парсим
                        parsedString = parsedString + new String(parsed, 0, num) + values[p].getString(syntax);
                        parsed = new char[toparse.length - i + (params.length - p) * 100];
                        num = 0;
                    } else {
                        parsed[num++] = '?';
                        if (!values[p].isSafeType()) {
                            String castString = "::" + values[p].getDBType(syntax);
                            System.arraycopy(castString.toCharArray(), 0, parsed, num, castString.length());
                            num += castString.length();
                        }
                        preparedParams.add(values[p]);
                    }
                    charParsed = params[p].length;
                    break;
                }
            }
            if (charParsed == 0) {
                parsed[num++] = toparse[i];
                charParsed = 1;
            }
            i = i + charParsed;
        }
        parsedString = parsedString + new String(parsed, 0, num);

        logger.info(parsedString);

        PreparedStatement statement = connection.prepareStatement(parsedString);
        paramNum = 1;
        for (ParseInterface param : preparedParams)
            param.writeParam(statement, paramNum++, syntax);

        return statement;
    }

    // вспомогательные методы
    public static String stringExpr(Map<String, String> keySelect, Map<String, String> propertySelect) {
        String expressionString = "";
        for (Map.Entry<String, String> key : keySelect.entrySet())
            expressionString = (expressionString.length() == 0 ? "" : expressionString + ",") + key.getValue() + " AS " + key.getKey();
        for (Map.Entry<String, String> property : propertySelect.entrySet())
            expressionString = (expressionString.length() == 0 ? "" : expressionString + ",") + property.getValue() + " AS " + property.getKey();
        if (expressionString.length() == 0)
            expressionString = "0";
        return expressionString;
    }

    public static <T> OrderedMap<String, String> mapNames(Map<T, String> exprs, Map<T, String> names, List<T> order) {
        OrderedMap<String, String> result = new OrderedMap<String, String>();
        if (order.isEmpty())
            for (Map.Entry<T, String> name : names.entrySet()) {
                result.put(name.getValue(), exprs.get(name.getKey()));
                order.add(name.getKey());
            }
        else // для union all
            for (T expr : order)
                result.put(names.get(expr), exprs.get(expr));
        return result;
    }

    public void vacuumSessionTable(SessionTable table) throws SQLException {
        vacuumSessionTable(table.getName(syntax));
    }
}
