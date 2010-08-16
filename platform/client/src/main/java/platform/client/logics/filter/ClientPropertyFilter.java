package platform.client.logics.filter;

import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.ClientValueLink;
import platform.interop.Compare;
import platform.interop.FilterType;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPropertyFilter {

    public ClientPropertyDraw property;
    public ClientValueLink value;

    public Compare compare;

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(FilterType.COMPARE);

        outStream.writeInt(property.getID());
        compare.serialize(outStream);
        value.serialize(outStream);
    }    
}
