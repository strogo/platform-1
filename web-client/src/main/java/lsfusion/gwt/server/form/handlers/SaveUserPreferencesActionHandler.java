package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.gwt.shared.actions.form.SaveUserPreferencesAction;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.view.GColumnUserPreferences;
import lsfusion.gwt.shared.view.GGroupObjectUserPreferences;
import lsfusion.interop.form.user.ColumnUserPreferences;
import lsfusion.interop.form.user.GroupObjectUserPreferences;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class SaveUserPreferencesActionHandler extends FormServerResponseActionHandler<SaveUserPreferencesAction> {
    private final static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public SaveUserPreferencesActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(SaveUserPreferencesAction action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        GGroupObjectUserPreferences gGroupObjectUP = action.groupObjectUserPreferences;
        
        HashMap<String, ColumnUserPreferences> columnUPMap = new HashMap<>();
        for (Map.Entry<String, GColumnUserPreferences> entry : gGroupObjectUP.getColumnUserPreferences().entrySet()) {
            GColumnUserPreferences gColumnUP = entry.getValue();
            columnUPMap.put(entry.getKey(), new ColumnUserPreferences(gColumnUP.userHide, gColumnUP.userCaption, gColumnUP.userPattern, gColumnUP.userWidth, gColumnUP.userOrder, gColumnUP.userSort, gColumnUP.userAscendingSort));
        }
        GroupObjectUserPreferences groupObjectUP = new GroupObjectUserPreferences(columnUPMap, gGroupObjectUP.getGroupObjectSID(), gwtConverter.convertFont(gGroupObjectUP.getFont()), gGroupObjectUP.getPageSize(), gGroupObjectUP.getHeaderHeight(), gGroupObjectUP.hasUserPreferences());

        return getServerResponseResult(form, form.remoteForm.saveUserPreferences(action.requestIndex, defaultLastReceivedRequestIndex, groupObjectUP, action.forAllUsers, action.completeOverride, action.hiddenProps));
    }
}
