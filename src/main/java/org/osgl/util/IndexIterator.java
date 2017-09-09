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

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: luog
 * Date: 10/11/13
 * Time: 8:31 PM
 * To change this template use File | Settings | File Templates.
 */
class IndexIterator implements Iterator<Integer> {
    private Iterator<?> itr;
    int cursor = 0;

    IndexIterator(Iterator<?> itr) {
        E.NPE(itr);
        this.itr = itr;
    }

    @Override
    public boolean hasNext() {
        return itr.hasNext();
    }

    @Override
    public Integer next() {
        itr.next();
        return cursor++;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
