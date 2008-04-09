/*
 * @(#)TypeHierarchy.java created 30.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeHierarchy {
    public static final String TOP = "****TOP TYPE****";
    private final Map<String,String> subtypes;  // pairs (subtype -> supertype)

    public TypeHierarchy() {
        subtypes = new HashMap<String,String>();
        subtypes.put(TOP, null);
    }

    public void addSubtype(String subtype, String supertype) {
        if( !containsType(supertype) ) {
            System.err.println("Warning: Unknown supertype " + supertype );
        }

        subtypes.put(subtype, supertype);
    }

    public boolean isSubtypeOf(String subtype, String supertype) {
        if( subtype == null ) {
            return false;
        } else if( subtype.equals(supertype)) {
            return true;
        } else {
            return isSubtypeOf(subtypes.get(subtype), supertype);
        }
    }

    public boolean containsType(String type) {
        return subtypes.containsKey(type);
    }

    // returns all types except for "object"
    public Set<String> getTypes() {
    	return subtypes.keySet();
    }

    public String getDirectSupertype(String type) {
    	return subtypes.get(type);
    }

    public void clear() {
        subtypes.clear();
        subtypes.put(TOP, null);
    }

    @Override
    public String toString() {
        return subtypes.toString();
    }
}
