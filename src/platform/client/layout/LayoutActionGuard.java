package platform.client.layout;

import bibliothek.gui.dock.action.ActionGuard;
import bibliothek.gui.dock.action.DefaultDockActionSource;
import bibliothek.gui.dock.action.LocationHint;
import bibliothek.gui.dock.action.DockActionSource;
import bibliothek.gui.DockController;
import bibliothek.gui.Dockable;

// подкидывает действия стандартные типа закрытия
class LayoutActionGuard implements ActionGuard {

    DefaultDockActionSource Source;

    LayoutActionGuard(DockController Controller) {
        Source = new DefaultDockActionSource(
                new LocationHint( LocationHint.ACTION_GUARD, LocationHint.RIGHT_OF_ALL));
        Source.add(new CloseAction(Controller));
    }

    public boolean react(Dockable dockable) {
        // заинтересованы в своих
        return (dockable instanceof FormDockable);
    }

    public DockActionSource getSource(Dockable dockable) {
        return Source;
    }
}
