package platform.server.classes;

import platform.interop.Data;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.ParseException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.text.NumberFormat;

public class LongClass extends IntegralClass<Long> {

    public final static LongClass instance = new LongClass();
    private final static String sid = "LongClass";
    static {
        DataClass.storeClass(sid, instance);
    }

    public String toString() {
        return "Большое целое число";
    }

    public Class getJavaClass() {
        return Long.class;
    }

    public byte getTypeID() {
        return Data.LONG;
    }

    int getWhole() {
        return 10;
    }

    int getPrecision() {
        return 0;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getLongType();
    }

    public Long read(Object value) {
        if(value==null) return null;
        return ((Number)value).longValue();
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setLong(num, (Long)value);
    }

    public Object getDefaultValue() {
        return 0l;
    }

    public Format getDefaultFormat() {
        return NumberFormat.getIntegerInstance();
    }

    public Object parseString(String s) throws ParseException {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            throw new ParseException("error parsing long", e);
        }
    }

    public String getSID() {
        return sid;
    }
}
