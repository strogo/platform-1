package platform.server.logics.classes;

import platform.server.data.types.Type;

public class LongClass extends IntegralClass {
    LongClass(Integer iID, String caption) {super(iID, caption);}

    public Type getType() {
        return Type.longType;
    }
}
