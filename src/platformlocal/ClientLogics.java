/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ClientGroupObjectImplement extends ArrayList<ClientObjectImplement> {

    Integer GID = 0;
    
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (this.GID != null ? this.GID.hashCode() : 0);
        return hash;
    }
}

class ClientGroupObjectMap<T> extends HashMap<ClientObjectImplement,T> {

   
/*  На самом деле не надо - так как сравнивать как раз надо именно по значениям
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    // Здесь по хорошему нужно hashcode когда новые свойства появятся перегрузить
    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    } */

}

class ClientGroupObjectValue extends ClientGroupObjectMap<Integer> {
    
}

class ClientObjectImplement {
    
    Integer GID = 0;
    
    ClientGroupObjectImplement groupObject;
 
    String caption = "";
    
    ClientObjectView objectIDView;
    
}

class ClientGroupPropertyView {
    
}

abstract class ClientAbstractView {
    
    Integer GID = 0;

    ClientGroupPropertyView groupProperty;
    ClientGroupObjectImplement groupObject;

    Dimension minimumSize;
    Dimension maximumSize;
    Dimension preferredSize;

    String caption;

    public int getPreferredWidth() {
        return 50;
    }
    
    public int getPreferredHeight() {
        return 15;
    }
    
    public Dimension getPreferredSize() {
        
        if (preferredSize != null) return preferredSize;
        return new Dimension(getPreferredWidth(), getPreferredHeight());
    }
    
    abstract public PropertyRendererComponent getRendererComponent(ClientForm form);
    abstract public PropertyEditorComponent getEditorComponent(ClientForm form);

}

class ClientPropertyView extends ClientAbstractView {
    
    String type;
    
    public int getPreferredWidth() {
        
        int res = 15;
        
        if (type.equals("integer")) res = 9;
        if (type.equals("char(50)")) res = 50;
        
        return res * 5;
    }
    
    private PropertyRendererComponent renderer;
    public PropertyRendererComponent getRendererComponent(ClientForm form) {
        
        if (renderer == null) {
            
            if (type.equals("integer")) renderer = new IntegerPropertyRenderer();
            if (type.equals("char(50)")) renderer = new StringPropertyRenderer();

            if (renderer == null) renderer = new StringPropertyRenderer();
            
        }
        
        return renderer;
        
    }
    
    public PropertyEditorComponent getEditorComponent(ClientForm form) {
        
        if (type.equals("integer")) return new IntegerPropertyEditor();
        if (type.equals("char(50)")) return new StringPropertyEditor();
        
        return new StringPropertyEditor();
        
    }

}

class ClientObjectView extends ClientAbstractView {

    private PropertyRendererComponent renderer;
    public PropertyRendererComponent getRendererComponent(ClientForm form) {
        
        if (renderer == null) {
            renderer = new IntegerPropertyRenderer();
        }
        
        return renderer;
        
    }
    
    public PropertyEditorComponent getEditorComponent(ClientForm form) {
        
        form.switchClassView(groupObject);
        return null;
    }
    
}

class ClientFormChanges extends AbstractFormChanges<ClientGroupObjectImplement,ClientGroupObjectValue,ClientPropertyView> {
    
}


class ClientFormInit {
    
    List<ClientGroupObjectImplement> groupObjects;
    List<ClientObjectImplement> objects;
    List<ClientGroupPropertyView> groupProperties;
    List<ClientPropertyView> properties;
    
    List<ClientAbstractView> order;
    
    public ClientFormInit() {
        
        groupObjects = new ArrayList();
        objects = new ArrayList();
        groupProperties = new ArrayList();
        properties = new ArrayList();
        
        order = new ArrayList();
        
    }

}

class ClientFormBean {
    
    FormBeanView formBean;
    
    Map<GroupObjectImplement, ClientGroupObjectImplement> groupObjects = new HashMap();
    Map<ObjectImplement, ClientObjectImplement> objects = new HashMap();
    Map<PropertyView, ClientPropertyView> properties = new HashMap();
    Map<GroupObjectValue, ClientGroupObjectValue> objectValues = new HashMap();

