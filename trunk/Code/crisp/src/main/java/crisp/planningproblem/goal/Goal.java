/*
 * @(#)Goal.java created 30.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem.goal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;


public abstract class Goal {
    public abstract Goal instantiate(Substitution subst);

    public List<Literal> getGoalList(Problem problem) {
        List<Literal> ret = new ArrayList<Literal>();

        computeGoalList(ret, problem);
        return ret;
    }


    abstract void computeGoalList(List<Literal> goals, Problem problem);

    public abstract boolean isStaticallySatisfied(Problem problem, Collection<Predicate> staticPredicates);

    public abstract boolean isStatic(Problem problem, Collection<Predicate> staticPredicates);

    public abstract void getPositiveTerms(List<Term> terms);

}
