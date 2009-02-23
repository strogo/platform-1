package platform.server.view.form;

import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.PropertyImplement;
import platform.server.logics.properties.Property;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.DataClass;
import platform.server.logics.session.ChangeValue;
import platform.server.logics.session.DataSession;
import platform.server.logics.ObjectValue;
import platform.server.logics.auth.ChangePropertySecurityPolicy;
import platform.server.data.query.exprs.SourceExpr;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class PropertyObjectImplement<P extends PropertyInterface> extends PropertyImplement<ObjectImplement,P> {

    PropertyObjectImplement(PropertyObjectImplement<P> iProperty) { super(iProperty); }
    public PropertyObjectImplement(Property<P> iProperty) {super(iProperty);}

    // получает Grid в котором рисоваться
    public GroupObjectImplement getApplyObject() {
        GroupObjectImplement ApplyObject=null;
        for(ObjectImplement IntObject : mapping.values())
            if(ApplyObject==null || IntObject.groupTo.Order>ApplyObject.Order) ApplyObject = IntObject.groupTo;

        return ApplyObject;
    }

    // получает класс значения
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        InterfaceClass<P> ClassImplement = new InterfaceClass<P>();
        for(P Interface : property.interfaces) {
            ObjectImplement IntObject = mapping.get(Interface);
            ClassSet ImpClass;
            if(IntObject.groupTo ==ClassGroup)
                if(IntObject.gridClass ==null)
                    throw new RuntimeException("надо еще думать");
                else
                    ImpClass = new ClassSet(IntObject.gridClass);//ClassSet.getUp(IntObject.GridClass);
            else
                if(IntObject.Class==null)
                    return new ClassSet();
                else
                    ImpClass = new ClassSet(IntObject.Class);
            ClassImplement.put(Interface,ImpClass);
        }

        return property.getValueClass(ClassImplement);
    }

    // в интерфейсе
    boolean isInInterface(GroupObjectImplement ClassGroup) {
        return !getValueClass(ClassGroup).isEmpty();
    }

    // проверяет на то что изменился верхний объект
    boolean objectUpdated(GroupObjectImplement ClassGroup) {
        for(ObjectImplement IntObject : mapping.values())
            if(IntObject.groupTo !=ClassGroup && ((IntObject.updated & ObjectImplement.UPDATED_OBJECT)!=0)) return true;

        return false;
    }

    // изменился хоть один из классов интерфейса (могло повлиять на вхождение в интерфейс)
    boolean classUpdated(GroupObjectImplement ClassGroup) {
        for(ObjectImplement IntObject : mapping.values())
            if(((IntObject.updated & ((IntObject.groupTo ==ClassGroup)?ObjectImplement.UPDATED_CLASS:ObjectImplement.UPDATED_CLASS)))!=0) return true;

        return false;
    }

    public ChangeValue getChangeProperty(DataSession Session, ChangePropertySecurityPolicy securityPolicy) {
        Map<P,ObjectValue> Interface = new HashMap<P,ObjectValue>();
        for(Map.Entry<P, ObjectImplement> Implement : mapping.entrySet())
            Interface.put(Implement.getKey(),new ObjectValue(Implement.getValue().idObject,Implement.getValue().Class));

        return property.getChangeProperty(Session,Interface,1,securityPolicy);
    }

    SourceExpr getSourceExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session) {

        Map<P, SourceExpr> JoinImplement = new HashMap<P,SourceExpr>();
        for(P Interface : property.interfaces)
            JoinImplement.put(Interface, mapping.get(Interface).getSourceExpr(ClassGroup,ClassSource));

        InterfaceClass<P> JoinClasses = new InterfaceClass<P>();
        for(Map.Entry<P, ObjectImplement> Implement : mapping.entrySet()) {
            ClassSet Classes;
            if(ClassGroup!=null && ClassGroup.contains(Implement.getValue().groupTo)) {
                DataClass ImplementClass = Implement.getValue().gridClass;
                Classes = ClassSet.getUp(ImplementClass);
                ClassSet AddClasses = Session.addChanges.get(ImplementClass);
                if(AddClasses!=null)
                    Classes.or(AddClasses);
            } else {
                DataClass ImplementClass = Session.BaseClasses.get(Implement.getValue().idObject);
                if(ImplementClass==null) ImplementClass = Implement.getValue().Class;
                // чего не должно быть
                if(ImplementClass==null)
                    Classes = new ClassSet();
                else
                    Classes = new ClassSet(ImplementClass);
            }
            JoinClasses.put(Implement.getKey(),Classes);
        }

        // если есть не все интерфейсы и есть изменения надо с Full Join'ить старое с новым
        // иначе как и было
        return Session.getSourceExpr(property,JoinImplement,new InterfaceClassSet<P>(JoinClasses));
    }
}
