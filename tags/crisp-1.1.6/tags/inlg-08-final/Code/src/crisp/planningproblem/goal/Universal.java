/*
 * @(#)Universal.java created 01.10.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem.goal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.SubstitutionIterator;
import crisp.planningproblem.TypeHierarchy;
import crisp.planningproblem.TypedVariableList;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;

public class Universal extends Goal {
    private TypedVariableList variables;
    private Goal goal;

    public Universal(TypedVariableList variables, Goal goal ) {
        this.variables = variables;
        this.goal = goal;
    }


    @Override
    public Goal instantiate(Substitution subst) {
        Map<Variable,Term> valuesForBoundVariables = new HashMap<Variable,Term>();

        for( Variable v : variables ) {
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

    @Override
    public String toString() {
        return "forall(" + variables + ", " + goal + ")";
    }


    public Iterator<Substitution> getSubstitutions(Problem problem) {
        Domain domain = problem.getDomain();
        return new SubstitutionIterator(variables, domain.getUniverse(), domain.getTypeHierarchy());
    }

    public Iterator<Substitution> getDestructiveGroundSubstitutions(Problem problem, Substitution subst) {
        return new DestructiveGroundSubstitutionIterator(problem, subst);
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


	public Goal getScope() {
		return goal;
	}


	public void setGoal(Goal goal) {
		this.goal = goal;
	}


	public TypedVariableList getVariables() {
		return variables;
	}


	public void setVariables(TypedVariableList variables) {
		this.variables = variables;
	}


	private class DestructiveGroundSubstitutionIterator implements Iterator<Substitution> {
	    private final Substitution subst;
	    private final List<List<String>> valuesForVariables;
	    private final int[] index; // each in 0 .. vfv[i].size()-1
	    private boolean finished;

	    protected DestructiveGroundSubstitutionIterator(Problem problem, Substitution subst) {
            super();
            this.subst = subst;

            Map<String,String> universe = problem.getDomain().getUniverse();
            TypeHierarchy types = problem.getDomain().getTypeHierarchy();

            valuesForVariables = new ArrayList<List<String>>(variables.size());
            for( int i = 0; i < variables.size(); i++ ) {
                List<String> thisUniverse = new ArrayList<String>();

                for( String individual : universe.keySet() ) {
                    if( types.isSubtypeOf(universe.get(individual), variables.getType(variables.get(i))) ) {
                        thisUniverse.add(individual);
                    }
                }

                valuesForVariables.add(thisUniverse);
            }

            index = new int[variables.size()];
            // all initialized with 0

            finished = false;
        }

        public boolean hasNext() {
            return !finished;
        }

        public Substitution next() {
            int carry = 0;

            for( int i = variables.size() - 1; i >= 0; i-- ) {
                subst.setSubstitution(variables.get(i),
                        new Constant(valuesForVariables.get(i).get(index[i])));

                if( i == variables.size() - 1 ) {
                    index[i]++;
                } else {
                    index[i] += carry;
                }

                if( index[i] > valuesForVariables.get(i).size() - 1 ) {
                    index[i] = 0;
                    carry = 1;
                } else {
                    carry = 0;
                }
            }

            if( carry == 1 ) {
                finished = true;
            }

            return subst;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

	}

}
