package platform.server.data.sql;

import platform.base.BaseUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MSSQLDataAdapter extends DataAdapter {

    public MSSQLDataAdapter(String iDataBase, String iServer, String iUserID, String iPassword) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(iDataBase, iServer, iUserID, iPassword);
    }

    public String getLongType() {
        return "bigint";
    }

    public int updateModel() {
        return 1;
    }

    public boolean allowViews() {
        return true;
    }

    public String getUpdate(String tableString, String setString, String fromString, String whereString) {
        return tableString + setString + " FROM " + fromString + whereString;
    }

    public String getClassName() {
        return "net.sourceforge.jtds.jdbc.Driver";
    }

    public void ensureDB() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        //namedPipe=true;
        Connection connect = DriverManager.getConnection("jdbc:jtds:sqlserver://"+ server +":1433;User=" + userID + ";Password=" + password);
        try {
//        try {
//            connect.createStatement().execute("DROP DATABASE "+ dataBase);
//        } catch (Exception e) {
//
//        }
            connect.createStatement().execute("CREATE DATABASE "+ dataBase);
        } catch(Exception e) {
        }
        connect.close();
    }

    public Connection startConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        //namedPipe=true;
        Connection connect = DriverManager.getConnection("jdbc:jtds:sqlserver://"+ server +":1433;User=" + userID + ";Password=" + password);
        connect.createStatement().execute("USE "+ dataBase);

        return connect;
    }

    public String isNULL(String expr1, String expr2, boolean notSafe) {
        if(notSafe)
            return "CASE WHEN "+ expr1 +" IS NULL THEN "+ expr2 +" ELSE "+ expr1 +" END";
        else
            return "ISNULL("+ expr1 +","+ expr2 +")";
    }

    public String getCreateSessionTable(String tableName, String declareString, String constraintString) {
        return "CREATE TABLE #"+ tableName +" ("+ declareString +","+ constraintString +")";
    }

    public String getSessionTableName(String tableName) {
        return "#"+ tableName;
    }

    public boolean isNullSafe() {
        return false;
    }

    public boolean isGreatest() {
        return false;
    }

    public String getSelect(String from, String exprs, String where, String orderBy, String groupBy, String top) {
        return "SELECT" + BaseUtils.clause("TOP", top) + " " + exprs + " FROM " + from + BaseUtils.clause("WHERE", where) + BaseUtils.clause("GROUP BY", groupBy) + BaseUtils.clause("ORDER BY", orderBy);
    }

    public String getUnionOrder(String union, String orderBy, String top) {
        if(top.length()==0)
            return union + BaseUtils.clause("ORDER BY", orderBy);
        return "SELECT" + BaseUtils.clause("TOP", top) + " * FROM (" + union + ")" + BaseUtils.clause("ORDER BY", orderBy);
    }

    @Override
    public String getByteArrayType() {
        return "varbinary";
    }

    public String getCommandEnd() {
        return ";";
    }
}
