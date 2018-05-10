package org.osgl.util;

/*-
 * #%L
 * Java Tool
 * %%
 * Copyright (C) 2014 - 2018 OSGL (Open Source General Library)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.osgl.util.DataMapper.MappingRule.KEYWORD_MATCHING;

import org.osgl.$;
import org.osgl.Lang;
import org.osgl.OsglConfig;
import org.osgl.exception.MappingException;
import org.osgl.exception.UnexpectedException;
import org.osgl.util.converter.TypeConverterRegistry;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Map data from one structure into another structure. The target data structure
 * must be exists and must be mutable to do the data mapping.
 *
 * ## Error reporting
 *
 * `DataMapper` consumer can dictate on how `DataMapper` to handle exceptions by
 * passing boolean value `ignoreError` to constructor.
 *
 * If `ignoreError` is set to `true` then `DataMapper` will not raise error for
 * any exception raised during mapping process. It will simply skip to the next
 * field.
 *
 * Otherwise it will raise {@link MappingException} when
 *
 * * Exceptions raised
 * * Type conversion failed
 *
 * ## Mapping rule
 *
 * Three mapping rule has been defined:
 *
 * * {@link MappingRule#STRICT_MATCHING}
 * - mapping happens between exact same name, and do type conversion if needed
 * * {@link MappingRule#KEYWORD_MATCHING}
 * - mapping happens if the {@link Keyword} of source field name
 * and target field name matches. For example `foo_bar` can be mapped to `fooBar`.
 *
 * ## Field name based mapping
 *
 * `DataMapper` do field name based mapping, in other words, it does not rely
 * on JavaBean properties to do the mapping. For example if source object
 * has a field name `foo`, the value of `source.foo` will be mapped to
 * the target object's field `foo`: `target.foo`.
 *
 * ## Recursive mapping
 *
 * `DataMapper` do recursive mapping. If target object has embedded structure
 * and source object has the corresponding value, then that value will be
 * mapped to target object's embedded structure recursively.
 *
 * ## Type conversion
 *
 * When mapping semantic is {@link Semantic#MAP}, the mapper will try to
 * convert from source type to target type when types doesn't match. Otherwise
 * it will raise an {@link MappingException}.
 *
 * ## Mapping to array
 *
 * For target field that is an array, tt will first try to do type conversion
 * in case special converter has been defined.
 *
 * If there is not converter defined or when root mapping target is an array,
 * then it will check if source component can be converted to an {@link Iterable}.
 * If it's okay then it will add elements from the iterable to the target array.
 * In case element exists in the target array then it will do recursive mapping
 * from the source element to the target element.
 *
 * ### Handle out of range
 *
 * If source array/collection has more elements that the target array can hold,
 * an new array will be returned when calling to {@link #getTarget()}.
 *
 * If array is inner property of another structure, it will be replaced with
 * the new array that holds all elements from source array/collection
 *
 * ## Mapping to a List
 *
 * Determine the list element type. If this is a recursive mapping, list element
 * type can be found from the field type of upper level target. Otherwise, it
 * will try to determine list type from the existing elements of the list. If
 * there is no element in the list then it assume list can be put in any type
 * of object.
 *
 * For target field that is an list, it will first try to do type conversion
 * in case special converter has been defined.
 *
 * If there is no converter defined, then it will check if source component can be
 * converted to an {@link Iterable}. If it's okay then it will add elements from
 * the iterable to the target list. In case the element in the position is not
 * null in the target list, it will do recursive mapping from the source element
 * to the target element.
 *
 * If source array/collection has more elements than the target list, then
 * the extra elements will be added to the list
 *
 * ## Mapping to a Set
 *
 * Determine the set element type. If this is a recursive mapping, set element
 * type can be found from the field type of upper level target. Otherwise, it
 * will try to determine list type from the existing elements of the set. If
 * there is no element in the list then it assume set can be put in any type
 * of object.
 *
 * For target field that is a set, it will first try to do type conversion in
 * case special converter has been defined.
 *
 * If there is no converter defined, then it will check if source component can be
 * converted to an {@link Iterable}. If it's okay then it will add elements from
 * the iterable to the target set. Unlike mapping to an array or a list, there is
 * no recursive element mapping happening for set, it just add the elements from
 * source iterable into the target set, after type conversion if needed.
 *
 * ## Mapping to a Map
 *
 * When mapping to a map, it will check if target map has existing non-null value
 * for a certain property, if it exists, then it will do recursive mapping from
 * property value to the target map entry value.
 *
 * If there are extra properties in the source object, they will be added into the
 * target map if the type can be converted.
 *
 * When mapping to a map, the {@link MappingRule#KEYWORD_MATCHING} won't effect, it is
 * treated as {@link MappingRule#STRICT_MATCHING}
 *
 * ## Mapping from a Map
 *
 * Again it will try to do type conversion to allow custom data mapping for certain types
 * happening. In case no type conversion is done then
 *
 * It treats map entry as fields of a class, literally the entry is treated as the field name
 * and entry value is treated as field value. And then start from there to do the
 * data mapping process.
 *
 * ## `null` value
 *
 * If `null` value encountered in source, the corresponding field/map entry will be set to `null`
 * in the target.
 */
