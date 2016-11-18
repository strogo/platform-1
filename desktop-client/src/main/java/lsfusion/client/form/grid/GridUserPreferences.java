package lsfusion.client.form.grid;

import lsfusion.client.logics.ClientGroupObject;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.FontInfo;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GridUserPreferences {
    public Map<ClientPropertyDraw, ColumnUserPreferences> columnUserPreferences;
    public FontInfo fontInfo;
    public Integer pageSize;
    public Integer headerHeight;
    public ClientGroupObject groupObject;
    private Boolean hasUserPreferences;
    
    public GridUserPreferences(ClientGroupObject groupObject) {
        this(groupObject, new HashMap<ClientPropertyDraw, ColumnUserPreferences>(), null, null, null, null);
    }
    
    public GridUserPreferences(ClientGroupObject groupObject, Map<ClientPropertyDraw, ColumnUserPreferences> columnUserPreferences,
                               FontInfo fontInfo, Integer pageSize, Integer headerHeight, Boolean hasUserPreferences) {
        this.groupObject = groupObject;
        this.columnUserPreferences = columnUserPreferences;
        this.fontInfo = fontInfo;
        this.pageSize = pageSize;
        this.headerHeight = headerHeight;
        this.hasUserPreferences = hasUserPreferences;
    }
    
    public GridUserPreferences(GridUserPreferences prefs) {
        this(prefs.groupObject, new HashMap<ClientPropertyDraw, ColumnUserPreferences>(), prefs.fontInfo, prefs.pageSize, prefs.headerHeight, prefs.hasUserPreferences);
        
        for (Map.Entry<ClientPropertyDraw, ColumnUserPreferences> entry : prefs.columnUserPreferences.entrySet()) {
            columnUserPreferences.put(entry.getKey(), new ColumnUserPreferences(entry.getValue()));
        }
    }
    
    public boolean hasUserPreferences() {
        return hasUserPreferences != null && hasUserPreferences;   
    }
    
    public void setHasUserPreferences(boolean hasUserPreferences) {
        this.hasUserPreferences = hasUserPreferences;
    }
    
    public Boolean getUserHide(ClientPropertyDraw property) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userHide;
    }

    public String getUserCaption(ClientPropertyDraw property) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userCaption;
    }

    public String getUserPattern(ClientPropertyDraw property) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userPattern;
    }
    
    public Integer getUserWidth(ClientPropertyDraw property) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userWidth;
    }
    
    public Integer getUserOrder(ClientPropertyDraw property) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userOrder;
    }
    
    public Integer getUserSort(ClientPropertyDraw property) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userSort;
    }
    
    public Boolean getUserAscendingSort(ClientPropertyDraw property) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        return prefs.userAscendingSort;
    }
    
    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getHeaderHeight() {
        return headerHeight;
    }
    
    private ColumnUserPreferences ensureColumnPreferences(ClientPropertyDraw property) {
        ColumnUserPreferences prefs = columnUserPreferences.get(property);
        if (prefs == null) {
            prefs = new ColumnUserPreferences(null, null, null, null, null, null, null);
            columnUserPreferences.put(property, prefs);
        }
        return prefs;
    }
    
    public void setUserHide(ClientPropertyDraw property, Boolean userHide) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userHide = userHide;
    }

    public void setUserColumnsSettings(ClientPropertyDraw property, String userCaption, String userPattern, Integer userOrder, Boolean userHide) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userCaption = userCaption;
        prefs.userPattern = userPattern;
        prefs.userOrder = userOrder;
        prefs.userHide = userHide;
    }
    
    public void setUserWidth(ClientPropertyDraw property, Integer userWidth) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userWidth = userWidth;
    }

    public void setUserOrder(ClientPropertyDraw property, Integer userOrder) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userOrder = userOrder;    
    }

    public void setUserSort(ClientPropertyDraw property, Integer userSort) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userSort = userSort;
    }

    public void setUserAscendingSort(ClientPropertyDraw property, Boolean userAscendingSort) {
        ColumnUserPreferences prefs = ensureColumnPreferences(property);
        prefs.userAscendingSort = userAscendingSort;
    }
    
    public void resetPreferences() {
        fontInfo = new FontInfo(null, -1, false, false);
        pageSize = null;
        headerHeight = null;
        hasUserPreferences = false;
        for (ClientPropertyDraw property : new HashSet<>(columnUserPreferences.keySet())) {
            columnUserPreferences.put(property, new ColumnUserPreferences(null, null, null, null, null, null, null));
        }
    }

    public Comparator<ClientPropertyDraw> getUserSortComparator() {
        return  new Comparator<ClientPropertyDraw>() {
            public int compare(ClientPropertyDraw c1, ClientPropertyDraw c2) {
                if(getUserAscendingSort(c1) == null) {
                    return getUserAscendingSort(c2) == null ? 0 : -1;
                } else {
                    return getUserAscendingSort(c2) == null ? 1 : (getUserSort(c1) - getUserSort(c2));
                }
            }
        };
    }
    
    public Comparator<ClientPropertyDraw> getUserOrderComparator() {
        return new Comparator<ClientPropertyDraw>() {
            public int compare(ClientPropertyDraw c1, ClientPropertyDraw c2) {
                if (getUserOrder(c1) == null)
                    return getUserOrder(c2) == null ? 0 : 1;
                else
                    return getUserOrder(c2) == null ? -1 : (getUserOrder(c1) - getUserOrder(c2));
            }
        };
    }
    
    public GroupObjectUserPreferences convertPreferences() {
        Map<String, ColumnUserPreferences> columns = new HashMap<>();
        for (Map.Entry<ClientPropertyDraw, ColumnUserPreferences> entry : columnUserPreferences.entrySet()) {
            columns.put(entry.getKey().getSID(), entry.getValue());
        }
        return new GroupObjectUserPreferences(columns, groupObject.getSID(), fontInfo, pageSize, headerHeight, hasUserPreferences());
    }
}
