package org.osgl.util;

/*-
 * #%L
 * Java Tool
 * %%
 * Copyright (C) 2014 - 2017 OSGL (Open Source General Library)
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

import org.osgl.$;
import org.osgl.exception.NotAppliedException;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.EnumSet;

public abstract class SetBase<T> extends AbstractSet<T> implements C.Set<T> {

    @Override
    public C.Set<T> parallel() {
        setFeature(C.Feature.PARALLEL);
        return this;
    }

    @Override
    public C.Set<T> sequential() {
        unsetFeature(C.Feature.PARALLEL);
        return this;
    }

    @Override
    public C.Set<T> lazy() {
        setFeature(C.Feature.LAZY);
        return this;
    }

    @Override
    public C.Set<T> eager() {
        unsetFeature(C.Feature.LAZY);
        return this;
    }

    @Override
    public C.Set<T> filter($.Function<? super T, Boolean> predicate) {
        boolean immutable = isImmutable();
        int sz = size();
        // TODO: handle lazy filter
        if (immutable) {
            if (0 == sz) {
                return Nil.set();
            }
            ListBuilder<T> lb = new ListBuilder<T>(sz);
            forEach($.visitor($.predicate(predicate).ifThen(C.F.addTo(lb))));
            return lb.toSet();
        } else {
            if (0 == sz) {
                return C.newSet();
            }
            C.Set<T> set = C.newSet();
            forEach($.visitor($.predicate(predicate).ifThen(C.F.addTo(set))));
            return set;
        }
    }

    @Override
    public SetBase<T> accept($.Visitor<? super T> visitor) {
        return forEach(visitor);
    }

    @Override
    public <R> C.Traversable<R> map($.Function<? super T, ? extends R> mapper) {
        boolean immutable = isImmutable();
        int sz = size();
        if (immutable) {
            if (0 == sz) {
                return Nil.set();
            }
            ListBuilder<R> lb = new ListBuilder<>(sz);
            forEach($.visitor($.f1(mapper).andThen(C.F.addTo(lb))));
            return lb.toList();
        } else {
            if (0 == sz) {
                return C.newSet();
            }
            C.List<R> l = C.newSizedList(sz);
            forEach($.visitor($.f1(mapper).andThen(C.F.addTo(l))));
            return l;
        }
    }

    @Override
    public <R> C.Traversable<R> flatMap($.Function<? super T, ? extends Iterable<? extends R>> mapper) {
        C.List<R> list = C.newList();
        for (T t : this) {
            Iterable<? extends R> iterable = mapper.apply(t);
            list.addAll(C.list(iterable));
        }
        return list;
    }

    @Override
    public <R> C.Traversable<R> collect(String path) {
        C.List<R> list = C.newList();
        for (T t : this) {
            list.add((R) $.getProperty(t, path));
        }
        return list;
    }

    @Override
    public <R> R reduce(R identity, $.Func2<R, T, R> accumulator) {
        return null;
    }

    @Override
    public $.Option<T> reduce($.Func2<T, T, T> accumulator) {
        return null;
    }

    @Override
    public boolean allMatch($.Function<? super T, Boolean> predicate) {
        return false;
    }

    @Override
    public boolean anyMatch($.Function<? super T, Boolean> predicate) {
        return false;
    }

    @Override
    public boolean noneMatch($.Function<? super T, Boolean> predicate) {
        return false;
    }

    @Override
    public $.Option<T> findOne($.Function<? super T, Boolean> predicate) {
        return null;
    }


    // utilities
    protected final boolean isLazy() {
        return is(C.Feature.LAZY);
    }

    protected final boolean isImmutable() {
        return is(C.Feature.IMMUTABLE);
    }

    protected final boolean isReadOnly() {
        return is(C.Feature.READONLY);
    }

    protected final boolean isMutable() {
        return !isImmutable() && !isReadOnly();
    }

    /**
     * Sub class could override this method to implement iterating in parallel.
     *
     * <p>The iterating support partial function visitor by ignoring the
     * {@link org.osgl.exception.NotAppliedException} thrown out by visitor's apply
     * method call</p>
     *
     * @param visitor the visitor
     * @throws $.Break if visitor needs to terminate the iteration
     */
    public SetBase<T> forEach($.Visitor<? super T> visitor) throws $.Break {
        for (T t : this) {
            try {
                visitor.apply(t);
            } catch (NotAppliedException e) {
                // ignore
            }
        }
        return this;
    }

    @Override
    public SetBase<T> each($.Visitor<? super T> visitor) {
        return forEach(visitor);
    }

    @Override
    public C.Set<T> onlyIn(Collection<? extends T> col) {
        C.Set<T> others = C.newSet(col);
        others.removeAll(this);
        if (isImmutable()) {
            return ImmutableSet.of(others);
        }
        return others;
    }

    @Override
    public C.Set<T> withIn(Collection<? extends T> col) {
        C.Set<T> others = C.newSet(col);
        others.retainAll(this);
        if (isImmutable()) {
            return ImmutableSet.of(others);
        }
        return others;
    }

    @Override
    public C.Set<T> without(Collection<? super T> col) {
        C.Set<T> copy = C.newSet(this);
        copy.removeAll(col);
        if (isImmutable()) {
            return ImmutableSet.of(copy);
        }
        return copy;
    }

    @Override
    public C.Set<T> with(Collection<? extends T> col) {
        C.Set<T> copy = C.newSet(this);
        copy.addAll(col);
        if (isImmutable()) {
            return ImmutableSet.of(copy);
        }
        return copy;
    }

    @Override
    public C.Set<T> with(T element) {
        C.Set<T> copy = C.newSet(this);
        copy.add(element);
        return copy;
    }

    @Override
    public C.Set<T> with(T element, T... elements) {
        C.Set<T> copy = C.newSet(this);
        copy.add(element);
        copy.addAll(C.listOf(elements));
        return copy;
    }

    @Override
    public C.Set<T> without(T element) {
        return null;
    }

    @Override
    public C.Set<T> without(T element, T... elements) {
        return null;
    }

    // --- Featured methods

    volatile private EnumSet<C.Feature> features_;

    protected final EnumSet<C.Feature> features_() {
        if (null == features_) {
            synchronized (this) {
                if (null == features_) {
                    features_ = initFeatures();
                    assert(null != features_);
                }
            }
        }
        return features_;
    }

    /**
     * Sub class should override this method to provide initial feature
     * set for the feature based instance
     *
     * @return the initial feature set configuration
     */
    abstract protected EnumSet<C.Feature> initFeatures();

    @Override
    public final EnumSet<C.Feature> features() {
        return EnumSet.copyOf(features_());
    }

    @Override
    public final boolean is(C.Feature feature) {
        return features_().contains(feature);
    }

    protected SetBase<T> setFeature(C.Feature feature) {
        features_().add(feature);
        return this;
    }

    protected SetBase<T> unsetFeature(C.Feature feature) {
        features_().remove(feature);
        return this;
    }

    // --- eof Featured methods



}
