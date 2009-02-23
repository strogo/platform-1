package platform.server.logics.auth;

import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

public class AbstractSecurityPolicy<T> {

    private Set<T> permitted = new HashSet();
    private Set<T> denied = new HashSet();

    boolean defaultPermission = true;

    public void permit(T obj) { permitted.add(obj); }
    public void deny(T obj) { denied.add(obj); }

    public void permit(Collection<? extends T> colObj) { permitted.addAll(colObj); }
    public void deny(Collection<? extends T> colObj) { denied.addAll(colObj); }

    protected void override(AbstractSecurityPolicy<T> policy) {

        for (T obj : policy.denied) {
            permitted.remove(obj);
            denied.add(obj);
        }

        for (T obj : policy.permitted) {
            denied.remove(obj);
            permitted.add(obj);
        }
    }

    public boolean checkPermission(T obj) {

        if (permitted.contains(obj)) return true;
        if (denied.contains(obj)) return false;
        return defaultPermission;
    }
}