public class DataMapper {

    public enum MappingRule {

        /**
         * field name must match exactly.
         */
        STRICT_MATCHING,

        /**
         * field name match by {@link Keyword} equality.
         *
         * For example, `foo_bar` in source can be mapped to `fooBar` in target
         */
        KEYWORD_MATCHING;

        public boolean keywordMatching() {
            return this == KEYWORD_MATCHING;
        }
    }

    /**
     * The mapping semantic steering the mapping logic
     */
    public enum Semantic {
        /**
         * Copy the reference from source properties to target properties.
         * * require property type be exactly match between source and target data
         * * if there are more properties in the target data, the extra properties will be
         * set to `null` or default value if they are primitive types
         */
        SHALLOW_COPY,

        /**
         * Recursively copy data from source data structure into target data structure.
         * * intermediate data structure can be different between source and target
         * * the terminate data type must match exactly
         * * if there are more properties in the target data, the extra properties will be
         * set to `null` or default value if they are primitive types
         * * if there are existing data in collection of target data structure, the existing data must be cleared
         * before copy happening
         */
        DEEP_COPY,

        /**
         * Recursively merge data from source data structure into target data structure. This
         * semantic is same as {@link #DEEP_COPY} except:
         * * if there are more properties in the target data, the extra properties will be left
         * untouched
         * * if there are existing data in collection of target data structure, the data will be
         * merged or untouched
         */
        MERGE,

        /**
         * Recursively map data from source data structure into target data structure. This
         * semantic is same as {@link #MERGE} except:
         * * the terminate data type can be different, in which case type conversion will be used.
         */
        MAP;

        boolean isShallowCopy() {
            return this == SHALLOW_COPY;
        }

        boolean isDeepCopy() {
            return this == DEEP_COPY;
        }

        boolean isCopy() {
            return isShallowCopy() || isDeepCopy();
        }

        boolean isMerge() {
            return this == MERGE;
        }

        boolean isMapping() {
            return this == MAP;
        }
    }

    class PropertyFilter extends $.Predicate<String> {

        /**
         * Keep a set of properties that can be copied.
         *
         * Note if both {@link #whiteList} and `blackList` contains
         * elements, then `whiteList` is ignored.
         */
        private Set<String> whiteList = C.set();

        /**
         * Keep a set of properties that shall not be copied.
         */
        private Set<String> blackList = C.set();

        /**
         * Contains the {@link Keyword} correspondence of
         * {@link #whiteList}.
         */
        private Set<Keyword> whiteKeywords = C.set();

        /**
         * Contains the {@link Keyword} correspondence of
         * {@link #blackList}
         */
        private Set<Keyword> blackKeywords = C.set();

        private boolean allEmpty = true;

