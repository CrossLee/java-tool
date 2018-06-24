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
import org.osgl.Lang;

import java.io.*;

/**
 * Represent an immutable value
 * @param <T> the generic type of the object stored in the value
 */
public final class Const<T> implements Serializable {

    private T v;

    /**
     * Construct a Const with a value
     * @param value the value to be stored in the `Const`
     */
    private Const(T value) {
        v = value;
    }

    /**
     * Returns the object stored in this `Val`
     * @return the object stored
     */
    public T get() {
        return v;
    }

    @Override
    public String toString() {
        return S.string(v);
    }

    public Lang.Var<T> toVar() {
        return Lang.var(v);
    }

    public Lang.Val<T> toVal() {
        return Lang.val(v);
    }

    @Override
    public boolean equals(Object o) {
        return (this == o || ((o instanceof Const) && Lang.eq(((Const)o).v, v)));
    }

    @Override
    public int hashCode() {
        return Lang.hc(v);
    }

    public static <E> Const of(E t) {
        return new Const<E>(t);
    }

    public static <E> Const of(Lang.Var<E> var) {
        return null == var ? new Const<E>(null) : new Const<E>(var.get());
    }

    public static void main(String[] args) throws Exception {
        Const<String> x = $.constant("abc");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(x);
        byte[] ba = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        Const<String> y = (Const<String>)new ObjectInputStream(bais).readObject();
        System.out.println(y.get());
    }
}
