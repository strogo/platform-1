package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.Mapper;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.logics.property.PropertyInterface;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.entity.PropertyObjectEntity;

import java.sql.SQLException;
import java.util.Set;

public abstract class PropertyFilterEntity<P extends PropertyInterface> extends FilterEntity {

    public PropertyObjectEntity<P> property;

    public PropertyFilterEntity(PropertyObjectEntity<P> iProperty) {
        property = iProperty;
    }

    protected abstract FilterInstance doMapping(PropertyObjectInstance<P> propertyImplement, Mapper mapper) throws SQLException;

    public FilterInstance doMapping(Mapper mapper) throws SQLException {
        return doMapping(mapper.mapProperty(property),mapper);
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        property.fillObjects(objects);
    }
}
