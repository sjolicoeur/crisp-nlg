/*
 * @(#)Action.java created 30.09.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package crisp.planningproblem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import crisp.planningproblem.effect.Effect;
import crisp.planningproblem.goal.Goal;
import crisp.planningproblem.goal.Literal;
import de.saar.chorus.term.Substitution;


public class Action {
    private Predicate label; 
    
    private Goal precondition;
    private Effect effect;

    
    
    
    public Action(Predicate label, Goal precondition, Effect effect) {
        this.effect = effect;
        this.label = label;
        this.precondition = precondition;
    }

    private Action() {
        
    }
    
    


    public Effect getEffect() {
        return effect;
    }

    public Predicate getPredicate() {
        return label;
    }

    public Goal getPrecondition() {
        return precondition;
    }
    
    

    public List<Literal> getDynamicGoalList(Problem problem) {
    	Domain dom = problem.getDomain();
    	Set<Predicate> staticPredicates = new HashSet<Predicate>(dom.getStaticPredicates());
    	
    	List<Literal> all = getPrecondition().getGoalList(problem);
    	List<Literal> ret = new ArrayList<Literal>(all.size());
    	
    	for( Literal lit : all ) {
    		if( ! lit.isStatic(problem, staticPredicates) ) {
    			ret.add(lit);
    		}
    	}
    	
    	return ret;
    }
    
    public boolean isStaticGoalsSatisfied(Problem problem) {
    	Domain dom = problem.getDomain();
    	Set<Predicate> staticPredicates = new HashSet<Predicate>(dom.getStaticPredicates());
    	
    	List<Literal> all = getPrecondition().getGoalList(problem);
    	
    	for( Literal lit : all ) {
    		if( lit.isStatic(problem, staticPredicates) ) {
    			if( !lit.isStaticallySatisfied(problem, staticPredicates) ) {
    				return false;
    			}
    		}
    	}
    	
    	return true;
    }
    
    

    public Action instantiate(Substitution subst) {
        Action ret = new Action();
        
        ret.label = label.instantiate(subst);
        ret.precondition = precondition.instantiate(subst);
        ret.effect = effect.instantiate(subst);
        
        return ret;
    }
    
    public String toString() {
        if( label.getVariables().size() == 0 ) {
            return label.getLabel();
        } else {
            return label.toString();
        }
    }
    
    public String getDescription() {
        return "<ACTION " + label + ": goals = " + precondition + ", effects = " + effect + ">";
    }

	public String toPddlString() {
		StringBuffer buf = new StringBuffer();
		String prefix = "      ";
		
		buf.append("   (:action " + label.getLabel() + "\n");
		buf.append(prefix + ":parameters (" + label.getVariables().toLispString() + ")\n");
		buf.append(prefix + ":precondition " + precondition.toPddlString() + "\n");
		buf.append(prefix + ":effect " + effect.toPddlString() + "\n");
		buf.append("   )\n");
		
		return buf.toString();
	}
}
