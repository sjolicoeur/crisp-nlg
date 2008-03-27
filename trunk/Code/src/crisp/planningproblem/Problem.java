/*
 * @(#)Problem.java created 30.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem;

import java.util.ArrayList;
import java.util.List;

import crisp.planningproblem.goal.Goal;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;


public class Problem {
    private String name;
    private Domain domain;

    private final List<Term> initialState;
    private Goal goal;

    public Problem() {
        name = null;
        domain = null;
        initialState = new ArrayList<Term>();
        goal = null;
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