        PropertyFilter(String spec) {
            if (S.blank(spec)) {
                return;
            }
            List<String> words = S.fastSplit(spec, ",");
            boolean useBlackList = false;
            for (String word : words) {
                if (useBlackList && !word.startsWith("-")) {
                    // ignore black list
                    continue;
                }
                if (word.startsWith("-")) {
                    useBlackList = true;
                    word = word.substring(1);
                }
                word = word.trim();
                if (useBlackList) {
                    if (rule == MappingRule.KEYWORD_MATCHING) {
                        if (blackKeywords == C.EMPTY_SET) {
                            blackKeywords = new HashSet<>();
                        }
                        blackKeywords.add(Keyword.of(word));
                    } else {
                        if (blackList == C.EMPTY_SET) {
                            blackList = new HashSet<>();
                        }
                        blackList.add(word);
                    }
                } else {
                    if (rule == MappingRule.KEYWORD_MATCHING) {
                        if (whiteKeywords == C.EMPTY_SET) {
                            whiteKeywords = new HashSet<>();
                        }
                        whiteKeywords.add(Keyword.of(word));
                    } else {
                        if (whiteList == C.EMPTY_SET) {
                            whiteList = new HashSet<>();
                        }
                        whiteList.add(word);
                    }
                }
            }
            if (useBlackList) {
                whiteKeywords = C.set();
                whiteList = C.set();
            }
            allEmpty = whiteKeywords.isEmpty() && whiteList.isEmpty() && blackKeywords.isEmpty() && blackList.isEmpty();
        }

        @Override
        public boolean test(String s) {
            E.illegalArgumentIf(S.blank(s));
            if (allEmpty) {
                return true;
            }
            String prefix = context.toString();
            if (S.notBlank(prefix)) {
                s = S.pathConcat(prefix, '.', s);
            }
            if (rule == KEYWORD_MATCHING) {
                Keyword keyword = Keyword.of(s);
                return blackKeywords.isEmpty() ? whiteKeywords.contains(keyword) : !blackKeywords.contains(keyword);
            } else {
                return blackList.isEmpty() ? whiteList.contains(s) : !blackList.contains(s);
            }
        }

    }

    private MappingRule rule;

    private Semantic semantic;

    /**
     * Keep track the object hierarchies
     */
    private StringBuilder context = new StringBuilder();

    /**
     * Used to track types walked through and detect
     * circular reference
     */
    private Set<Class> circularReferenceDetector;

    /**
     * Decide whether copy a field or not
     */
    private PropertyFilter filter;

    // the global filter
    private PropertyFilter globalFilter;

    private Object source;
    private Class<?> sourceType;

    private Object target;
    private Class<?> targetType;
    private ParameterizedType targetGenericType;
    private ParameterizedType targetComponentType;
    private Class<?> targetComponentRawType = Object.class;
    private Class<?> targetKeyType;

    /**
     * convert hints indexed by type
     */
    Map<Class, Object> conversionHints;

    /**
     * used to create new instance of certain type
     */
    $.Function<Class, ?> instanceFactory;

    /**
     * Allow inject custom type converters
     */
    TypeConverterRegistry typeConverterRegistry;

    /**
     * If set to `false` then it will raise {@link org.osgl.exception.MappingException}
     * whenever an error is encountered during mapping process.
     */
    private boolean ignoreError;

    /**
     * If set then it will stop exploring fields
     * when it reaches the `rootClass`
     */
    private Class rootClass;

    /**
     * Cache target fields
     */
    private List<Field> targetFields;

    /**
     * Cache result of check if target is an array
     */
    private boolean targetIsArray;

    /**
     * Cache result of check if target is a list
     */
    private boolean targetIsCollection;

    /**
     * Cache result of check if target is a collection
     */
    private boolean targetIsList;

    /**
     * Cache result of check if target is a map
     */
    private boolean targetIsMap;

    private boolean targetIsPojo;

    /**
     * Cache result of target length if it is array or collection
     */
    private int targetLength = -1;

    /**
     * Cache result of target as collection if it is a collection
     */
    private Collection targetCollection;

    /**
     * Cache result of target as list if it is a list
     */
    private List targetList;

    /**
     * Cache result of target as map if it is a map
     */
    private Map targetMap;

    /**
     * If target is newinstance then we will save the
     * logic to clear target
     */
    private boolean targetIsNewInstance;

