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

public class TypedList implements Iterable<String> {
    private List<String> items;
    private Map<String,String> types;
    
    public TypedList() {
        items = new ArrayList<String>();
        types = new HashMap<String,String>();
    }
    

    public void addItem(String var, String type) {
        items.add(var);
        types.put(var,type);
    }

    public List<String> getItems() {
        return items;
    }
    
    public String getType(String item) {
        return types.get(item);
    }
    
    
    
    /** access to underlying list **/
    
    public boolean contains(String arg0) {
        return items.contains(arg0);
    }


    public boolean containsAll(Collection< ? extends String > arg0) {
        return items.containsAll(arg0);
    }


    public String get(int arg0) {
        return items.get(arg0);
    }


    public int size() {
        return items.size();
    }


    public String toString() {
        StringBuffer buf = new StringBuffer();
        
        for( String var : getItems() ) {
            buf.append(var + ":" + getType(var) + " ");
        }
        
        return buf.toString();
    }
    
    public String toLispString() {
    	StringBuffer buf = new StringBuffer();
    	
    	for( String var : getItems() ) {
            buf.append(var + " - " + getType(var) + "  ");
        }
        
        return buf.toString();
    }


    public Iterator<String> iterator() {
        return items.iterator();
    }


	
    
    
}
