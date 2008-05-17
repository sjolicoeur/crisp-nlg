/*
 * @(#)Conjunction.java created 30.09.2006
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
import de.saar.basic.StringTools;
import de.saar.chorus.term.Substitution;


public class Conjunction extends Goal {
    private final List<Goal> conjuncts;

    public Conjunction() {
        this.conjuncts = new ArrayList<Goal>();
    }

    public Conjunction(List<Goal> conjuncts) {
        this.conjuncts = conjuncts;
    }



    @Override
    public Goal instantiate(Substitution subst) {
        Conjunction ret = new Conjunction();

        for( Goal g : conjuncts ) {
            ret.conjuncts.add(g.instantiate(subst));
        }

        return ret;
    }

    @Override
    public String toString() {
        return "and(" + StringTools.join(conjuncts, ",") + ")";
    }

    public List<Goal> getConjuncts() {
        return conjuncts;
    }

    @Override
    void computeGoalList(List<Literal> goals, Problem problem) {
        for( Goal sub : conjuncts ) {
            sub.computeGoalList(goals, problem);
        }

    }

    @Override
    public boolean isStaticallySatisfied(Problem problem, Collection<Predicate> staticPredicates) {
        for( Goal sub : conjuncts ) {
            if( !sub.isStaticallySatisfied(problem, staticPredicates)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isStatic(Problem problem, Collection<Predicate> staticPredicates) {
        for( Goal sub : conjuncts ) {
            if( !sub.isStatic(problem, staticPredicates)) {
                return false;
            }
        }

        return true;
    }

}