    public DataMapper(Object source, Object target, ParameterizedType targetGenericType, MappingRule rule, Semantic semantic, String filterSpec, boolean ignoreError, Map<Class, Object> conversionHints, $.Function<Class, ?> instanceFactory, TypeConverterRegistry typeConverterRegistry, boolean targetIsNewInstance, Class<?> rootClass) {
        this.targetType = target.getClass();
        E.illegalArgumentIf(isImmutable(targetType), "target type is immutable: " + targetType.getName());
        this.targetGenericType = targetGenericType;
        this.sourceType = source.getClass();
        this.rule = $.requireNotNull(rule);
        this.semantic = $.requireNotNull(semantic);
        this.filter = new PropertyFilter(filterSpec);
        this.conversionHints = null == conversionHints ? C.<Class, Object>Map() : conversionHints;
        this.instanceFactory = null == instanceFactory ? OsglConfig.INSTANCE_FACTORY : instanceFactory;
        this.source = source;
        this.target = target;
        this.ignoreError = ignoreError;
        this.typeConverterRegistry = null == typeConverterRegistry ? TypeConverterRegistry.INSTANCE : typeConverterRegistry;
        this.targetIsNewInstance = targetIsNewInstance;
        this.rootClass = null == rootClass ? Object.class : rootClass;
        this.circularReferenceDetector = new HashSet<>();
        this.circularReferenceDetector.add(targetType);
        E.illegalArgumentIfNot(this.rootClass.isAssignableFrom(this.targetType), "root class[%s] must be assignable from target type[%s]", rootClass.getName(), targetType.getName());
        this.doMapping();
    }

    private DataMapper(Object source, Object target, String targetName, ParameterizedType targetGenericType, DataMapper parentMapper) {
        this.sourceType = source.getClass();
        this.source = source;
        this.targetType = target.getClass();
        this.target = target;
        this.targetGenericType = targetGenericType;
        this.rule = parentMapper.rule;
        this.semantic = parentMapper.semantic;
        this.filter = parentMapper.filter;
        this.ignoreError = parentMapper.ignoreError;
        this.conversionHints = parentMapper.conversionHints;
        this.instanceFactory = parentMapper.instanceFactory;
        this.typeConverterRegistry = parentMapper.typeConverterRegistry;
        this.context = new StringBuilder();
        String parentContext = parentMapper.context.toString();
        this.context.append(parentContext);
        if (S.notBlank(targetName)) {
            if (S.notBlank(parentContext)) {
                this.context.append(".").append(targetName);
            } else {
                this.context.append(targetName);
            }
        } else {
            // this case is the array or collection element copy
        }
        this.targetIsNewInstance = parentMapper.targetIsNewInstance;
        this.rootClass = Object.class;
        this.circularReferenceDetector = new HashSet<>();
        this.circularReferenceDetector.addAll(parentMapper.circularReferenceDetector);
        this.circularReferenceDetector.add(targetType);
        this.doMapping();
    }

    public Object getTarget() {
        return target;
    }

    private void doMapping() {
        String s = OsglConfig.globalMappingFilter(this.targetType);
        if (S.notBlank(s)) {
            this.globalFilter = new PropertyFilter(s);
        }
        try {
            this.probeTargetType();
            if (targetIsArray || targetIsCollection) {
                mapToArrayOrCollection();
            } else {
                if (targetIsMap) {
                    mapToMap();
                } else {
                    mapToPojo();
                }
            }
        } catch (MappingException e) {
            throw e;
        } catch (Exception e) {
            logError(e);
        }
    }

