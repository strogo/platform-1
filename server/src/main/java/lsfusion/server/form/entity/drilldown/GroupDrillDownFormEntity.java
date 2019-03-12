package lsfusion.server.form.entity.drilldown;

import lsfusion.base.BaseUtils;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MRevMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.OrderEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

public class GroupDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<GroupProperty.Interface<I>, GroupProperty<I>> {

    private PropertyDrawEntity implPropertyDraw;
    private GroupObjectEntity detailsGroup;

    public GroupDrillDownFormEntity(String canonicalName, LocalizedString caption, GroupProperty<I> property, LogicsModule LM) {
        super(canonicalName, caption, property, LM);
    }

    @Override
    protected void setupDrillDownForm() {
        Version version = LM.getVersion();

        ImMap<I, GroupProperty.Interface<I>> byInnerInterfaces = BaseUtils.immutableCast(
                property.getMapInterfaces().toRevMap(property.getReflectionOrderInterfaces()).filterFnValuesRev(new SFunctionSet<CalcPropertyInterfaceImplement<I>>() {
                    @Override
                    public boolean contains(CalcPropertyInterfaceImplement<I> element) {
                        return element instanceof PropertyInterface;
                    }
                }).reverse()
        );

        detailsGroup = new GroupObjectEntity(genID(), "");

        ImMap<I, ValueClass> innerClasses = property.getInnerInterfaceClasses();
        MRevMap<I, ObjectEntity> mInnerObjects = MapFact.mRevMap();
        MAddSet<ObjectEntity> usedObjects = SetFact.mAddSet();

        for (int i = 0; i < innerClasses.size(); ++i) {
            ValueClass innerIntClass = innerClasses.getValue(i);
            I innerInterface = innerClasses.getKey(i);

            ObjectEntity innerObject = null;
            GroupProperty.Interface<I> byInterface = byInnerInterfaces.get(innerInterface);
            if (byInterface != null) {
                innerObject = interfaceObjects.get(byInterface);
            } 
            if(innerObject == null || usedObjects.add(innerObject)) {
                innerObject = new ObjectEntity(genID(), innerIntClass, LocalizedString.NONAME, innerIntClass == null);
                detailsGroup.add(innerObject);

                addPropertyDraw(LM.baseLM.getObjValueProp(this, innerObject), version, innerObject);
                addPropertyDraw(innerObject, version, LM.getRecognizeGroup());
            }

            mInnerObjects.revAdd(innerInterface, innerObject);
        }
        addGroupObject(detailsGroup, version);

        ImRevMap<I, ObjectEntity> innerObjects = mInnerObjects.immutableRev();
        
        //добавляем основные свойства
        ImList<CalcPropertyInterfaceImplement<I>> groupImplements = property.getProps();
        for (CalcPropertyInterfaceImplement<I> groupImplement : groupImplements) {
            if (groupImplement instanceof CalcPropertyMapImplement) {
                CalcPropertyMapImplement<PropertyInterface, I> mapImplement = (CalcPropertyMapImplement<PropertyInterface, I>) groupImplement;
                ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(innerObjects).mapping;

                addFixedFilter(new FilterEntity(addPropertyObject(mapImplement.property, mapImplMapping)), version);
                if (mapImplement.property.isDrillFull()) {
                    addPropertyDraw(mapImplement.property, mapImplMapping, version);
                }
            }
        }

        //добавляем BY свойства
        ImMap<GroupProperty.Interface<I>, CalcPropertyInterfaceImplement<I>> mapInterfaces = property.getMapInterfaces();
        for (int i = 0; i < mapInterfaces.size(); ++i) {
            GroupProperty.Interface<I> groupInterface = mapInterfaces.getKey(i);
            CalcPropertyInterfaceImplement<I> groupImplement = mapInterfaces.getValue(i);

            if (groupImplement instanceof CalcPropertyMapImplement || !innerObjects.containsKey((I) groupImplement)) {
                CalcPropertyRevImplement filterProp = DerivedProperty.createCompare(groupImplement, (PropertyInterface) groupInterface, Compare.EQUALS).mapRevImplement(MapFact.addRevExcl(innerObjects, groupInterface, interfaceObjects.get(groupInterface)));
                addFixedFilter(new FilterEntity(addPropertyObject(filterProp)), version);

                if(groupImplement instanceof CalcPropertyMapImplement) {
                    CalcPropertyMapImplement<PropertyInterface, I> mapImplement = (CalcPropertyMapImplement<PropertyInterface, I>) groupImplement;
                    ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(innerObjects).mapping;
                    if (mapImplMapping.size() != 1 || !LM.getRecognizeGroup().hasChild(mapImplement.property)) {
                        if (mapImplement.property.isDrillFull()) {
                            addPropertyDraw(mapImplement.property, mapImplMapping, version);
                        }
                    }
                }
            }
        }

        // добавляем порядки
        ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders = property.getOrders();
        for (int i = 0; i < orders.size(); ++i) {
            CalcPropertyInterfaceImplement<I> orderImplement = orders.getKey(i);
            Boolean asc = orders.getValue(i);

            OrderEntity orderEntity;
            if (orderImplement instanceof CalcPropertyMapImplement) {
                CalcPropertyMapImplement<PropertyInterface, I> mapImplement = (CalcPropertyMapImplement<PropertyInterface, I>) orderImplement;
                ImRevMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapRevImplement(innerObjects).mapping;
                orderEntity = addPropertyObject(mapImplement.property, mapImplMapping);
            } else {
                I innerInterface = (I) orderImplement;
                orderEntity = innerObjects.get(innerInterface);
            }

            addFixedOrder(orderEntity, asc != null && asc, version);
        }

        implPropertyDraw = addPropertyDraw(property, interfaceObjects, version);
    }

    @Override
    public FormView createDefaultRichDesign(Version version) {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign(version);

        detailsContainer.add(design.getBoxContainer(detailsGroup), version);

        valueContainer.add(design.get(implPropertyDraw), version);

        return design;
    }
}
