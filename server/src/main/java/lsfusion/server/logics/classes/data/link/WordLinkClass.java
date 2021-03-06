package lsfusion.server.logics.classes.data.link;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;

import java.util.ArrayList;
import java.util.Collection;

public class WordLinkClass extends StaticFormatLinkClass {

    protected String getFileSID() {
        return "WORDLINK";
    }

    private static Collection<WordLinkClass> instances = new ArrayList<>();

    public static WordLinkClass get(boolean multiple) {
        for (WordLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        WordLinkClass instance = new WordLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private WordLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeID() {
        return DataType.WORDLINK;
    }

    @Override
    public String getDefaultCastExtension() {
        return "doc";
    }
}