    /*
     * Array mapping require the source type to
     * be sequenced, i.e. array or iterable
     */
    private void mapToArrayOrCollection() {
        List sourceList = null;

        // ensure we have a source as list
        if (!isSequence(sourceType)) {
            if (semantic.isMapping()) {
                sourceList = convert(source).to(List.class);
            }
            if (null == sourceList) {
                logError("Cannot map source[%s] into array or collection", sourceType.getName());
                return;
            }
        } else {
            sourceList = convert(source).to(List.class);
        }

        final Object nullVal = $.convert(null).to(targetComponentRawType);

        // clear target for copy operations
        if (semantic.isCopy()) {
            if (targetIsArray) {
                for (int i = 0; i < targetLength; ++i) {
                    Array.set(target, i, nullVal);
                }
            } else if (targetIsList) {
                for (int i = 0; i < targetLength; ++i) {
                    targetList.set(i, nullVal);
                }
            } else {
                targetCollection.clear();
            }
        }

        // make sure target has enough size if it is an array or list
        int sourceLength = sourceList.size();
        final int originTargetLength = targetLength;
        if (targetLength < sourceLength) {
            if (targetIsArray) {
                Object target0 = target;
                target = Array.newInstance(targetComponentRawType, sourceLength);
                System.arraycopy(target0, 0, target, 0, targetLength);
                targetLength = sourceLength;
            } else if (targetIsList) {
                for (int i = targetLength; i < sourceLength; ++i) {
                    targetList.add(nullVal);
                }
                targetLength = sourceLength;
            }
        }

        // now map from source list into target
        boolean targetComponentIsContainer = isContainer(targetComponentRawType);
        Iterator itr = sourceList.iterator();
        int cursor = 0;
        while (itr.hasNext()) {
            Object sourceComponent = itr.next();

            // handle null value
            if (null == sourceComponent) {
                if (semantic.isMapping() || semantic.isMerge()) {
                    continue;
                }
                if (targetIsList) {
                    targetList.set(cursor++, nullVal);
                } else if (targetIsArray) {
                    Array.set(target, cursor++, nullVal);
                }
                continue;
            }

            // sanity check on type matching
            boolean componentRawTypeMatches = $.is(sourceComponent).allowBoxing().instanceOf(targetComponentRawType);
            if (!semantic.isMapping() && !componentRawTypeMatches) {
                logError("component type mismatch. Source component type: %s, target component type: %s", sourceComponent.getClass().getName(), targetComponentRawType.getName());
                continue;
            }

            // do the map work
            Object targetComponent = null;
            // try fetch existing component from array or list
            if ((targetIsArray || targetIsList) && !semantic.isCopy() && cursor < originTargetLength) {
                // if is copy then target is already cleared
                // if cursor >= original target length then the element is always null
                if (targetIsArray) {
                    targetComponent = Array.get(target, cursor);
                } else if (targetIsList) {
                    targetComponent = targetList.get(cursor);
                }
            }
            targetComponent = prepareTargetComponent(
                    sourceComponent, targetComponent, targetComponentRawType,
                    targetComponentType, targetComponentIsContainer, "");
            if (targetIsArray || targetIsList) {
                // for array and list update existing slot from source
                if (targetIsList) {
                    targetList.set(cursor++, targetComponent);
                } else if (targetIsArray) {
                    Array.set(target, cursor++, targetComponent);
                }
            } else {
                // for set or any other collection that is not an list, append from source
                targetCollection.add(targetComponent);
            }
        }
    }

    private void mapToMap() {
        targetMap.clear();

        // build target keyword index if needed
        Map targetMapKeywordLookup = null;
        if (rule.keywordMatching() && String.class == targetKeyType) {
            targetMapKeywordLookup = new HashMap();
            for (Object key : targetMap.keySet()) {
                targetMapKeywordLookup.put(Keyword.of(key.toString()), key);
            }
        }

        // do map work
        boolean targetComponentIsSequence = isSequence(targetComponentRawType);
        boolean targetComponentIsMap = !targetComponentIsSequence && isMap(targetComponentRawType);
        boolean targetComponentIsContainer = targetComponentIsMap || targetComponentIsSequence;
        String prefix = context.toString();
        for ($.Triple<Object, Keyword, $.Producer<Object>> sourceProperty : sourceProperties()) {
            Object sourceKey = sourceProperty.first();
            if (!semantic.isMapping() && !$.is(sourceKey).allowBoxing().instanceOf(targetKeyType)) {
                logError("map key type mismatch, required: %s; found: %s", targetKeyType, sourceKey.getClass().getName());
                continue;
            }
            Object sourceVal = sourceProperty.last().produce();
            if (null == sourceVal) {
                continue;
            }
            Keyword sourceKeyword = sourceProperty.second();
            Object targetKey = null;
            if (targetMapKeywordLookup != null) {
                targetKey = targetMapKeywordLookup.get(sourceKeyword);
            }
            if (targetKey == null) {
                targetKey = semantic.isMapping() ? convert(sourceKey).to(targetKeyType) : sourceKey;
            }
            String key = S.notBlank(prefix) ? S.pathConcat(prefix, '.', targetKey.toString()) : targetKey.toString();
            Object targetVal = targetMap.get(targetKey);
            targetVal = prepareTargetComponent(
                    sourceVal, targetVal, targetComponentRawType,
                    targetComponentType, targetComponentIsContainer, "");
            targetMap.put(targetKey, targetVal);
        }
    }

