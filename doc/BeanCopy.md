# Copy/Map between two structures

OSGL tool provides flexible support to allow developer copy/map between any two data structures.

## 1. API at a glance

```java
// do shallow copy from `foo` to `bar`
$.copy(foo).to(bar);

// deep copy from `foo` to `bar
$.deepCopy(foo).to(bar);

// deep copy using loose name match
$.deepCopy(foo).looseMatching().to(bar);

// deep copy with filter
$.deepCopy(foo).filter("-password,-address.streetNo").to(bar);

// deep copy with special name mapping rule
$.deepCopy(foo)
    .map("id").to("no")
    .map("subject").to("title")
    .to(bar);

// merge data from `foo` to `bar`
$.merge(foo).to(bar);

// map data from `foo` to `bar`
$.map(foo).to(bar);

// map data from `foo` to `bar` using strict name match
$.map(foo).strictMatching().to(bar);

// merge map data from `foo` to `bar`
$.mergeMap(foo).to(bar);
```

## 2. Concept

### 2.1 Semantic

OSGL mapping framework support the following five different semantics:

1. `SHALLOW_COPY`, copy the first level fields
2. `DEEP_COPY`, copy recursively until immutable type reached
3. `MERGE`, similar to DEEP_COPY, but append elements from source container to target container including array
4. `MAP`, similar to `DEEP_COPY`, with value type conversion support
5. `MERGE_MAP`, similar to `MERGE`, with value type conversion support

#### 2.1.1 Immutable type

The following types are considered to be immutable types:

* primitive types
* wrapper type of primitive types
* String
* Enum
* Any type that has been regisered into `OsglConfig` via `registerImmutableClassNames` API
* Any type that when applied to the predicate function in `OsglConfig` which is registered via `registerImmutableClassPredicate($.Predicate<Class>)` API, cause `true` returned.

### 2.2 Name mapping

OSGL mapping framework support the following three different name mapping rules:

1. Strict matching, require source name be equal to target name
2. Keyword matching or loose matching, match keyword of two names. For example, the following names are considered to be match to each other
    * foo_bar
    * foo-bar
    * fooBar
    * FooBar
    * Foo-Bar
    * Foo_Bar
3. Special matching rules can be set for each mapping process to match completely two different names.

Here is an example of using special mapping rules:

```java
$.deepCopy(foo)
    .map("id").to("no")
    .map("subject").to("title")
    .to(bar);
```

The above call tells mapping framework to map `id` field in `foo` to `no` field in target `bar`, and map `subject` field in `foo` to `title` field in `bar`.

### 2.3 Filter

Filter can be used to skip copying/mapping certain fields. Filter is provided with a list of field names separated by `,`, if a field name is prefixed with `-` it means the field must not be copied/mapped. If the field were prefixed with `+` or without prefix, then it means the field shall be copied/mapped and the fields that are not mentioned shall NOT be copied/mapped. Examples:

* `-email,-password` - do not copy/map email and password fields, all other fields shall be copied/mapped
* `+email` - copy only email field, all other fields shall not be copied.
* `-cc.cvv` - do not copy `cvv` field in the instance of `cc` field, all other fields shall be copied
* `-cc,+cc.cvv` - copy `cvv` field in the instance of `cc` field, all other fields in the `cc` instance shall not be copied, all fields other than `cc` instance shall be copied.

To apply filter use the following API:

```java
$.deepCopy(foo).filter("-password,-address.streetNo").to(bar);
```

**Note** filter matches the field names in the target object.

### 2.4 root class

OSGL copy/mapping tool applied on fields instead of Getter/Setter methods. The exploring of fields of a bean is a recursive procedure till it reaches the `Object.class`. However there are cases that it needs to stop the fields exploring journey at a certain parent class. For example, suppose we have defined the following Base class:

```java
public abstract class ModelBase {
    public Date _created;
}
```

Your other model classes extends from `ModelBase`, and your Dao use the `_created` field to check whether the instance is new (when _created is `null`) or an existing record.

Now you want to copy an existing record int an new record to prepopulate that new record for updates, and in the end you will save the updated copy as an new record. Thus in this case you do not want to copy the `_created` field which is defined in `ModelBase`. Here is how to do it with `rootClass`:

```java
MyModel copy = $.copy(existing).rootClass(ModelBase.class).to(MyModel.class);
```

### 2.5 Target generic type

If you want to map from a container to another container with different element type, you need to provide the `targetGenericType` parameter to make it work:

```java
List<Foo> fooList = C.list(new Foo(), new Foo());
List<Bar> barList = C.newList();
$.map(fooList).targetGenericType(new TypeReference<List<Bar>>(){}).to(barList);
```

