/*
 * @(#)Domain.java created 30.09.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package crisp.planningproblem;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.saar.basic.StringTools;
import de.saar.chorus.term.Substitution;


public class Domain {
    private static Map<String,Domain> domains = new HashMap<String,Domain>();
    
    private String name;
    private List<String> requirements;
    private TypeHierarchy types;
    private Map<String,String> constants;  // constant -> type
    private Set<Predicate> predicates;
    
    private List<String> constantsList;
    
    private Set<Predicate> staticPredicates;
    
    private List<Substitution> emptySubstitutionList;
    
    private List<Action> actions;
    
    public static Domain getDomainForName(String name) {
        return domains.get(name);
    }
    
    public void clear() {
        name = null;
        requirements.clear();
        types.clear();
        constants.clear();
        predicates.clear();
        actions.clear();
        
        // "equals" hack
        Predicate equals = new Predicate();
        equals.setLabel("**equals**");
        equals.addVariable("x", "object");
        equals.addVariable("y", "object");
        predicates.add(equals);
    }
    
    public Domain() {
        requirements = new ArrayList<String>();
        types = new TypeHierarchy();
        constants = new HashMap<String,String>();
        constantsList = new ArrayList<String>();
        predicates = new HashSet<Predicate>();
        actions = new ArrayList<Action>();
        staticPredicates = null;
        
        emptySubstitutionList = new ArrayList<Substitution>();
        emptySubstitutionList.add(new Substitution());
    }
    
    public void setName(String name) {
        this.name = name;
        
        
        domains.put(name, this);
    }
    
    public void addRequirement(String requirement) {
        requirements.add(requirement);
    }
    
    public void addSubtype(String subtype, String supertype) {
        types.addSubtype(subtype, supertype);
    }
    
    public void addConstant(String name, String type) {
        constants.put(name, type);
        constantsList.add(name);
    }
    
    public int getConstantIndex(String name) {
    	for( int i = 0; i < constantsList.size(); i++ ) {
    		if( constantsList.get(i).equals(name)) {
    			return i;
    		}
    	}
    	
    	return -1;
    }
    
    public String getConstantForIndex(int i) {
    	if( i >= 0 ) {
    		return constantsList.get(i);
    	} else {
    		return null;
    	}
    }
    
    public String constantIndicesToString(int[] tuple) {
    	StringBuilder buf = new StringBuilder();
    	
    	for( int j = 0; j < tuple.length; j++ ) {
			if( j > 0 ) {
				buf.append(",");
			}
			
			buf.append(getConstantForIndex(tuple[j]));
		}
    	
    	return buf.toString();
    }
    
    public void addPredicate(Predicate pred) {
        predicates.add(pred);
    }
    
    public void addAction(Action a) {
        actions.add(a);
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer("Domain " + name + ":\n");
        buf.append("  requirements: " + requirements + "\n");
        buf.append("  types: " + types + "\n");
        buf.append("  constants: " + constants + "\n");
        buf.append("  predicates: " + predicates + "\n");
        buf.append("  actions: " + actions + "\n");
        
        return buf.toString();
    }

    public List<Action> getActions() {
        return actions;
    }
    
    public Set<Predicate> getPredicates() {
        return predicates;
    }
    
    public Iterator<Substitution> getSubstitutionsFor(Action a) {
        if( a.getPredicate().getVariables().size() == 0 ) {
            return emptySubstitutionList.iterator();
        } else {
            return new SubstitutionIterator(a.getPredicate().getVariables(),
                    constants, types);
        }
    }
    
    public Iterator<Substitution> getSubstitutionsFor(Predicate p) {
        if( p.getVariables().size() == 0 ) {
            return emptySubstitutionList.iterator();
        } else {
            return new SubstitutionIterator(p.getVariables(),
                    constants, types);
        }
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getUniverse() {
        return constants;
    }
    
    public Collection<String> getUniverse(String type) {
    	List<String> ret = new ArrayList<String>();
    	
    	for( String c : constants.keySet() ) {
    		if( constants.get(c).equals(type)) {
    			ret.add(c);
    		}
    	}
    	
    	return ret;
    }

	public TypeHierarchy getTypeHierarchy() {
		return types;
	}
	
	// The static predicates are cached when this method is first called. If we do
	// anything to the domain after this first call that can possibly change the 
	// staticness of predicates, we need to expire the static predicates.
	public Set<Predicate> getStaticPredicates() {
	//	staticPredicates = null; // debugging
		
		if( staticPredicates != null ) {
			return staticPredicates;
		} else {
			Set<Predicate> ret = new HashSet<Predicate>();

			for( Predicate pred : predicates ) {
				boolean isStatic = true;
				
				for( Action action : actions ) {
					//if( pred.getLabel().equals("confusable")) System.err.println("Does " + action + " (" + action.getEffect() + ") mention " + pred + "? -> " + action.getEffect().mentionsPredicate(pred));
					if( action.getEffect().mentionsPredicate(pred) ) {
						isStatic = false;
					}
				}
				
				if( isStatic ) {
					ret.add(pred);
				}
			}

			staticPredicates = ret;
			return ret;
		}
	}
	
	
	
	public void writePddl(PrintWriter writer) {
		writer.println("(define (domain " + name + ")");
		writer.println("        (:requirements " + StringTools.join(requirements, " ") + ")");
		
		writer.print("        (:types");
		for( String type : types.getTypes() ) {
			writer.print("  " + type + " - " + types.getDirectSupertype(type));
		}
		writer.println(")");
		
		writer.println("       (:predicates");
		for( Predicate pred : predicates ) {
			if( !pred.getLabel().equals("**equals**")) {
				writer.println("         (" + pred.getLabel() + " " + pred.getVariables().toLispString() + ")");
			}
		}
		writer.println("        )");
		
		writer.println("       (:constants");
		for( String constant : constants.keySet()) {
			writer.println("         " + constant + " - " + constants.get(constant));
		}
		writer.println("       )");
		
		for( Action a : actions ) {
			writer.println("\n" + a.toPddlString());
		}
		
		writer.println(")");
	}
}
