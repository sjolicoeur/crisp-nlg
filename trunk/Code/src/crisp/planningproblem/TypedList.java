/*
 * @(#)TypedList.java created 01.10.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

abstract public class TypedList<E> implements Iterable<E> {
    private final List<E> items;
    private final Map<E,String> types;

    public TypedList() {
        items = new ArrayList<E>();
        types = new HashMap<E,String>();
    }


    public void addItem(E var, String type) {
        items.add(var);
        types.put(var,type);
    }

    public List<E> getItems() {
        return items;
    }

    public String getType(E item) {
        return types.get(item);
    }



    /** access to underlying list **/

    public boolean contains(String arg0) {
        return items.contains(arg0);
    }


    public boolean containsAll(Collection< ? extends String > arg0) {
        return items.containsAll(arg0);
    }


    public E get(int arg0) {
        return items.get(arg0);
    }


    public int size() {
        return items.size();
    }


    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        for( E var : getItems() ) {
            buf.append(var + ":" + getType(var) + " ");
        }

        return buf.toString();
    }

    public String toLispString() {
    	StringBuffer buf = new StringBuffer();

    	for( E var : getItems() ) {
            buf.append(var + " - " + getType(var) + "  ");
        }

        return buf.toString();
    }


    public Iterator<E> iterator() {
        return items.iterator();
    }





}
