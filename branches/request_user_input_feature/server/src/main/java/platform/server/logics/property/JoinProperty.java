package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.server.classes.CustomClass;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.*;

import java.util.*;

public class JoinProperty<T extends PropertyInterface> extends SimpleIncrementProperty<JoinProperty.Interface> {
    public final PropertyImplement<T, PropertyInterfaceImplement<Interface>> implement;

    public static class Interface extends PropertyInterface<Interface> {
        Interface(int ID) {
            super(ID);
        }
    }

    public static List<Interface> getInterfaces(int intNum) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new Interface(i));
        return interfaces;
    }

    public JoinProperty(String sID, String caption, List<Interface> interfaces, boolean implementChange, PropertyImplement<T, PropertyInterfaceImplement<Interface>> implement) {
        super(sID, caption, interfaces);
        this.implement = implement;
        this.implementChange = implementChange;

        finalizeInit();
    }

    private Map<T, Expr> getJoinImplements(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getJoinImplements(joinImplement, false, propChanges, changedWhere);
    }

    private Map<T, Expr> getJoinImplements(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Map<T, Expr> result = new HashMap<T, Expr>();
        for(Map.Entry<T,PropertyInterfaceImplement<Interface>> interfaceImplement : implement.mapping.entrySet())
            result.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapExpr(joinImplement, propClasses, propChanges, changedWhere));
        return result;
    }

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return implement.property.getExpr(getJoinImplements(joinImplement, propClasses, propChanges, changedWhere), propClasses, propChanges, changedWhere);
    }

    @Override
    public void fillDepends(Set<Property> depends, boolean events) {
        fillDepends(depends,implement.mapping.values());
        depends.add(implement.property);       
    }

    // разрешить менять основное свойство
    public final boolean implementChange;
    
    @Override
    protected QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            QuickSet<Property> result = new QuickSet<Property>();
            for(Property<?> property : getDepends()) {
                result.addAll(property.getUsedDataChanges(propChanges));
                result.addAll(property.getUsedChanges(propChanges));
            }
            return result;
        }

        if(implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            Set<Property> depends = new HashSet<Property>();
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface)
                    implement.mapping.get(andInterface).mapFillDepends(depends);
            Set<Property> implementDepends = new HashSet<Property>();
            implement.mapping.get(andProperty.objectInterface).mapFillDepends(implementDepends);
            return QuickSet.add(propChanges.getUsedDataChanges(implementDepends), propChanges.getUsedChanges(depends));
        }

        if(implement.property.isOnlyNotZero)
            return ((PropertyMapImplement<?,Interface>)BaseUtils.singleValue(implement.mapping)).property.getUsedDataChanges(propChanges);

        if(implementChange) {
            Set<Property> implementProps = new HashSet<Property>();
            fillDepends(implementProps,implement.mapping.values());
            return QuickSet.add(implement.property.getUsedDataChanges(propChanges), propChanges.getUsedChanges(implementProps));
        }
        if(implement.mapping.size()==1 && !implementChange && implement.property.aggProp) {
            // пока тупо MGProp'им назад
            return QuickSet.add(((PropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping)).property.getUsedDataChanges(propChanges), implement.property.getUsedChanges(propChanges));
        }

        return super.calculateUsedDataChanges(propChanges);
    }

    private static MapDataChanges<Interface> getDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges, PropertyInterfaceImplement<Interface> changeImp, PropertyInterfaceImplement<Interface> valueImp) {
        Map<Interface, Expr> mapExprs = change.getMapExprs();
        Expr toChangeExpr = valueImp.mapExpr(mapExprs, propChanges);
        Where toChangeWhere = change.expr.getWhere();
        return changeImp.mapJoinDataChanges(mapExprs, toChangeExpr.and(toChangeWhere), // меняем на новое значение, если надо и скидываем в null если было какое-то
                change.where.and(toChangeWhere.or(toChangeExpr.compare(changeImp.mapExpr(mapExprs, propChanges),Compare.EQUALS))), changedWhere, propChanges);
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            assert implement.mapping.size()==2;
            Iterator<PropertyInterfaceImplement<Interface>> i = implement.mapping.values().iterator();
            PropertyInterfaceImplement<Interface> op1 = i.next();
            PropertyInterfaceImplement<Interface> op2 = i.next();

            // сначала первый на второй пытаемся изменить, затем для оставшихся второй на первый второй
            WhereBuilder changedWhere1 = new WhereBuilder();
            MapDataChanges<Interface> result1 = getDataChanges(change, changedWhere1, propChanges, op1, op2);
            if(changedWhere!=null) changedWhere.add(changedWhere1.toWhere());

            return result1.add(getDataChanges(change.and(changedWhere1.toWhere().not()), changedWhere, propChanges, op2, op1));
        }

        if(implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            Map<Interface, Expr> mapExprs = change.getMapExprs();
            Where where = Where.TRUE;
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface) {
                    Where andWhere = implement.mapping.get(andInterface).mapExpr(mapExprs, propChanges).getWhere();
                    if(((AndFormulaProperty.AndInterface)andInterface).not)
                        andWhere = andWhere.not();
                    where = where.and(andWhere);
                }
            return implement.mapping.get(andProperty.objectInterface).mapJoinDataChanges(mapExprs, change.expr, change.where.and(where), changedWhere, propChanges);
        }

        if(implement.property.isOnlyNotZero)
            return ((PropertyMapImplement<?,Interface>)BaseUtils.singleValue(implement.mapping)).mapDataChanges(change, changedWhere, propChanges);

        if(implementChange) { // groupBy'им выбирая max
            Map<T, Interface> mapInterfaces = new HashMap<T, Interface>();
            for(Map.Entry<T,PropertyInterfaceImplement<Interface>> interfaceMap : implement.mapping.entrySet())
                if(interfaceMap.getValue() instanceof Interface)
                    mapInterfaces.put(interfaceMap.getKey(), (Interface) interfaceMap.getValue());
            return implement.property.getJoinDataChanges(getJoinImplements(change.getMapExprs(), propChanges, null), change.expr, change.where, propChanges, changedWhere).map(mapInterfaces);
        }
        if(implement.mapping.size()==1 && !implementChange && implement.property.aggProp) {
            // пока тупо MGProp'им назад
            PropertyMapImplement<?, Interface> implementSingle = (PropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping);
            KeyExpr keyExpr = new KeyExpr("key");
            Expr groupExpr = GroupExpr.create(Collections.singletonMap(0, implement.property.getExpr(Collections.singletonMap(BaseUtils.single(implement.property.interfaces), keyExpr), propChanges)),
                    keyExpr, keyExpr.isClass(implementSingle.property.getCommonClasses().value.getUpSet()), GroupType.ANY, Collections.singletonMap(0, change.expr));
            return implementSingle.mapDataChanges(
                    new PropertyChange<Interface>(change, groupExpr), changedWhere, propChanges);
        }

        return super.calculateDataChanges(change, changedWhere, propChanges);
    }

    @Override
    public PropertyMapImplement<ClassPropertyInterface, Interface> getDefaultEditAction(String editActionSID, Property filterProperty) {
        if(implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            List<PropertyInterfaceImplement<Interface>> ands = new ArrayList<PropertyInterfaceImplement<Interface>>();
            List<Boolean> nots = new ArrayList<Boolean>();
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface) {
                    ands.add(implement.mapping.get(andInterface));
                    nots.add(((AndFormulaProperty.AndInterface)andInterface).not);
                }
            return DerivedProperty.createIfAction(interfaces, DerivedProperty.createAnd(interfaces, DerivedProperty.<Interface>createStatic(true, LogicalClass.instance), ands, nots),
                    implement.mapping.get(andProperty.objectInterface).mapEditAction(editActionSID, filterProperty), null, false);
        }
        if(implement.mapping.size()==1 && !implementChange) {
            // тут вообще надо что=то типа с join'ить (assertion что filterProperty с одним интерфейсом)
            return BaseUtils.singleValue(implement.mapping).mapEditAction(editActionSID, implement.property);
        }
        return super.getDefaultEditAction(editActionSID, filterProperty);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public boolean checkEquals() {
        if (implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProp = (AndFormulaProperty) implement.property;
            PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>> andImplement
                    = (PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>>) implement;

            PropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof PropertyMapImplement) {
                return ((PropertyMapImplement) objectIface).property.checkEquals();
            }
        }

        return implement.property.checkEquals();
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<Interface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        if (implement.mapping.size() == 1 && ((PropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping)).property instanceof ObjectClassProperty) {
            PropertyObjectInterfaceEntity mapObject = BaseUtils.singleValue(entity.propertyObject.mapping);
            if (mapObject instanceof ObjectEntity && !((CustomClass) ((ObjectEntity) mapObject).baseClass).hasChildren())
                entity.forceViewType = ClassViewType.HIDE;
        }
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);

        if (implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProp = (AndFormulaProperty) implement.property;
            PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>> andImplement
                    = (PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>>) implement;

            PropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof PropertyMapImplement) {
                ((PropertyMapImplement) objectIface).property.proceedDefaultDesign(propertyView, view);
            }
        }
    }
}