    private void mapToPojo() {
        Map<Object, Object> sourceMap = Map.class.isAssignableFrom(sourceType) ? (Map) source : null;
        Map<Keyword, Object> sourceMapByKeyword = null;
        if (rule.keywordMatching()) {
            sourceMapByKeyword = new HashMap<>();
            if (null != sourceMap) {
                for (Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
                    sourceMapByKeyword.put(Keyword.of(entry.getKey().toString()), entry.getValue());
                }
            } else {
                for (Field sourceField : $.fieldsOf(sourceType)) {
                    sourceMapByKeyword.put(Keyword.of(sourceField.getName()), sourceField);
                }
            }
        }
        String prefix = context.toString();
        for (Field targetField : targetFields) {
            Class<?> targetFieldType = targetField.getType();
            if (circularReferenceDetector.contains(targetFieldType)) {
                continue;
            }
            String targetFieldName = targetField.getName();
            if (null != globalFilter && !globalFilter.test(targetFieldName)) {
                continue;
            }
            String key = S.notBlank(prefix) ? S.pathConcat(prefix, '.', targetFieldName) : targetFieldName;
            if (!filter.test(key)) {
                continue;
            }
            Type type = targetField.getGenericType();
            ParameterizedType targetFieldGenericType = type instanceof ParameterizedType ? (ParameterizedType) type : null;
            Object sourcePropValue;
            if (null != sourceMapByKeyword) {
                sourcePropValue = sourceMapByKeyword.get(Keyword.of(targetFieldName));
                if (null == sourcePropValue) {
                    continue;
                }
                if (sourcePropValue instanceof Field) {
                    sourcePropValue = $.getFieldValue(source, (Field) sourcePropValue);
                }
            } else if (null != sourceMap) {
                sourcePropValue = sourceMap.get(targetFieldName);
            } else {
                Field sourceField = $.fieldOf(sourceType, targetFieldName);
                if (null == sourceField) {
                    continue;
                }
                sourcePropValue = $.getFieldValue(source, sourceField);
            }
            if (null == sourcePropValue) {
                if (semantic.isCopy()) {
                    $.setFieldValue(target, targetField, $.convert(null).to(targetFieldType));
                }
                continue;
            }

            boolean targetFieldIsContainer = isContainer(targetFieldType);
            if (!targetFieldIsContainer && !semantic.isMapping() && !$.is(sourcePropValue).allowBoxing().instanceOf(targetFieldType)) {
                logError("Type mismatch copy source [%s] to field[%s|%s]", sourcePropValue.getClass().getName(), targetFieldName, targetFieldType.getName());
                continue;
            }

            Object targetFieldValue = $.getFieldValue(target, targetField);
            targetFieldValue = prepareTargetComponent(
                    sourcePropValue, targetFieldValue, targetFieldType,
                    targetFieldGenericType, targetFieldIsContainer, targetFieldName);
            $.setFieldValue(target, targetField, targetFieldValue);
        }
    }

    private Iterable<$.Triple<Object, Keyword, $.Producer<Object>>> sourceProperties() {
        if (Map.class.isAssignableFrom(sourceType)) {
            return C.list(((Map<Object, Object>) source).entrySet())
                    .map(new $.Transformer<Map.Entry, $.Triple<Object, Keyword, $.Producer<Object>>>() {
                        @Override
                        public $.Triple<Object, Keyword, $.Producer<Object>> transform(final Map.Entry entry) {
                            $.Producer<Object> producer = new $.Producer<Object>() {
                                @Override
                                public Object produce() {
                                    return entry.getValue();
                                }
                            };
                            Object key = entry.getKey();
                            Keyword keyword = null;
                            if (rule.keywordMatching() && key instanceof String) {
                                keyword = Keyword.of(key.toString());
                            }
                            return $.T3(key, keyword, producer);
                        }
                    });
        } else {
            final List<Field> fields = $.fieldsOf(sourceType);
            return C.list(fields)
                    .filter(fieldFilter())
                    .map(new $.Transformer<Field, $.Triple<Object, Keyword, $.Producer<Object>>>() {
                        @Override
                        public $.Triple<Object, Keyword, $.Producer<Object>> transform(final Field field) {
                            Object name = field.getName();
                            Keyword keyword = null;
                            if (rule.keywordMatching()) {
                                keyword = Keyword.of(name.toString());
                            }
                            $.Producer<Object> producer = new $.Producer<Object>() {
                                @Override
                                public Object produce() {
                                    return $.getFieldValue(source, field);
                                }
                            };
                            return $.T3(name, keyword, producer);
                        }
                    });
        }
    }