    List<ClientGroupObjectImplement> listGroups = new ArrayList();
    List<ClientObjectImplement> listObjects = new ArrayList();
    List<ClientPropertyView> listProperties = new ArrayList();
    
    List<ClientAbstractView> listOrder = new ArrayList();
    
    public ClientGroupObjectImplement client(GroupObjectImplement groupObject) {
        return groupObjects.get(groupObject);
    }

    public ClientObjectImplement client(ObjectImplement object) {
        return objects.get(object);
    }
    
    public ClientPropertyView client(PropertyView property) {
        return properties.get(property);
    }
    
    public ClientFormBean(FormBeanView iformBean) {
        
        formBean = iformBean;

        for (GroupObjectImplement group : formBean.Groups) {
            
            ClientGroupObjectImplement clientGroup = new ClientGroupObjectImplement();
            groupObjects.put(group, clientGroup);
            listGroups.add(clientGroup);

            for (ObjectImplement object : group) {

                ClientObjectImplement clientObject = new ClientObjectImplement();
                clientObject.groupObject = clientGroup;
                
                clientObject.caption = object.OutName;
                clientObject.objectIDView = new ClientObjectView();
                clientObject.objectIDView.groupObject = clientGroup;
                clientObject.objectIDView.caption = object.OutName;
                
                clientGroup.add(clientObject);
                
                objects.put(object, clientObject);
                listObjects.add(clientObject);

                listOrder.add(clientObject.objectIDView);
            }
        }
        
        for (PropertyView property : formBean.Properties) {
            
            ClientPropertyView clientProperty = new ClientPropertyView();
            clientProperty.groupObject = groupObjects.get(property.ToDraw);
            
            //временно
            clientProperty.caption = property.View.Property.OutName;
            clientProperty.type = property.View.Property.GetDBType();
            
            properties.put(property, clientProperty);
            listProperties.add(clientProperty);
            
            listOrder.add(clientProperty);
        }
        
    }

    ClientFormChanges convertFormChangesToClient (FormChanges formChanges)
    {
        ClientFormChanges clientChanges = new ClientFormChanges();

        for (GroupObjectImplement group : formChanges.Objects.keySet()) {

            ClientGroupObjectImplement clientGroup = groupObjects.get(group);
            
            GroupObjectValue objectValue = formChanges.Objects.get(group);
            ClientGroupObjectValue clientObjectValue = new ClientGroupObjectValue();
            
            for (ObjectImplement object : objectValue.keySet()) {

                ClientObjectImplement clientObject = objects.get(object);
                
                clientObjectValue.put(clientObject, objectValue.get(object));
            }
            
            objectValues.put(objectValue, clientObjectValue);
            clientChanges.Objects.put(clientGroup, clientObjectValue);
        }

        for (GroupObjectImplement group : formChanges.GridObjects.keySet()) {

            ClientGroupObjectImplement clientGroup = groupObjects.get(group);
            
            List<GroupObjectValue> listObjectValue = formChanges.GridObjects.get(group);
            List<ClientGroupObjectValue> listClientObjectValue = new ArrayList();
            
            for (GroupObjectValue objectValue : listObjectValue) {

                ClientGroupObjectValue clientObjectValue = new ClientGroupObjectValue();
            
                for (ObjectImplement object : objectValue.keySet()) {

                    ClientObjectImplement clientObject = objects.get(object);

                    clientObjectValue.put(clientObject, objectValue.get(object));
                }
                
                objectValues.put(objectValue, clientObjectValue);
                listClientObjectValue.add(clientObjectValue);
            }
            
            clientChanges.GridObjects.put(clientGroup, listClientObjectValue);
        }
        
        for (PropertyView property : formChanges.PanelProperties.keySet()) {
            
            ClientPropertyView clientProperty = properties.get(property);
            
            clientChanges.PanelProperties.put(clientProperty, formChanges.PanelProperties.get(property));
        }

        for (PropertyView property : formChanges.GridProperties.keySet()) {

            ClientPropertyView clientProperty = properties.get(property);
            
            Map<GroupObjectValue, Object> mapObjectValue = formChanges.GridProperties.get(property);
            Map<ClientGroupObjectValue, Object> clientMapObjectValue = new HashMap();
            
            for (GroupObjectValue objectValue : mapObjectValue.keySet()) {

                ClientGroupObjectValue clientObjectValue = objectValues.get(objectValue);
                clientMapObjectValue.put(clientObjectValue, mapObjectValue.get(objectValue));
            }
            
            clientChanges.GridProperties.put(clientProperty, clientMapObjectValue);
        }
        
        for (PropertyView property : formChanges.DropProperties) {

            ClientPropertyView clientProperty = properties.get(property);
            
            clientChanges.DropProperties.add(clientProperty);
        }
        
        return clientChanges;
    
    }
    
