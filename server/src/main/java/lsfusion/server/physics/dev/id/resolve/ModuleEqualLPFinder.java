package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.property.LP;
import lsfusion.server.logics.LogicsModule;

public class ModuleEqualLPFinder extends ModuleEqualLAPFinder<LP<?>> {
    protected final boolean findLocals;

    public ModuleEqualLPFinder(boolean findLocals) {
        this.findLocals = findLocals;
    }

    @Override
    public boolean isFiltered(LP<?> property) {
        return (!findLocals && property.property.isLocal());
    }

    @Override
    protected Iterable<LP<?>> getSourceList(LogicsModule module, String name) {
        return module.getNamedProperties(name);
    }
}
