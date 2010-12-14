package platform.server.form.instance;

import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.CustomClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.session.ChangesSession;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;
import java.lang.ref.WeakReference;

public class CustomObjectInstance extends ObjectInstance {

    public CustomClass baseClass;
    CustomClass gridClass;

    public ConcreteCustomClass currentClass;

    private WeakReference<CustomClassListener> weakClassListener;

    public CustomClassListener getClassListener() {
        return weakClassListener.get();        
    }

    public void setClassListener(CustomClassListener classListener) {
        this.weakClassListener = new WeakReference<CustomClassListener>(classListener);
    }

    public boolean isAddOnTransaction() {
        return entity.addOnTransaction;
    }

    public CustomObjectInstance(ObjectEntity entity, CustomClass baseClass) {
        super(entity);
        this.baseClass = baseClass;
        gridClass = baseClass;
    }

    public CustomClass getBaseClass() {
        return baseClass;
    }

    public AndClassSet getClassSet(Set<GroupObjectInstance> gridGroups) {
        if(objectInGrid(gridGroups))
            return getGridClass().getUpSet();
        else
            return getCurrentClass();
    }

    public ConcreteObjectClass getCurrentClass() {
        if(currentClass==null) // нету объекта
            return baseClass.getBaseClass().unknown;
        else
            return currentClass;
    }

    public CustomClass getGridClass() {
        return gridClass;
    }

    ObjectValue value = NullValue.instance;

    public void changeValue(ChangesSession session, ObjectValue changeValue) throws SQLException {
        if(changeValue.equals(value)) return;

        value = changeValue;

        updateCurrentClass(session);

        updated = updated | ObjectInstance.UPDATED_OBJECT;
        groupTo.updated = groupTo.updated | GroupObjectInstance.UPDATED_OBJECT;
    }

    public void refreshValueClass(ChangesSession session) throws SQLException {
        value = value.refresh(session);
        updateCurrentClass(session);
    }

    public void updateCurrentClass(ChangesSession session) throws SQLException {
        // запишем класс объекта
        ConcreteCustomClass changeClass;
        if(value instanceof NullValue)
            changeClass = null;
        else {
            ConcreteClass sessionClass = session.getCurrentClass(getDataObject());
            if(!(sessionClass instanceof ConcreteCustomClass)) {
                groupTo.dropSeek(this);
                return;
            }
            changeClass = (ConcreteCustomClass) sessionClass;
            CustomClassListener classListener = getClassListener();
            if (classListener != null) // если вообще кто-то следит за изменением классов объектов
                classListener.objectChanged(changeClass, (Integer) getDataObject().object);
        }

        if(changeClass != currentClass) {
            currentClass = changeClass;
            updated = updated | ObjectInstance.UPDATED_CLASS;
        }
    }

    public boolean classChanged(Collection<CustomClass> changedClasses) {
        return changedClasses.contains(gridClass);
    }

    public boolean classUpdated(Set<GroupObjectInstance> gridGroups) {
        if(objectInGrid(gridGroups))
            return (updated & ObjectInstance.UPDATED_CLASS)!=0;
        else
            return (updated & ObjectInstance.UPDATED_GRIDCLASS)!=0;
    }

    public boolean isInInterface(GroupObjectInstance group) {
        return groupTo == group || value instanceof DataObject; // если не в классовом виде то только если не null
    }

    public ObjectValue getObjectValue() {
        return value;
    }

    public void changeClass(ChangesSession session, DataObject change, int classID) throws SQLException {

        // запишем объекты, которые надо будет сохранять
        if(classID==-1) {
            session.changeClass(change,null);
            groupTo.dropSeek(this);
        } else {
            session.changeClass(change, baseClass.findConcreteClassID(classID));
            updateCurrentClass(session);
        }
    }

    public void changeGridClass(int classID) {

        CustomClass changeClass = baseClass.findClassID(classID);

        if(gridClass != changeClass) {
            gridClass = changeClass;

            // расставляем пометки
            updated |= ObjectInstance.UPDATED_GRIDCLASS;
            groupTo.updated |= GroupObjectInstance.UPDATED_GRIDCLASS;
        }
    }

    public Type getType() {
        return ObjectType.instance;
    }
}
