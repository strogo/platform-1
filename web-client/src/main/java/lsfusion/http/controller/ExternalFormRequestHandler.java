package lsfusion.http.controller;

import lsfusion.base.Pair;
import lsfusion.base.file.IOUtils;
import lsfusion.http.provider.form.FormProvider;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.http.provider.navigator.NavigatorSessionObject;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.session.ExternalUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;

public class ExternalFormRequestHandler extends ExternalRequestHandler {

    @Autowired
    NavigatorProvider logicsAndNavigatorProvider;
    
    @Autowired
    FormProvider formProvider;

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String contentTypeString = request.getContentType();
        ContentType contentType = contentTypeString != null ? ContentType.parse(contentTypeString) : null;

        Charset charset = ExternalUtils.getCharsetFromContentType(contentType);
        String json = new String(IOUtils.readBytesFromStream(request.getInputStream()), charset);
        JSONObject jsonObject = new JSONObject(json);

        String jsonResult;
        String action = jsonObject.getString("action");
        String formID = jsonObject.getString("form");
        
        JSONObject dataObject = jsonObject.optJSONObject("data");
        String data = dataObject != null ? dataObject.toString() : null; 

        if(action.equals("create")) {
            String navigatorID = jsonObject.optString("navigator");
            if(navigatorID.isEmpty())
                navigatorID = "external";
            
            NavigatorSessionObject navigatorSessionObject = logicsAndNavigatorProvider.createOrGetNavigatorSessionObject(navigatorID, sessionObject, request);
            
            Pair<RemoteFormInterface, String> result = navigatorSessionObject.remoteNavigator.createFormExternal(data);
            formProvider.createFormExternal(formID, result.first, navigatorID); // registering form for further usage
            jsonResult = result.second;
        } else {
            FormSessionObject formSessionObject = formProvider.getFormSessionObject(formID);
            if(action.equals("change")) {
                Pair<Long, String> result = formSessionObject.remoteForm.changeExternal(jsonObject.getLong("requestIndex"), jsonObject.getLong("lastReceivedRequestIndex"), data);
                jsonResult = result.second;
            } else {
                formSessionObject.remoteForm.closeExternal();
                formProvider.removeFormSessionObject(formID);
                jsonResult = "{}";
            }
        }

        sendResponse(response, jsonResult, charset);
    }
}
