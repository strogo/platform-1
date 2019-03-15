package lsfusion.server.logics.action;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.ListCaseAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.changed.ChangedProperty;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.action.session.changed.SessionProperty;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.sets.AndClassSet;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.event.*;
import lsfusion.server.logics.form.interactive.action.change.GroupChangeActionProperty;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.property.ActionClassImplement;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassDataProperty;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.ObjectClassProperty;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.infer.AlgType;
import lsfusion.server.logics.property.infer.ClassType;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.*;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class Action<P extends PropertyInterface> extends ActionOrProperty<P> {
    //просто для быстрого доступа
    private static final ActionPropertyDebugger debugger = ActionPropertyDebugger.getInstance();

    private boolean newDebugStack; // только для "top-level" action
    private ParamDebugInfo<P> paramInfo; // только для "top-level" action

    private ImSet<Pair<LP, List<ResolveClassSet>>> debugLocals;// только для list action
    
    public boolean hasDebugLocals() {
        return debugLocals != null && !debugLocals.isEmpty();
    }
    public void setDebugLocals(ImSet<Pair<LP, List<ResolveClassSet>>> debugLocals) {
        this.debugLocals = debugLocals;
    }

    public Action(LocalizedString caption, ImOrderSet<P> interfaces) {
        super(caption, interfaces);

        drawOptions.addProcessor(new DefaultProcessor() {
            @Override
            public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form) {
                if(entity.forceViewType == null)
                    entity.forceViewType = ClassViewType.PANEL;
            }

            @Override
            public void proceedDefaultDesign(PropertyDrawView propertyView) {
            }
        });
    }

    public final static AddValue<Property, Boolean> addValue = new SymmAddValue<Property, Boolean>() {
        public Boolean addValue(Property key, Boolean prevValue, Boolean newValue) {
            return prevValue && newValue;
        }
    };

    public void setDebugInfo(ActionDebugInfo debugInfo) {
        this.debugInfo = debugInfo;
    }
    
    @Override
    public ActionDebugInfo getDebugInfo() {
        return (ActionDebugInfo) debugInfo;
    }

    public void setNewDebugStack(boolean newDebugStack) {
        this.newDebugStack = newDebugStack;
    }

    public void setParamInfo(ParamDebugInfo<P> paramInfo) {
        this.paramInfo = paramInfo;
    }

    // assert что возвращает только DataProperty, ClassDataProperty, Set(IsClassProperty), Drop(IsClassProperty), Drop(ClassDataProperty), ObjectClassProperty, для использования в лексикографике (calculateLinks)
    public ImMap<Property, Boolean> getChangeExtProps() {
        ActionMapImplement<?, P> compile = callCompile(false);
        if(compile!=null)
            return compile.property.getChangeExtProps();

        return aspectChangeExtProps();
    }

    // убирает Set и Drop, так как с depends будет использоваться
    public ImSet<Property> getChangeProps() {
        ImMap<Property, Boolean> changeExtProps = getChangeExtProps();
        int size = changeExtProps.size();
        MSet<Property> mResult = SetFact.mSetMax(size);
        for(int i=0;i<size;i++) {
            Property property = changeExtProps.getKey(i);
            if(property instanceof ChangedProperty)
                mResult.add(((ChangedProperty)property).property);
            else {
                assert property instanceof DataProperty || property instanceof ObjectClassProperty || property instanceof ClassDataProperty;
                mResult.add(property);
            }
        }

        return mResult.immutable();
    }
    // схема с аспектом сделана из-за того что getChangeProps для ChangeClassAction не инвариантен (меняется после компиляции), тоже самое и For с addObject'ом
    @IdentityStartLazy // только компиляция, построение лексикографики
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        MMap<Property, Boolean> result = MapFact.mMap(addValue);
        for(Action<?> dependAction : getDependActions())
            result.addAll(dependAction.getChangeExtProps());
        return result.immutable();
    }

    protected void markRecursions(ImSet<ListCaseAction> recursiveActions) {
        for(Action action : getDependActions())
            action.markRecursions(recursiveActions);
    }

    public ImMap<Property, Boolean> getUsedExtProps() {
        ActionMapImplement<?, P> compile = callCompile(false);
        if(compile!=null)
            return compile.property.getUsedExtProps();

        return aspectUsedExtProps();
    }

    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    protected ImMap<Property, Boolean> aspectUsedExtProps() {
        MMap<Property, Boolean> result = MapFact.mMap(addValue);
        for(Action<?> dependAction : getDependActions())
            result.addAll(dependAction.getUsedExtProps());
        return result.immutable();
    }

    public ImSet<Property> getUsedProps() {
        return getUsedExtProps().keys();
    }

    protected static ImMap<Property, Boolean> getChangeProps(Property... props) {
        MMap<Property, Boolean> result = MapFact.mMap(addValue);
        for(Property element : props)
            result.addAll(element.getChangeProps().toMap(false));
        return result.immutable();
    }
    protected static ImMap<Property, Boolean> getChangeProps(ImCol<Property> props) {
        MMap<Property, Boolean> result = MapFact.mMap(addValue);
        for(Property element : props)
            result.addAll(element.getChangeProps().toMap(false));
        return result.immutable();
    }
    protected static <T extends PropertyInterface> ImMap<Property, Boolean> getUsedProps(PropertyInterfaceImplement<T>... props) {
        return getUsedProps(SetFact.<PropertyInterfaceImplement<T>>EMPTY(), props);
    }
    protected static <T extends PropertyInterface> ImMap<Property, Boolean> getUsedProps(ImCol<? extends PropertyInterfaceImplement<T>> col, PropertyInterfaceImplement<T>... props) {
        MSet<Property> mResult = SetFact.mSet();
        for(PropertyInterfaceImplement<T> element : col)
            element.mapFillDepends(mResult);
        for(PropertyInterfaceImplement<T> element : props)
            element.mapFillDepends(mResult);
        return mResult.immutable().toMap(false);
    }

    private FunctionSet<Property> usedProps;
    public FunctionSet<Property> getDependsUsedProps() {
        if(usedProps==null)
            usedProps = Property.getDependsFromSet(getUsedProps());
        return usedProps;
    }

    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    public boolean hasFlow(ChangeFlowType type) {
        for(Action<?> dependAction : getDependActions())
            if(dependAction.hasFlow(type))
                return true;
        return false;
    }

    // пока просто ищем в конце APPLY и CHANGE'ы после APPLY
    // потом по хорошему надо будет в if then apply else cancel
    public boolean endsWithApplyAndNoChangesAfterBreaksBefore() {
        return false;
    }

    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    public ImSet<SessionProperty> getSessionCalcDepends(boolean events) {
        MSet<SessionProperty> mResult = SetFact.mSet();
        for(Property property : getUsedProps())
            mResult.addAll(property.getSessionCalcDepends(events));
        return mResult.immutable();
    }

    public ImSet<OldProperty> getParseOldDepends() {
        MSet<OldProperty> mResult = SetFact.mSet();
        for(Property property : getUsedProps())
            mResult.addAll(property.getParseOldDepends());
        return mResult.immutable();
    }

    public abstract ImSet<Action> getDependActions();
    
    @IdentityLazy
    private ImSet<Pair<String, Integer>> getInnerDebugActions() {
        ImSet<Pair<String, Integer>> result = getRecInnerDebugActions();
        if (debugInfo != null && debugInfo.needToCreateDelegate()) {
            result = result.merge(debugInfo.getDebuggerModuleLine());
        }
        return result;
    }

    protected ImSet<Pair<String, Integer>> getRecInnerDebugActions() {
        MSet<Pair<String, Integer>> result = SetFact.mSet();
        for (Action actionProperty : getDependActions()) {
            result.addAll(actionProperty.getInnerDebugActions());
        }
        return result.immutable();
    }

    @IdentityLazy
    public boolean uses(Property property) {
        for(Property usedProp : getUsedProps())
            if(Property.depends(usedProp, property))
                return true;
        return false;
    }

    @IdentityLazy
    private ImSet<Pair<String, Integer>> getChangePropsLocations() {
        MSet<Pair<String, Integer>> result = SetFact.mSet();
        for (Property property : getChangeProps()) {
            PropertyDebugInfo debugInfo = property.getDebugInfo();
            if (debugInfo != null && debugInfo.needToCreateDelegate()) {
                result.add(debugInfo.getDebuggerModuleLine());
            }
        }
        return result.immutable();
    }

    public boolean isInInterface(ImMap<P, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        return getWhereProperty().mapIsInInterface(interfaceClasses, isAny);
    }

    public ImMap<P, ValueClass> getInterfaceClasses(ClassType type) {
        return getWhereProperty().mapGetInterfaceClasses(type);
    }

    @IdentityLazy
    public PropertyMapImplement<?, P> getWhereProperty() {
        return getWhereProperty(false);
    }
    
    private static final Checker<ValueClass> checker = new Checker<ValueClass>() {
        public boolean checkEquals(ValueClass expl, ValueClass calc) {
            return BaseUtils.hashEquals(expl, calc);
        }
    }; 
            
    private PropertyMapImplement<?, P> calcClassWhereProperty() {
        ImMap<P, ValueClass> inferred = getExplicitCalcInterfaces(interfaces, getExplicitInterfaces(), new Callable<ImMap<P, ValueClass>>() {
            public ImMap<P, ValueClass> call() throws Exception {
                return calcWhereInterfaceClasses();
            }}, "ACTION ", this, checker);
        return IsClassProperty.getMapProperty(inferred);
    }

    private ImMap<P, ValueClass> getExplicitInterfaces() {
        if(explicitClasses == null)
            return null;
        return ExClassSet.fromResolveValue(explicitClasses);
    }

    private ImMap<P, ValueClass> calcWhereInterfaceClasses() {
        return calcWhereProperty().mapInterfaceClasses(ClassType.signaturePolicy);
    }

    @StackMessage("{logics.property.actions.flow.calc.where}")
    @ThisMessage
    public PropertyMapImplement<?, P> getWhereProperty(boolean recursive) {
        ActionWhereType actionWhere = AlgType.actionWhere;
        if(actionWhere != ActionWhereType.CALC && (!recursive || actionWhere == ActionWhereType.CLASS))
            return calcClassWhereProperty();
        else
            return calcWhereProperty();
    }
    
    public abstract PropertyMapImplement<?, P> calcWhereProperty();

    @Override
    protected ImCol<Pair<ActionOrProperty<?>, LinkType>> calculateLinks(boolean events) {
        if(getEvents().isEmpty()) // вырежем Action'ы без Event'ов, они нигде не используются, а дают много компонент связности
            return SetFact.EMPTY();

        MCol<Pair<ActionOrProperty<?>, LinkType>> mResult = ListFact.mCol();
        ImMap<Property, Boolean> used = getUsedExtProps();
        for(int i=0,size=used.size();i<size;i++) {
            Property<?> property = used.getKey(i);
            Boolean rec = used.getValue(i);

            // эвристика : усилим связи к session calc, предполагается 
            ImSet<SessionProperty> calcDepends = property.getSessionCalcDepends(events); // в том числе и для событий усилим, хотя может быть определенная избыточность,когда в SessionCalc - другой SessionCalc, но это очень редкие случаи
            for(int j=0,sizeJ=calcDepends.size();j<sizeJ;j++)
                mResult.add(new Pair<ActionOrProperty<?>, LinkType>(calcDepends.get(j), rec ? LinkType.RECEVENT : LinkType.EVENTACTION));

            mResult.add(new Pair<ActionOrProperty<?>, LinkType>(property, rec ? LinkType.RECUSED : LinkType.USEDACTION));
        }

//        раньше зачем-то было, но зачем непонятно
//        mResult.add(new Pair<Property<?>, LinkType>(getWhereProperty().property, hasFlow(ChangeFlowType.NEWSESSION) ? LinkType.RECUSED : LinkType.USEDACTION));

        ImSet<Property> depend = getStrongUsed();
        for(int i=0,size=depend.size();i<size;i++) {
            Property property = depend.get(i);
            mResult.add(new Pair<ActionOrProperty<?>, LinkType>(property, isRecursiveStrongUsed(property) ? LinkType.GOAFTERREC : LinkType.DEPEND));
        }
        return mResult.immutableCol();
    }

    private ImSet<Property> strongUsed = SetFact.EMPTY();
    public void addStrongUsed(ImSet<Property> properties) { // чисто для лексикографики
        strongUsed = strongUsed.merge(properties);
    }
    public ImSet<Property> getStrongUsed() {
        return strongUsed;
    }
    private ImSet<Property> recursiveStrongUsed = SetFact.EMPTY();
    public void checkRecursiveStrongUsed(Property property) {
        if(strongUsed.contains(property))
            recursiveStrongUsed = recursiveStrongUsed.merge(property);  
    }
    public boolean isRecursiveStrongUsed(Property property) { // при рекурсии ослабим связь, но не удалим, чтобы была в той же компоненте связности, но при этом не было цикла
        return recursiveStrongUsed.contains(property);
    }

    public <V extends PropertyInterface> ActionMapImplement<P, V> getImplement(ImOrderSet<V> list) {
        return new ActionMapImplement<>(this, getMapInterfaces(list));
    }

    public Object events = MapFact.mExclMap();
    public void addEvent(BaseEvent event, SessionEnvEvent forms) {
        ((MExclMap<BaseEvent, SessionEnvEvent>)events).exclAdd(event, forms);
    }
    @LongMutable
    public ImMap<BaseEvent, SessionEnvEvent> getEvents() {
        return (ImMap<BaseEvent, SessionEnvEvent>)events;
    }
    public SessionEnvEvent getSessionEnv(BaseEvent event) {
        return getEvents().get(event);
    }

    public boolean singleApply = false;
    public boolean resolve = false;
    public boolean hasResolve() {
        return getSessionEnv(SystemEvent.APPLY)==SessionEnvEvent.ALWAYS && resolve;
    }

    private Object beforeAspects = ListFact.mCol();
    public void addBeforeAspect(ActionMapImplement<?, P> action) {
        ((MCol<ActionMapImplement<?, P>>)beforeAspects).add(action);
    }
    @LongMutable
    public ImCol<ActionMapImplement<?, P>> getBeforeAspects() {
        return (ImCol<ActionMapImplement<?,P>>)beforeAspects;
    }
    private Object afterAspects = ListFact.mCol();
    public void addAfterAspect(ActionMapImplement<?, P> action) {
        ((MCol<ActionMapImplement<?, P>>)afterAspects).add(action);
    }
    @LongMutable
    public ImCol<ActionMapImplement<?, P>> getAfterAspects() {
        return (ImCol<ActionMapImplement<?,P>>)afterAspects;
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        beforeAspects = ((MCol<ActionMapImplement<?, P>>)beforeAspects).immutableCol();
        afterAspects = ((MCol<ActionMapImplement<?, P>>)afterAspects).immutableCol();
        events = ((MMap<BaseEvent, SessionEnvEvent>)events).immutable();
    }

    public final FlowResult execute(ExecutionContext<P> context) throws SQLException, SQLHandledException {
//        context.actionName = toString();
        if(newDebugStack) { // самым первым, так как paramInfo
            context = context.override();
            context.setNewDebugStack(true);
        }
        if(paramInfo != null) {
            context = context.override();
            context.setParamsToInterfaces(paramInfo.paramsToInterfaces);
            context.setParamsToFQN(paramInfo.paramsToClassFQN);
        }
        if(debugLocals != null) {
            context = context.override();
            context.setLocals(debugLocals);
        }
        if (debugInfo != null && debugger.isEnabled() && debugInfo.needToCreateDelegate()) {
            return debugger.delegate(this, context);
        } else {
            return executeImpl(context);
        }
    }

    public FlowResult executeImpl(ExecutionContext<P> context) throws SQLException, SQLHandledException {
        if(Thread.currentThread().isInterrupted() && !ThreadUtils.isFinallyMode(Thread.currentThread()))
            return FlowResult.THROWS;

        for(ActionMapImplement<?, P> aspect : getBeforeAspects()) {
            FlowResult beforeResult = aspect.execute(context);
            if(beforeResult != FlowResult.FINISH)
                return beforeResult;
        }

        ActionMapImplement<?, P> compile = callCompile(true);
        if (compile != null)
            return compile.execute(context);

        FlowResult result = aspectExecute(context);

        for(ActionMapImplement<?, P> aspect : getAfterAspects())
            aspect.execute(context);

        return result;
    }

    @Override
    public void prereadCaches() {
        super.prereadCaches();
        callCompile(true);
    }

    protected abstract FlowResult aspectExecute(ExecutionContext<P> context) throws SQLException, SQLHandledException;

    public ActionMapImplement<P, P> getImplement() {
        return new ActionMapImplement<>(this, getIdentityInterfaces());
    }

    public void execute(ExecutionEnvironment env, ExecutionStack stack) throws SQLException, SQLHandledException {
        assert interfaces.size()==0;
        execute(MapFact.<P, DataObject>EMPTY(), env, stack, null);
    }

    public void execute(ImMap<P, ? extends ObjectValue> keys, ExecutionEnvironment env, ExecutionStack stack, FormEnvironment<P> formEnv) throws SQLException, SQLHandledException {
        env.execute(this, keys, formEnv, null, stack);
    }

    @Override
    public ActionMapImplement<?, P> getDefaultEditAction(String editActionSID, Property filterProperty) {
        if(editActionSID.equals(ServerResponse.CHANGE_WYS) || editActionSID.equals(ServerResponse.EDIT_OBJECT))
            return null;
        return getImplement();
    }

    /**
     * возвращает тип для "простого" редактирования, когда этот action используется в качестве действия для редактирования </br>
     * assert, что тип будет DataClass, т.к. для остальных такое редактирование невозможно...
     * @param optimistic - если true, то если для некоторых случаев нельзя вывести тип, то эти случае будут игнорироваться
     */
    public Type getSimpleRequestInputType(boolean optimistic) {
        return getSimpleRequestInputType(optimistic, false);
    }
    // по сути protected (recursive usage)
    public Type getSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        return null;
    }

    // по аналогии с верхним, assert что !hasChildren
    public CustomClass getSimpleAdd() {
        return null;
    }

    private boolean isSimpleDelete;
    
    public void setSimpleDelete(boolean isSimpleDelete) {
        assert interfaces.size() == 1;
        this.isSimpleDelete = isSimpleDelete;
    }

    public P getSimpleDelete() {
        if(isSimpleDelete)
            return interfaces.single();
        return null;
    }

    protected ActionClassImplement<P> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<P> mapping) {
        return new ActionClassImplement<>(this, classes, mapping);
    }

    @IdentityStrongLazy // STRONG because of using in security policy
    public ActionMapImplement<?, P> getGroupChange() {
        ActionMapImplement<P, P> changeImplement = getImplement();
        ImOrderSet<P> listInterfaces = getOrderInterfaces();

        GroupChangeActionProperty groupChangeActionProperty = new GroupChangeActionProperty(LocalizedString.NONAME, listInterfaces, changeImplement);
        return groupChangeActionProperty.getImplement(listInterfaces);
    }

    @IdentityLazy
    public ImSet<OldProperty> getSessionEventOldDepends() { // assert что OldProperty, при этом у которых Scope соответствующий локальному событию
        // assert что OldProperty, при этом у которых Scope соответствующий локальному событию
        assert getSessionEnv(SystemEvent.SESSION) != null;
        return getOldDepends().filterFn(new SFunctionSet<OldProperty>() {
            public boolean contains(OldProperty element) {
                return element.scope == PrevScope.EVENT;
            }
        });
    }

    @IdentityLazy
    public ImSet<SessionProperty> getGlobalEventSessionCalcDepends() { // assert что OldProperty, при этом у которых Scope соответствующий локальному событию
        assert getSessionEnv(SystemEvent.APPLY) != null;
        return getSessionCalcDepends(false); // предполагается что FOR g, гда для g - есть вычисляемое свойство не будет, так как может привести к событиям на пустых сессиях
    }

    private ActionMapImplement<?, P> callCompile(boolean forExecution) {
        //не включаем компиляцию экшенов при дебаге
        if (forExecution && debugger.isEnabled() && !forceCompile()) {
            if (debugger.steppingMode || debugger.hasBreakpoint(getInnerDebugActions(), getChangePropsLocations())) {
                return null;
            }
        }
        return compile();
    }
    
    protected boolean forceCompile() {
        return false;
    }

    public ActionMapImplement<?, P> compile() {
        return null;
    }

    public ImList<ActionMapImplement<?, P>> getList() {
        return ListFact.<ActionMapImplement<?, P>>singleton(getImplement());
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<P, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return false;
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> Property getPushWhere(ImRevMap<P, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        throw new RuntimeException("should not be");
    }
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionMapImplement<?,T> pushFor(ImRevMap<P, T> mapping, ImSet<T> context, PropertyMapImplement<PW, T> where, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        throw new RuntimeException("should not be");
    }

    protected void proceedNullException() {
    }

    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.AFTER_DELEGATE;        
    }

    @Override
    public boolean isNotNull() {
        return false;
    }

    @IdentityStartLazy
    public ImList<Property> getSortedUsedProps() {
        return getUsedProps().sort(BusinessLogics.propComparator());
    }

    public boolean ignoreReadOnlyPolicy() {
        return !hasFlow(ChangeFlowType.READONLYCHANGE);
    }

    @Override
    public ApplyGlobalEvent getApplyEvent() {
        if (getSessionEnv(SystemEvent.APPLY)!=null) {
            if(event == null)
                event = new ApplyGlobalActionEvent(this);
            return event;
        }
        return null;
    }
}