    private $.Predicate<Field> fieldFilter() {
        if (filter.allEmpty) {
            return $.F.yes();
        }
        return new $.Predicate<Field>() {
            @Override
            public boolean test(Field field) {
                String key = field.getName();
                if (null != globalFilter && !globalFilter.test(key)) {
                    return false;
                }
                String prefix = context.toString();
                if (S.notBlank(prefix)) {
                    key = S.pathConcat(prefix, '.', key);
                }
                return filter.test(key);
            }
        };
    }

    // preconditions:
    // 1. source component is not null
    // 2. source component can be assigned to target component type or
    // 3. source component can be converted to target component type and semantic is MAP
    private Object prepareTargetComponent(
            Object sourceComponent,
            Object targetComponent,
            Class targetComponentType,
            ParameterizedType targetComponentGenericType,
            boolean targetComponentIsContainer,
            String key
    ) {
        if (semantic.isShallowCopy() || isImmutable(targetComponentType)) {
            if (semantic.isMapping() && !$.is(sourceComponent).allowBoxing().instanceOf(targetComponentType)) {
                return convert(sourceComponent).to(targetComponentType);
            }
            return sourceComponent;
        }
        Object convertedTargetComponent = null;
        if (!targetComponentIsContainer && semantic.isMapping()) {
            convertedTargetComponent = convert(sourceComponent).to(targetComponentType);
        }
        if (null != convertedTargetComponent) {
            return convertedTargetComponent;
        }
        if (null != targetComponent) {
            if (targetComponentType.isInterface()) {
                Class realComponentType = targetComponent.getClass();
                if (Map.class == targetComponentType && HashMap.class != realComponentType) {
                    Map map = new HashMap();
                    map.putAll((Map) targetComponent);
                    targetComponent = map;
                } else if (List.class == targetComponentType && ArrayList.class != realComponentType) {
                    List list = new ArrayList();
                    list.addAll((List) targetComponent);
                    targetComponent = list;
                } else if (Set.class == targetComponentType && HashSet.class != realComponentType) {
                    Set set = new HashSet();
                    set.addAll((Set) targetComponent);
                    targetComponent = set;
                } else if (SortedMap.class == targetComponentType && TreeMap.class != realComponentType) {
                    Map map = new TreeMap();
                    map.putAll((Map) targetComponent);
                    targetComponent = map;
                } else if (SortedSet.class == targetComponentType && TreeSet.class != realComponentType) {
                    Set set = new TreeSet();
                    set.addAll((Set) targetComponent);
                    targetComponent = set;
                }
            }
            targetComponent = new DataMapper(sourceComponent, targetComponent, key, targetComponentGenericType, this).getTarget();
        } else {
            targetComponent = copyOrReferenceOf(sourceComponent, sourceComponent.getClass(), key, targetComponentType, targetComponentGenericType);
        }
        return targetComponent;
    }

    private void logError(Throwable cause) {
        if (!ignoreError) {
            mappingError(cause, "Error mapping from %s to %s", sourceType.getName(), targetType.getName());
        }
    }

    private void logError(Throwable cause, String message, Object... messageArgs) {
        if (!ignoreError) {
            mappingError(cause, message, messageArgs);
        }
    }

    private void logError(String message, Object... messageArgs) {
        if (!ignoreError) {
            mappingError(message, messageArgs);
        }
    }

    private void logMappingFailure() {
        logError("Error mapping from %s to %s", sourceType.getName(), targetType.getName());
    }

    private <T> T convertSourceTo(Class<T> type) {
        try {
            return convertStage(!ignoreError).to(type);
        } catch (Exception e) {
            logError(e, "Cannot convert source into " + type.getName());
            return null;
        }
    }

    private <T> T tryConvertSourceTo(Class<T> type) {
        return convertStage(false).to(type);
    }

    private $._ConvertStage<?> convertStage() {
        return convert(source, !ignoreError);
    }

    private $._ConvertStage<?> convertStage(boolean reportError) {
        return convert(source, reportError);
    }

    private $._ConvertStage<?> convert(Object source) {
        return convert(source, !ignoreError);
    }

    private $._ConvertStage<?> convert(Object source, boolean reportError) {
        $._ConvertStage stage = $.convert(source).customTypeConverters(typeConverterRegistry).hint(convertHintOf(sourceType));
        if (!reportError) {
            stage.reportError();
        }
        return stage;
    }

    private Object convertHintOf(Class type) {
        return conversionHints.get(type);
    }

