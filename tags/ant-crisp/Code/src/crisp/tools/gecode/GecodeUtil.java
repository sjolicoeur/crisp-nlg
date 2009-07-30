/*
 * @(#)GecodeUtil.java created 06.10.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package crisp.tools.gecode;

import java.util.ArrayList;
import java.util.List;

import org.gecode.BoolVar;
import org.gecode.SetVar;

public class GecodeUtil {
    public static List<Integer> getSafeMembers(SetVar var, int maxValue) {
        List<Integer> ret = new ArrayList<Integer>();
        
        for( int i = 0; i < maxValue; i++ ) {
            if( var.contains(i)) {
                ret.add(i);
            }
        }
        
        return ret;
    }
    
    public static List<Integer> getPossibleMembers(SetVar var, int maxValue) {
        List<Integer> ret = new ArrayList<Integer>();
        
        for( int i = 0; i < maxValue; i++ ) {
            if( !var.notContains(i) && !var.contains(i)) {
                ret.add(i);
            }
        }
        
        return ret;
    }

    public static String getBooleanStatus(BoolVar var) {
    	if( var.assigned() ) {
    		return "" + var.val();
    	} else {
    		return "?";
    	}
    }
}
