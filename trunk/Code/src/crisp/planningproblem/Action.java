/*
 * @(#)Action.java created 30.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 * 
 * modified by Daniel Bauer, 06.02.2009
 */

package crisp.planningproblem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
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

    private HashMap<String,String> constants; // Store constants used by this action.
    private ArrayList<Predicate> predicates; // Store (semantic) predicates used by this action

    public Action(Predicate label, Goal precondition, Effect effect, HashMap<String, String> constants, ArrayList<Predicate> predicates) {
        this.effect = effect;
        this.label = label;
        this.precondition = precondition;
        this.constants = constants;
        this.predicates = predicates; 
    }
    
    
    public Action(Predicate label, Goal precondition, Effect effect) {
        this(label, precondition, effect, null, null); 
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

    public HashMap<String, String> getDomainConstants(){
        return constants;
    }

    public ArrayList<Predicate> getDomainPredicates(){
        return predicates;
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

    @Override
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (label == null) ? 0 : label.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if( this == obj ) {
            return true;
        }
        if( obj == null ) {
            return false;
        }
        if( getClass() != obj.getClass() ) {
            return false;
        }
        final Action other = (Action) obj;
        if( label == null ) {
            if( other.label != null ) {
                return false;
            }
        } else if( !label.equals(other.label) ) {
            return false;
        }
        return true;
    }



}
