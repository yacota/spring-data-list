/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.springframework.data.mongodb.core.convert;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.DefaultSpELExpressionEvaluator;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.mapping.model.SpELContext;
import org.springframework.data.mapping.model.SpELExpressionEvaluator;
import org.springframework.data.mapping.model.SpELExpressionParameterValueProvider;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * The only relevant part here is the method readCollectionOrArray the rest is pure c&p from parent class
 */
public class CustomMappingMongoConverter 
extends      MappingMongoConverter {
    
    private static final String INCOMPATIBLE_TYPES = "Cannot convert %1$s of type %2$s into an instance of %3$s! Implement a custom Converter<%2$s, %3$s> and register it with the CustomConversions. Parent object was: %4$s";
    
    private final SpELContext spELContext;

    public CustomMappingMongoConverter(DbRefResolver dbRefResolver, MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
        super(dbRefResolver, mappingContext);
        this.spELContext = new SpELContext(DBObjectPropertyAccessor.INSTANCE);
    }

    public CustomMappingMongoConverter(MongoDbFactory mongoDbFactory, MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
        super(mongoDbFactory, mappingContext);
        this.spELContext = new SpELContext(DBObjectPropertyAccessor.INSTANCE);
    }
    
    //
    // THE PROPOSED CHANGE IS JUST in the following  method ... the rest is pure c&p 
    //
    
    
    //
    // Dealing with a Collection in MongoDB and mapping it to a single object(the first one) in return
    //
    private Object readCollectionOrArray(TypeInformation<?> targetType, BasicDBList sourceValue, ObjectPath path) {
            Assert.notNull(targetType, "Target type must not be null!");
            Assert.notNull(path, "Object path must not be null!");

            Class<?> collectionType = targetType.getType();

            boolean isCollectionTypeClass = Collection.class.isAssignableFrom(collectionType);

            if (sourceValue.isEmpty()) {
                return getPotentiallyConvertedSimpleRead(new HashSet<Object>(), collectionType);
            }

            //JLL when a non collection type is in the model we can build a List of this type, not the generic one,
            //and leave to DefaultconversionService to pick the first one

            TypeInformation<?> componentType = null;
            Class<?> rawComponentType = null;
            if (isCollectionTypeClass) {
                componentType = targetType.getComponentType();
                rawComponentType = componentType == null ? null : componentType.getType();
                collectionType = isCollectionTypeClass ? collectionType : List.class;
            } else {
                componentType = targetType;
                rawComponentType = collectionType;
                collectionType = List.class;
            }

            Collection<Object> items = targetType.getType().isArray() ? new ArrayList<Object>()
                            : CollectionFactory.createCollection(collectionType, rawComponentType, sourceValue.size());

            for (int i = 0; i < sourceValue.size(); i++) {

                    Object dbObjItem = sourceValue.get(i);

                    if (dbObjItem instanceof DBRef) {
                            items.add(
                                            DBRef.class.equals(rawComponentType) ? dbObjItem : read(componentType, readRef((DBRef) dbObjItem), path));
                    } else if (dbObjItem instanceof DBObject) {
                            items.add(read(componentType, (DBObject) dbObjItem, path));
                    } else {
                            items.add(getPotentiallyConvertedSimpleRead(dbObjItem, rawComponentType));
                    }
            }

            return getPotentiallyConvertedSimpleRead(items, targetType.getType());
	}
    
    // MappingMongoConverter c&p

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mongodb.core.core.MongoReader#read(java.lang.Class, com.mongodb.DBObject)
    s*/
    public <S extends Object> S read(Class<S> clazz, final DBObject dbo) {
           return read(ClassTypeInformation.from(clazz), dbo);
    }

    protected <S extends Object> S read(TypeInformation<S> type, DBObject dbo) {
           return read(type, dbo, ObjectPath.ROOT);
    }

    @SuppressWarnings("unchecked")
    private <S extends Object> S read(TypeInformation<S> type, DBObject dbo, ObjectPath path) {

            if (null == dbo) {
                    return null;
            }

            TypeInformation<? extends S> typeToUse = typeMapper.readType(dbo, type);
            Class<? extends S> rawType = typeToUse.getType();

            if (conversions.hasCustomReadTarget(dbo.getClass(), rawType)) {
                    return conversionService.convert(dbo, rawType);
            }

            if (DBObject.class.isAssignableFrom(rawType)) {
                    return (S) dbo;
            }

            if (typeToUse.isCollectionLike() && dbo instanceof BasicDBList) {
                    return (S) readCollectionOrArray(typeToUse, (BasicDBList) dbo, path);
            }

            if (typeToUse.isMap()) {
                    return (S) readMap(typeToUse, dbo, path);
            }

            if (dbo instanceof BasicDBList) {
                    throw new MappingException(String.format(INCOMPATIBLE_TYPES, dbo, BasicDBList.class, typeToUse.getType(), path));
            }

            // Retrieve persistent entity info
            MongoPersistentEntity<S> persistentEntity = (MongoPersistentEntity<S>) mappingContext
                            .getPersistentEntity(typeToUse);
            if (persistentEntity == null) {
                    throw new MappingException("No mapping metadata found for " + rawType.getName());
            }

            return read(persistentEntity, dbo, path);
    }

    private ParameterValueProvider<MongoPersistentProperty> getParameterProvider(MongoPersistentEntity<?> entity,
                    DBObject source, DefaultSpELExpressionEvaluator evaluator, ObjectPath path) {

            MongoDbPropertyValueProvider provider = new MongoDbPropertyValueProvider(source, evaluator, path);
            PersistentEntityParameterValueProvider<MongoPersistentProperty> parameterProvider = new PersistentEntityParameterValueProvider<MongoPersistentProperty>(
                            entity, provider, path.getCurrentObject());

            return new ConverterAwareSpELExpressionParameterValueProvider(evaluator, conversionService, parameterProvider,
                            path);
    }

    private <S extends Object> S read(final MongoPersistentEntity<S> entity, final DBObject dbo, final ObjectPath path) {

            final DefaultSpELExpressionEvaluator evaluator = new DefaultSpELExpressionEvaluator(dbo, spELContext);

            ParameterValueProvider<MongoPersistentProperty> provider = getParameterProvider(entity, dbo, evaluator, path);
            EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
            S instance = instantiator.createInstance(entity, provider);

            final PersistentPropertyAccessor accessor = new ConvertingPropertyAccessor(entity.getPropertyAccessor(instance),
                            conversionService);

            final MongoPersistentProperty idProperty = entity.getIdProperty();
            final S result = instance;

            // make sure id property is set before all other properties
            Object idValue = null;

            if (idProperty != null) {
                    idValue = getValueInternal(idProperty, dbo, evaluator, path);
                    accessor.setProperty(idProperty, idValue);
            }

            final ObjectPath currentPath = path.push(result, entity, idValue);

            // Set properties not already set in the constructor
            entity.doWithProperties(new PropertyHandler<MongoPersistentProperty>() {
                    public void doWithPersistentProperty(MongoPersistentProperty prop) {

                            // we skip the id property since it was already set
                            if (idProperty != null && idProperty.equals(prop)) {
                                    return;
                            }

                            if (!dbo.containsField(prop.getFieldName()) || entity.isConstructorArgument(prop)) {
                                    return;
                            }

                            accessor.setProperty(prop, getValueInternal(prop, dbo, evaluator, currentPath));
                    }
            });

            // Handle associations
            entity.doWithAssociations(new AssociationHandler<MongoPersistentProperty>() {
                    public void doWithAssociation(Association<MongoPersistentProperty> association) {

                            final MongoPersistentProperty property = association.getInverse();
                            Object value = dbo.get(property.getFieldName());

                            if (value == null) {
                                    return;
                            }

                            DBRef dbref = value instanceof DBRef ? (DBRef) value : null;

                            DbRefProxyHandler handler = new DefaultDbRefProxyHandler(spELContext, mappingContext,
                                            CustomMappingMongoConverter.this);
                            DbRefResolverCallback callback = new DefaultDbRefResolverCallback(dbo, currentPath, evaluator,
                                            CustomMappingMongoConverter.this);

                            accessor.setProperty(property, dbRefResolver.resolveDbRef(property, dbref, callback, handler));
                    }
            });

            return result;
    }

    /**
     * Internal write conversion method which should be used for nested invocations.
     * 
     * @param obj
     * @param dbo
     */
    @SuppressWarnings("unchecked")
    protected void writeInternal(final Object obj, final DBObject dbo, final TypeInformation<?> typeHint) {

            if (null == obj) {
                    return;
            }

            Class<?> entityType = obj.getClass();
            Class<?> customTarget = conversions.getCustomWriteTarget(entityType, DBObject.class);

            if (customTarget != null) {
                    DBObject result = conversionService.convert(obj, DBObject.class);
                    dbo.putAll(result);
                    return;
            }

            if (Map.class.isAssignableFrom(entityType)) {
                    writeMapInternal((Map<Object, Object>) obj, dbo, ClassTypeInformation.MAP);
                    return;
            }

            if (Collection.class.isAssignableFrom(entityType)) {
                    writeCollectionInternal((Collection<?>) obj, ClassTypeInformation.LIST, (BasicDBList) dbo);
                    return;
            }

            MongoPersistentEntity<?> entity = mappingContext.getPersistentEntity(entityType);
            writeInternal(obj, dbo, entity);
            addCustomTypeKeyIfNecessary(typeHint, obj, dbo);
    }

    protected void writeInternal(Object obj, final DBObject dbo, MongoPersistentEntity<?> entity) {

            if (obj == null) {
                    return;
            }

            if (null == entity) {
                    throw new MappingException("No mapping metadata found for entity of type " + obj.getClass().getName());
            }

            final PersistentPropertyAccessor accessor = entity.getPropertyAccessor(obj);
            final MongoPersistentProperty idProperty = entity.getIdProperty();

            if (!dbo.containsField("_id") && null != idProperty) {

                    try {
                            Object id = accessor.getProperty(idProperty);
                            dbo.put("_id", idMapper.convertId(id));
                    } catch (ConversionException ignored) {}
            }

            // Write the properties
            entity.doWithProperties(new PropertyHandler<MongoPersistentProperty>() {
                    public void doWithPersistentProperty(MongoPersistentProperty prop) {

                            if (prop.equals(idProperty) || !prop.isWritable()) {
                                    return;
                            }

                            Object propertyObj = accessor.getProperty(prop);

                            if (null != propertyObj) {

                                    if (!conversions.isSimpleType(propertyObj.getClass())) {
                                            writePropertyInternal(propertyObj, dbo, prop);
                                    } else {
                                            writeSimpleInternal(propertyObj, dbo, prop);
                                    }
                            }
                    }
            });

            entity.doWithAssociations(new AssociationHandler<MongoPersistentProperty>() {

                    public void doWithAssociation(Association<MongoPersistentProperty> association) {

                            MongoPersistentProperty inverseProp = association.getInverse();
                            Object propertyObj = accessor.getProperty(inverseProp);

                            if (null != propertyObj) {
                                    writePropertyInternal(propertyObj, dbo, inverseProp);
                            }
                    }
            });
    }

    @SuppressWarnings({ "unchecked" })
    protected void writePropertyInternal(Object obj, DBObject dbo, MongoPersistentProperty prop) {

            if (obj == null) {
                    return;
            }

            DBObjectAccessor accessor = new DBObjectAccessor(dbo);

            TypeInformation<?> valueType = ClassTypeInformation.from(obj.getClass());
            TypeInformation<?> type = prop.getTypeInformation();

            if (valueType.isCollectionLike()) {
                    DBObject collectionInternal = createCollection(asCollection(obj), prop);
                    accessor.put(prop, collectionInternal);
                    return;
            }

            if (valueType.isMap()) {
                    DBObject mapDbObj = createMap((Map<Object, Object>) obj, prop);
                    accessor.put(prop, mapDbObj);
                    return;
            }

            if (prop.isDbReference()) {

                    DBRef dbRefObj = null;

                    /*
                     * If we already have a LazyLoadingProxy, we use it's cached DBRef value instead of 
                     * unnecessarily initializing it only to convert it to a DBRef a few instructions later.
                     */
                    if (obj instanceof LazyLoadingProxy) {
                            dbRefObj = ((LazyLoadingProxy) obj).toDBRef();
                    }

                    dbRefObj = dbRefObj != null ? dbRefObj : createDBRef(obj, prop);

                    if (null != dbRefObj) {
                            accessor.put(prop, dbRefObj);
                            return;
                    }
            }

            /*
             * If we have a LazyLoadingProxy we make sure it is initialized first.
             */
            if (obj instanceof LazyLoadingProxy) {
                    obj = ((LazyLoadingProxy) obj).getTarget();
            }

            // Lookup potential custom target type
            Class<?> basicTargetType = conversions.getCustomWriteTarget(obj.getClass(), null);

            if (basicTargetType != null) {
                    accessor.put(prop, conversionService.convert(obj, basicTargetType));
                    return;
            }

            Object existingValue = accessor.get(prop);
            BasicDBObject propDbObj = existingValue instanceof BasicDBObject ? (BasicDBObject) existingValue
                            : new BasicDBObject();
            addCustomTypeKeyIfNecessary(ClassTypeInformation.from(prop.getRawType()), obj, propDbObj);

            MongoPersistentEntity<?> entity = isSubtype(prop.getType(), obj.getClass())
                            ? mappingContext.getPersistentEntity(obj.getClass()) : mappingContext.getPersistentEntity(type);

            writeInternal(obj, propDbObj, entity);
            accessor.put(prop, propDbObj);
    }

    private boolean isSubtype(Class<?> left, Class<?> right) {
            return left.isAssignableFrom(right) && !left.equals(right);
    }

    /**
     * Returns given object as {@link Collection}. Will return the {@link Collection} as is if the source is a
     * {@link Collection} already, will convert an array into a {@link Collection} or simply create a single element
     * collection for everything else.
     * 
     * @param source
     * @return
     */
    private static Collection<?> asCollection(Object source) {

            if (source instanceof Collection) {
                    return (Collection<?>) source;
            }

            return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
    }

    /**
     * Writes the given {@link Collection} using the given {@link MongoPersistentProperty} information.
     * 
     * @param collection must not be {@literal null}.
     * @param property must not be {@literal null}.
     * @return
     */
    protected DBObject createCollection(Collection<?> collection, MongoPersistentProperty property) {

            if (!property.isDbReference()) {
                    return writeCollectionInternal(collection, property.getTypeInformation(), new BasicDBList());
            }

            BasicDBList dbList = new BasicDBList();

            for (Object element : collection) {

                    if (element == null) {
                            continue;
                    }

                    DBRef dbRef = createDBRef(element, property);
                    dbList.add(dbRef);
            }

            return dbList;
    }

    /**
     * Writes the given {@link Map} using the given {@link MongoPersistentProperty} information.
     * 
     * @param map must not {@literal null}.
     * @param property must not be {@literal null}.
     * @return
     */
    protected DBObject createMap(Map<Object, Object> map, MongoPersistentProperty property) {

            Assert.notNull(map, "Given map must not be null!");
            Assert.notNull(property, "PersistentProperty must not be null!");

            if (!property.isDbReference()) {
                    return writeMapInternal(map, new BasicDBObject(), property.getTypeInformation());
            }

            BasicDBObject dbObject = new BasicDBObject();

            for (Map.Entry<Object, Object> entry : map.entrySet()) {

                    Object key = entry.getKey();
                    Object value = entry.getValue();

                    if (conversions.isSimpleType(key.getClass())) {

                            String simpleKey = prepareMapKey(key.toString());
                            dbObject.put(simpleKey, value != null ? createDBRef(value, property) : null);

                    } else {
                            throw new MappingException("Cannot use a complex object as a key value.");
                    }
            }

            return dbObject;
    }

    /**
     * Populates the given {@link BasicDBList} with values from the given {@link Collection}.
     * 
     * @param source the collection to create a {@link BasicDBList} for, must not be {@literal null}.
     * @param type the {@link TypeInformation} to consider or {@literal null} if unknown.
     * @param sink the {@link BasicDBList} to write to.
     * @return
     */
    private BasicDBList writeCollectionInternal(Collection<?> source, TypeInformation<?> type, BasicDBList sink) {

            TypeInformation<?> componentType = type == null ? null : type.getComponentType();

            for (Object element : source) {

                    Class<?> elementType = element == null ? null : element.getClass();

                    if (elementType == null || conversions.isSimpleType(elementType)) {
                            sink.add(getPotentiallyConvertedSimpleWrite(element));
                    } else if (element instanceof Collection || elementType.isArray()) {
                            sink.add(writeCollectionInternal(asCollection(element), componentType, new BasicDBList()));
                    } else {
                            BasicDBObject propDbObj = new BasicDBObject();
                            writeInternal(element, propDbObj, componentType);
                            sink.add(propDbObj);
                    }
            }

            return sink;
    }

    /**
     * Writes the given {@link Map} to the given {@link DBObject} considering the given {@link TypeInformation}.
     * 
     * @param obj must not be {@literal null}.
     * @param dbo must not be {@literal null}.
     * @param propertyType must not be {@literal null}.
     * @return
     */
    protected DBObject writeMapInternal(Map<Object, Object> obj, DBObject dbo, TypeInformation<?> propertyType) {

            for (Map.Entry<Object, Object> entry : obj.entrySet()) {

                    Object key = entry.getKey();
                    Object val = entry.getValue();

                    if (conversions.isSimpleType(key.getClass())) {

                            String simpleKey = prepareMapKey(key);
                            if (val == null || conversions.isSimpleType(val.getClass())) {
                                    writeSimpleInternal(val, dbo, simpleKey);
                            } else if (val instanceof Collection || val.getClass().isArray()) {
                                    dbo.put(simpleKey,
                                                    writeCollectionInternal(asCollection(val), propertyType.getMapValueType(), new BasicDBList()));
                            } else {
                                    DBObject newDbo = new BasicDBObject();
                                    TypeInformation<?> valueTypeInfo = propertyType.isMap() ? propertyType.getMapValueType()
                                                    : ClassTypeInformation.OBJECT;
                                    writeInternal(val, newDbo, valueTypeInfo);
                                    dbo.put(simpleKey, newDbo);
                            }
                    } else {
                            throw new MappingException("Cannot use a complex object as a key value.");
                    }
            }

            return dbo;
    }

    /**
     * Prepares the given {@link Map} key to be converted into a {@link String}. Will invoke potentially registered custom
     * conversions and escape dots from the result as they're not supported as {@link Map} key in MongoDB.
     * 
     * @param key must not be {@literal null}.
     * @return
     */
    private String prepareMapKey(Object key) {

            Assert.notNull(key, "Map key must not be null!");

            String convertedKey = potentiallyConvertMapKey(key);
            return potentiallyEscapeMapKey(convertedKey);
    }

    /**
     * Potentially replaces dots in the given map key with the configured map key replacement if configured or aborts
     * conversion if none is configured.
     * 
     * @see #setMapKeyDotReplacement(String)
     * @param source
     * @return
     */
    protected String potentiallyEscapeMapKey(String source) {

            if (!source.contains(".")) {
                    return source;
            }

            if (mapKeyDotReplacement == null) {
                    throw new MappingException(String.format(
                                    "Map key %s contains dots but no replacement was configured! Make "
                                                    + "sure map keys don't contain dots in the first place or configure an appropriate replacement!",
                                    source));
            }

            return source.replaceAll("\\.", mapKeyDotReplacement);
    }

    /**
     * Returns a {@link String} representation of the given {@link Map} key
     * 
     * @param key
     * @return
     */
    private String potentiallyConvertMapKey(Object key) {

            if (key instanceof String) {
                    return (String) key;
            }

            return conversions.hasCustomWriteTarget(key.getClass(), String.class)
                            ? (String) getPotentiallyConvertedSimpleWrite(key) : key.toString();
    }

    /**
     * Translates the map key replacements in the given key just read with a dot in case a map key replacement has been
     * configured.
     * 
     * @param source
     * @return
     */
    protected String potentiallyUnescapeMapKey(String source) {
            return mapKeyDotReplacement == null ? source : source.replaceAll(mapKeyDotReplacement, "\\.");
    }

    /**
     * Adds custom type information to the given {@link DBObject} if necessary. That is if the value is not the same as
     * the one given. This is usually the case if you store a subtype of the actual declared type of the property.
     *
     * @param type
     * @param value must not be {@literal null}.
     * @param dbObject must not be {@literal null}.
     */
    protected void addCustomTypeKeyIfNecessary(TypeInformation<?> type, Object value, DBObject dbObject) {

            TypeInformation<?> actualType = type != null ? type.getActualType() : null;
            Class<?> reference = actualType == null ? Object.class : actualType.getType();
            Class<?> valueType = ClassUtils.getUserClass(value.getClass());

            boolean notTheSameClass = !valueType.equals(reference);
            if (notTheSameClass) {
                    typeMapper.writeType(valueType, dbObject);
            }
    }

    /**
     * Writes the given simple value to the given {@link DBObject}. Will store enum names for enum values.
     * 
     * @param value
     * @param dbObject must not be {@literal null}.
     * @param key must not be {@literal null}.
     */
    private void writeSimpleInternal(Object value, DBObject dbObject, String key) {
            dbObject.put(key, getPotentiallyConvertedSimpleWrite(value));
    }

    private void writeSimpleInternal(Object value, DBObject dbObject, MongoPersistentProperty property) {
            DBObjectAccessor accessor = new DBObjectAccessor(dbObject);
            accessor.put(property, getPotentiallyConvertedSimpleWrite(value));
    }

    /**
     * Checks whether we have a custom conversion registered for the given value into an arbitrary simple Mongo type.
     * Returns the converted value if so. If not, we perform special enum handling or simply return the value as is.
     * 
     * @param value
     * @return
     */
    private Object getPotentiallyConvertedSimpleWrite(Object value) {

            if (value == null) {
                    return null;
            }

            Class<?> customTarget = conversions.getCustomWriteTarget(value.getClass(), null);

            if (customTarget != null) {
                    return conversionService.convert(value, customTarget);
            } else {
                    return Enum.class.isAssignableFrom(value.getClass()) ? ((Enum<?>) value).name() : value;
            }
    }

    /**
     * Checks whether we have a custom conversion for the given simple object. Converts the given value if so, applies
     * {@link Enum} handling or returns the value as is.
     * 
     * @param value
     * @param target must not be {@literal null}.
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object getPotentiallyConvertedSimpleRead(Object value, Class<?> target) {

            if (value == null || target == null || target.isAssignableFrom(value.getClass())) {
                    return value;
            }

            if (conversions.hasCustomReadTarget(value.getClass(), target)) {
                    return conversionService.convert(value, target);
            }

            if (Enum.class.isAssignableFrom(target)) {
                    return Enum.valueOf((Class<Enum>) target, value.toString());
            }

            return conversionService.convert(value, target);
    }

    protected DBRef createDBRef(Object target, MongoPersistentProperty property) {

            Assert.notNull(target);

            if (target instanceof DBRef) {
                    return (DBRef) target;
            }

            MongoPersistentEntity<?> targetEntity = mappingContext.getPersistentEntity(target.getClass());
            targetEntity = targetEntity == null ? targetEntity = mappingContext.getPersistentEntity(property) : targetEntity;

            if (null == targetEntity) {
                    throw new MappingException("No mapping metadata found for " + target.getClass());
            }

            MongoPersistentProperty idProperty = targetEntity.getIdProperty();

            if (idProperty == null) {
                    throw new MappingException("No id property found on class " + targetEntity.getType());
            }

            Object id = null;

            if (target.getClass().equals(idProperty.getType())) {
                    id = target;
            } else {
                    PersistentPropertyAccessor accessor = targetEntity.getPropertyAccessor(target);
                    id = accessor.getProperty(idProperty);
            }

            if (null == id) {
                    throw new MappingException("Cannot create a reference to an object with a NULL id.");
            }

            return dbRefResolver.createDbRef(property == null ? null : property.getDBRef(), targetEntity,
                            idMapper.convertId(id));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mongodb.core.convert.ValueResolver#getValueInternal(org.springframework.data.mongodb.core.mapping.MongoPersistentProperty, com.mongodb.DBObject, org.springframework.data.mapping.model.SpELExpressionEvaluator, java.lang.Object)
     */
    @Override
    public Object getValueInternal(MongoPersistentProperty prop, DBObject dbo, SpELExpressionEvaluator evaluator,
                    ObjectPath path) {
            return new MongoDbPropertyValueProvider(dbo, evaluator, path).getPropertyValue(prop);
    }

    /**
     * Reads the given {@link DBObject} into a {@link Map}. will recursively resolve nested {@link Map}s as well.
     * 
     * @param type the {@link Map} {@link TypeInformation} to be used to unmarshall this {@link DBObject}.
     * @param dbObject must not be {@literal null}
     * @param path must not be {@literal null}
     * @return
     */
    @SuppressWarnings("unchecked")
    protected Map<Object, Object> readMap(TypeInformation<?> type, DBObject dbObject, ObjectPath path) {

            Assert.notNull(dbObject, "DBObject must not be null!");
            Assert.notNull(path, "Object path must not be null!");

            Class<?> mapType = typeMapper.readType(dbObject, type).getType();

            TypeInformation<?> keyType = type.getComponentType();
            Class<?> rawKeyType = keyType == null ? null : keyType.getType();

            TypeInformation<?> valueType = type.getMapValueType();
            Class<?> rawValueType = valueType == null ? null : valueType.getType();

            Map<Object, Object> map = org.springframework.data.convert.CollectionFactory.createMap(mapType, rawKeyType, dbObject.keySet().size());
            Map<String, Object> sourceMap = dbObject.toMap();

            for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
                    if (typeMapper.isTypeKey(entry.getKey())) {
                            continue;
                    }

                    Object key = potentiallyUnescapeMapKey(entry.getKey());

                    if (rawKeyType != null) {
                            key = conversionService.convert(key, rawKeyType);
                    }

                    Object value = entry.getValue();

                    if (value instanceof DBObject) {
                            map.put(key, read(valueType, (DBObject) value, path));
                    } else if (value instanceof DBRef) {
                            map.put(key, DBRef.class.equals(rawValueType) ? value : read(valueType, readRef((DBRef) value)));
                    } else {
                            Class<?> valueClass = valueType == null ? null : valueType.getType();
                            map.put(key, getPotentiallyConvertedSimpleRead(value, valueClass));
                    }
            }

            return map;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mongodb.core.convert.MongoWriter#convertToMongoType(java.lang.Object, org.springframework.data.util.TypeInformation)
     */
    @SuppressWarnings("unchecked")
    public Object convertToMongoType(Object obj, TypeInformation<?> typeInformation) {

            if (obj == null) {
                    return null;
            }

            Class<?> target = conversions.getCustomWriteTarget(obj.getClass());
            if (target != null) {
                    return conversionService.convert(obj, target);
            }

            if (conversions.isSimpleType(obj.getClass())) {
                    // Doesn't need conversion
                    return getPotentiallyConvertedSimpleWrite(obj);
            }

            TypeInformation<?> typeHint = typeInformation;

            if (obj instanceof BasicDBList) {
                    return maybeConvertList((BasicDBList) obj, typeHint);
            }

            if (obj instanceof DBObject) {
                    DBObject newValueDbo = new BasicDBObject();
                    for (String vk : ((DBObject) obj).keySet()) {
                            Object o = ((DBObject) obj).get(vk);
                            newValueDbo.put(vk, convertToMongoType(o, typeHint));
                    }
                    return newValueDbo;
            }

            if (obj instanceof Map) {
                    DBObject result = new BasicDBObject();
                    for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) obj).entrySet()) {
                            result.put(entry.getKey().toString(), convertToMongoType(entry.getValue(), typeHint));
                    }
                    return result;
            }

            if (obj.getClass().isArray()) {
                    return maybeConvertList(Arrays.asList((Object[]) obj), typeHint);
            }

            if (obj instanceof Collection) {
                    return maybeConvertList((Collection<?>) obj, typeHint);
            }

            DBObject newDbo = new BasicDBObject();
            this.write(obj, newDbo);

            if (typeInformation == null) {
                    return removeTypeInfo(newDbo, true);
            }

            if (typeInformation.getType().equals(NestedDocument.class)) {
                    return removeTypeInfo(newDbo, false);
            }

            return !obj.getClass().equals(typeInformation.getType()) ? newDbo : removeTypeInfo(newDbo, true);
    }

    public BasicDBList maybeConvertList(Iterable<?> source, TypeInformation<?> typeInformation) {

            BasicDBList newDbl = new BasicDBList();
            for (Object element : source) {
                    newDbl.add(convertToMongoType(element, typeInformation));
            }

            return newDbl;
    }

    /**
     * Removes the type information from the entire conversion result.
     * 
     * @param object
     * @param recursively whether to apply the removal recursively
     * @return
     */
    private Object removeTypeInfo(Object object, boolean recursively) {

            if (!(object instanceof DBObject)) {
                    return object;
            }

            DBObject dbObject = (DBObject) object;
            String keyToRemove = null;

            for (String key : dbObject.keySet()) {

                    if (recursively) {

                            Object value = dbObject.get(key);

                            if (value instanceof BasicDBList) {
                                    for (Object element : (BasicDBList) value) {
                                            removeTypeInfo(element, recursively);
                                    }
                            } else {
                                    removeTypeInfo(value, recursively);
                            }
                    }

                    if (typeMapper.isTypeKey(key)) {

                            keyToRemove = key;

                            if (!recursively) {
                                    break;
                            }
                    }
            }

            if (keyToRemove != null) {
                    dbObject.removeField(keyToRemove);
            }

            return dbObject;
    }

    /**
     * {@link PropertyValueProvider} to evaluate a SpEL expression if present on the property or simply accesses the field
     * of the configured source {@link DBObject}.
     *
     * @author Oliver Gierke
     */
    private class MongoDbPropertyValueProvider implements PropertyValueProvider<MongoPersistentProperty> {

            private final DBObjectAccessor source;
            private final SpELExpressionEvaluator evaluator;
            private final ObjectPath path;

            /**
             * Creates a new {@link MongoDbPropertyValueProvider} for the given source, {@link SpELExpressionEvaluator} and
             * {@link ObjectPath}.
             * 
             * @param source must not be {@literal null}.
             * @param evaluator must not be {@literal null}.
             * @param path can be {@literal null}.
             */
            public MongoDbPropertyValueProvider(DBObject source, SpELExpressionEvaluator evaluator, ObjectPath path) {

                    Assert.notNull(source);
                    Assert.notNull(evaluator);

                    this.source = new DBObjectAccessor(source);
                    this.evaluator = evaluator;
                    this.path = path;
            }

            /* 
             * (non-Javadoc)
             * @see org.springframework.data.convert.PropertyValueProvider#getPropertyValue(org.springframework.data.mapping.PersistentProperty)
             */
            public <T> T getPropertyValue(MongoPersistentProperty property) {

                    String expression = property.getSpelExpression();
                    Object value = expression != null ? evaluator.evaluate(expression) : source.get(property);

                    if (value == null) {
                            return null;
                    }

                    return readValue(value, property.getTypeInformation(), path);
            }
    }

    /**
     * Extension of {@link SpELExpressionParameterValueProvider} to recursively trigger value conversion on the raw
     * resolved SpEL value.
     * 
     * @author Oliver Gierke
     */
    private class ConverterAwareSpELExpressionParameterValueProvider
                    extends SpELExpressionParameterValueProvider<MongoPersistentProperty> {

            private final ObjectPath path;

            /**
             * Creates a new {@link ConverterAwareSpELExpressionParameterValueProvider}.
             * 
             * @param evaluator must not be {@literal null}.
             * @param conversionService must not be {@literal null}.
             * @param delegate must not be {@literal null}.
             */
            public ConverterAwareSpELExpressionParameterValueProvider(SpELExpressionEvaluator evaluator,
                            ConversionService conversionService, ParameterValueProvider<MongoPersistentProperty> delegate,
                            ObjectPath path) {

                    super(evaluator, conversionService, delegate);
                    this.path = path;
            }

            /* 
             * (non-Javadoc)
             * @see org.springframework.data.mapping.model.SpELExpressionParameterValueProvider#potentiallyConvertSpelValue(java.lang.Object, org.springframework.data.mapping.PreferredConstructor.Parameter)
             */
            @Override
            protected <T> T potentiallyConvertSpelValue(Object object, PreferredConstructor.Parameter<T, MongoPersistentProperty> parameter) {
                    return readValue(object, parameter.getType(), path);
            }
    }

    @SuppressWarnings("unchecked")
    private <T> T readValue(Object value, TypeInformation<?> type, ObjectPath path) {

            Class<?> rawType = type.getType();

            if (conversions.hasCustomReadTarget(value.getClass(), rawType)) {
                    return (T) conversionService.convert(value, rawType);
            } else if (value instanceof DBRef) {
                    return potentiallyReadOrResolveDbRef((DBRef) value, type, path, rawType);
            } else if (value instanceof BasicDBList) {
                    return (T) readCollectionOrArray(type, (BasicDBList) value, path);
            } else if (value instanceof DBObject) {
                    return (T) read(type, (DBObject) value, path);
            } else {
                    return (T) getPotentiallyConvertedSimpleRead(value, rawType);
            }
    }

    @SuppressWarnings("unchecked")
    private <T> T potentiallyReadOrResolveDbRef(DBRef dbref, TypeInformation<?> type, ObjectPath path, Class<?> rawType) {

            if (rawType.equals(DBRef.class)) {
                    return (T) dbref;
            }

            Object object = dbref == null ? null : path.getPathItem(dbref.getId(), dbref.getCollectionName());

            if (object != null) {
                    return (T) object;
            }

            return (T) (object != null ? object : read(type, readRef(dbref), path));
    }

    /**
     * Performs the fetch operation for the given {@link DBRef}.
     * 
     * @param ref
     * @return
     */
    DBObject readRef(DBRef ref) {
            return dbRefResolver.fetch(ref);
    }

    /**
     * Marker class used to indicate we have a non root document object here that might be used within an update - so we
     * need to preserve type hints for potential nested elements but need to remove it on top level.
     * 
     * @author Christoph Strobl
     * @since 1.8
     */
    static class NestedDocument {

    }

}