    private void mappingError(Throwable cause, String message, Object... messageArgs) {
        throw new MappingException(source, target, cause, message, messageArgs);
    }

    private MappingException mappingError(String message, Object... messageArgs) {
        throw new MappingException(source, target, message, messageArgs);
    }

    private void probeTargetType() {
        targetIsArray = targetType.isArray();
        targetCollection = !targetIsArray && Collection.class.isAssignableFrom(targetType) ? (Collection) target : null;
        targetIsCollection = null != targetCollection;
        targetList = targetIsCollection && List.class.isAssignableFrom(targetType) ? (List) target : null;
        targetIsList = null != targetList;
        targetMap = !targetIsArray && null == targetCollection && Map.class.isAssignableFrom(targetType) ? (Map) target : null;
        targetIsMap = null != targetMap;
        targetIsPojo = !targetIsArray && !targetIsCollection && !targetIsMap;
        if (targetIsArray) {
            targetLength = Array.getLength(target);
            targetComponentRawType = targetType.getComponentType();
        } else {
            if (targetIsPojo) {
                targetFields = $.fieldsOf(targetType, rootClass, true);
            }
            if (null != targetGenericType) {
                Type[] ta = targetGenericType.getActualTypeArguments();
                Type componentType = null;
                if (null != targetMap) {
                    targetKeyType = (Class) ta[0];
                    componentType = ta[1];
                } else if (null != targetCollection) {
                    componentType = ta[0];
                }
                if (null != componentType) {
                    if (componentType instanceof ParameterizedType) {
                        targetComponentType = (ParameterizedType) componentType;
                        targetComponentRawType = (Class) targetComponentType.getRawType();
                    } else {
                        targetComponentRawType = (Class) componentType;
                    }
                }
            }
            if (targetList != null) {
                targetLength = targetList.size();
            }
        }
        if (targetIsMap) {
            if (null == targetKeyType) {
                targetKeyType = $.commonSuperTypeOf(targetMap.keySet());
                if (null == targetKeyType) {
                    targetKeyType = String.class;
                }
            }
            if (null == targetComponentRawType) {
                targetComponentRawType = $.commonSuperTypeOf(targetMap.values());
                if (null == targetComponentRawType) {
                    targetComponentRawType = Object.class;
                }
            }
        }
        if (targetIsCollection) {
            if (null == targetComponentRawType) {
                targetComponentRawType = $.commonSuperTypeOf(targetCollection);
            }
        }
    }

    private Object copyOrReferenceOf(Object source) {
        return copyOrReferenceOf(source, "", null, null);
    }

    private Object copyOrReferenceOf(Object source, String targetName, Class targetType, ParameterizedType targetGenericType) {
        return copyOrReferenceOf(source, source.getClass(), targetName, targetType, targetGenericType);
    }

    private Object copyOrReferenceOf(Object source, Class sourceType, String targetName, Class targetType, ParameterizedType targetGenericType) {
        if (semantic.isShallowCopy() || isImmutable(sourceType)) {
            return source;
        }
        Object target;
        if (targetType.isArray()) {
            int len;
            if (sourceType.isArray()) {
                len = Array.getLength(source);
            } else if (Collection.class.isAssignableFrom(sourceType)) {
                len = ((Collection) source).size();
            } else {
                throw new UnexpectedException("oops, how come source is not a array/collection??");
            }
            target = Array.newInstance(targetType.getComponentType(), len);
        } else {
            try {
                target = instanceFactory.apply(targetType);
            } catch (Exception e) {
                return source;
            }
        }
        return new DataMapper(source, target, targetName, targetGenericType, this).getTarget();
    }

    private static boolean isSequence(Class<?> type) {
        return type.isArray() || Collection.class.isAssignableFrom(type);
    }

    private static boolean isMap(Class<?> type) {
        return Map.class.isAssignableFrom(type);
    }

    private static boolean isContainer(Class<?> type) {
        return isSequence(type) || isMap(type);
    }

    private static Class elementTypeOf(Object o) {
        Class<?> type = o.getClass();
        if (type.isArray()) {
            return type.getComponentType();
        }
        Collection collection = (Collection) o;
        return $.commonSuperTypeOf(collection);
    }

    private static boolean isImmutable(Class<?> type) {
        return !type.isArray() && ($.isSimpleType(type) || Lang.isImmutable(type));
    }

}