    public ClientFormInit getClientFormInit() {
        
        ClientFormInit formInit = new ClientFormInit();
  
        for (ClientGroupObjectImplement groupObject : listGroups)
            formInit.groupObjects.add(groupObject);
        
        for (ClientObjectImplement object : listObjects)
            formInit.objects.add(object);
            
        for (ClientPropertyView property : listProperties)
            formInit.properties.add(property);
        
        for (ClientAbstractView view : listOrder)
            formInit.order.add(view);
        
        return formInit;
        
    }

    // Здесь тестинг, заглушка на простую перекачку FormChanges в ClientFormChanges
    
    ClientFormChanges changeObject(ClientGroupObjectImplement groupObject, ClientGroupObjectValue objectValue) {
        
        MapUtils<GroupObjectImplement, ClientGroupObjectImplement> mgu = new MapUtils();
        GroupObjectImplement GroupObject = mgu.getKey(groupObjects, groupObject);

        MapUtils<GroupObjectValue, ClientGroupObjectValue> mvu = new MapUtils();
        GroupObjectValue GroupVal = mvu.getKey(objectValues, objectValue);
        
        try {
            formBean.ChangeObject(GroupObject, GroupVal);
        } catch(SQLException e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        formChanges.Out(formBean);

        System.out.println(objectValue.toString());
        
        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges changeProperty(ClientPropertyView clientProperty, Object value) {
        
        MapUtils<PropertyView, ClientPropertyView> pvu = new MapUtils();
        PropertyView property = pvu.getKey(properties, clientProperty);
        
        try {
            formBean.ChangePropertyView(property, value);
        } catch(SQLException e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
        System.out.println("Change : " + value.toString());
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges addObject(ClientObjectImplement clientObject) {
        
        MapUtils<ObjectImplement, ClientObjectImplement> mou = new MapUtils();
        ObjectImplement object = mou.getKey(objects, clientObject);
        
        try {
            formBean.AddObject(object);
        } catch(SQLException e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        System.out.println("Change : " + value.toString());
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges changeClass(ClientObjectImplement clientObject) {
        
        MapUtils<ObjectImplement, ClientObjectImplement> mou = new MapUtils();
        ObjectImplement object = mou.getKey(objects, clientObject);
        
        try {
            formBean.ChangeClass(object, null);
        } catch(SQLException e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        System.out.println("Change : " + value.toString());
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges changeClassView(ClientGroupObjectImplement groupObject, Boolean classView) {
 
        MapUtils<GroupObjectImplement, ClientGroupObjectImplement> mgu = new MapUtils();
        GroupObjectImplement GroupObject = mgu.getKey(groupObjects, groupObject);

        formBean.ChangeClassView(GroupObject, classView);
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }

    ClientFormChanges saveChanges() {
 
        try {
            System.out.println("Save changes : " + formBean.SaveChanges());
        } catch(SQLException e) {
            
        }
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }
    
    ClientFormChanges cancelChanges() {
 
        formBean.CancelChanges();
        
        FormChanges formChanges = null;
        try {
            formChanges = formBean.EndApply();
        } catch (SQLException e) {
            System.out.println(e);
        }
        
//        formChanges.Out(formBean);

        return convertFormChangesToClient(formChanges);
        
    }
    
}