package platform.server.form.entity;

import platform.base.BaseUtils;
import platform.base.identity.IdentityObject;
import platform.server.classes.ValueClass;
import platform.server.data.type.TypeSerializer;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public class ObjectEntity extends IdentityObject implements PropertyObjectInterfaceEntity, ServerIdentitySerializable {

    public GroupObjectEntity groupTo;

    public String caption;
    public String getCaption() {
        return !BaseUtils.isRedundantString(caption)?caption:baseClass.toString();
    }

    public boolean addOnTransaction = false;
    public boolean resetOnApply = false;

    public ValueClass baseClass;

    public ObjectEntity() {

    }
    
    public ObjectEntity(int ID, ValueClass baseClass, String caption) {
        this(ID, null, baseClass, caption);
    }

    public ObjectEntity(int ID, String sID, ValueClass baseClass, String caption) {
        super(ID);
        this.sID = sID;
        this.caption = caption;
        this.baseClass = baseClass;
    }

    public PropertyObjectInterfaceInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void fillObjects(Set<ObjectEntity> objects) {
        objects.add(this);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupTo);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        groupTo = (GroupObjectEntity) pool.deserializeObject(inStream);
        baseClass = TypeSerializer.deserializeValueClass(pool.context.BL, inStream);
        caption = pool.readString(inStream);
    }

    public PropertyObjectInterfaceEntity getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return this == oldObject
                ? newObject
                : getInstance(instanceFactory).getDataObject();
    }
}
