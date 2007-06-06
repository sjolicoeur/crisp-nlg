/*
 * @(#)Universal.java created 01.10.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package crisp.planningproblem.goal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.SubstitutionIterator;
import crisp.planningproblem.TypedList;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;

public class Universal extends Goal {
    private TypedList variables;
    private Goal goal;
    
    public Universal(TypedList variables, Goal goal ) {
        this.variables = variables;
        this.goal = goal;
    }
    
    
    public Goal instantiate(Substitution subst) {
        Map<Variable,Term> valuesForBoundVariables = new HashMap<Variable,Term>();
        
        for( String var : variables ) {
            Variable v = new Variable(var);
            
            if( subst.appliesTo(v) ) {
                valuesForBoundVariables.put(v, subst.apply(v));
                subst.remove(v);
            }
        }
        
        Universal ret = new Universal(variables, goal.instantiate(subst));
        
        for( Map.Entry<Variable,Term> entry : valuesForBoundVariables.entrySet() ) {
            subst.addSubstitution(entry.getKey(), entry.getValue());
        }
        
        return ret;
    }
    
    public String toString() {
        return "forall(" + variables + ", " + goal + ")";
    }


    private Iterator<Substitution> getSubstitutions(Problem problem) {
        Domain domain = problem.getDomain();
        return new SubstitutionIterator(variables, domain.getUniverse(), domain.getTypeHierarchy());
    }


    @Override
    void computeGoalList(List<Literal> goals, Problem problem) {
    	Iterator<Substitution> substitutions = getSubstitutions(problem);
    	
    	while( substitutions.hasNext() ) {
            Substitution s = substitutions.next();
            
            //System.err.println("cgl for instance " + goal.instantiate(s));
            
    		goal.instantiate(s).computeGoalList(goals, problem);
    	}
    }


    @Override
    public boolean isStaticallySatisfied(Problem problem, Collection<Predicate> staticPredicates) {
        Iterator<Substitution> substitutions = getSubstitutions(problem);
        
        while( substitutions.hasNext() ) {
            if( !  goal.instantiate(substitutions.next()) .isStaticallySatisfied(problem, staticPredicates) ) {
                return false;
            }
        }
        
        return true;
    }


    @Override
    public boolean isStatic(Problem problem, Collection<Predicate> staticPredicates) {
        Iterator<Substitution> substitutions = getSubstitutions(problem);
        
        while( substitutions.hasNext() ) {
            if( !  goal.instantiate(substitutions.next()) .isStatic(problem, staticPredicates) ) {
                return false;
            }
        }
        
        return true;
    }
    

	@Override
	public String toPddlString() {
		return "(forall (" + variables.toLispString() + ") " + goal.toPddlString() + ")";
	}


	public Goal getScope() {
		return goal;
	}


	public void setGoal(Goal goal) {
		this.goal = goal;
	}


	public TypedList getVariables() {
		return variables;
	}


	public void setVariables(TypedList variables) {
		this.variables = variables;
	}
	
	

}
