/*
 * @(#)Problem.java created 30.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import crisp.planningproblem.goal.Goal;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;


public class Problem {
    private String name;
    private Domain domain;

    private HashSet<Integer> comgoalArities; // Keep track of observed arities of communicative goals
    
    private final List<Term> initialState;
    private Goal goal;

    public Problem() {
        name = null;
        domain = null;
        initialState = new ArrayList<Term>();
        goal = null;
        comgoalArities = new HashSet<Integer>();
    }

    public void clear() {
        initialState.clear();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDomain(String name) {
        domain = Domain.getDomainForName(name);
    }

    public void setGoal(Goal g) {
        goal = g;
    }

    public void addToInitialState(Term term) {
        initialState.add(term);
    }

    public void registerComgoalArity(int arity){
        comgoalArities.add(new Integer(arity));
    }
    
    @Override
    public String toString() {
        return "<PROBLEM " + name + " (domain: " + domain.getName() + "); init=" +
        initialState + "; goal=" + goal + ">";
    }

    public Domain getDomain() {
        return domain;
    }

    public Goal getGoal() {
        return goal;
    }

    public List<Term> getInitialState() {
        return initialState;
    }

    public String getName() {
        return name;
    }
   
    public Set<Integer> getComgoalArities() {
        return comgoalArities;
    }
    
    // for "equals" hack
    public void addEqualityLiterals() {
    	for( String individual : domain.getUniverse().keySet() ) {
    		Constant v = new Constant(individual);
    		List<Term> subterms = new ArrayList<Term>();
    		subterms.add(v);
    		subterms.add(v);

    		addToInitialState(new Compound("**equals**", subterms));
    	}
    }

}
