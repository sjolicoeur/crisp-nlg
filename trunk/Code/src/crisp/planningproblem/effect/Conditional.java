/*
 * @(#)Conditional.java created 01.10.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem.effect;

import java.util.Collection;
import java.util.List;

import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.goal.Goal;
import de.saar.chorus.term.Substitution;

public class Conditional extends Effect {
    private final Goal condition;
    private final Effect effect;



    public Conditional(Goal condition, Effect effect) {
        super();
        // TODO Auto-generated constructor stub
        this.condition = condition;
        this.effect = effect;
    }


    @Override
    public Effect instantiate(Substitution subst) {
        return new Conditional(condition.instantiate(subst), effect.instantiate(subst));
    }

    @Override
    public String toString() {
        return "when(" + condition + ", " + effect + ")";
    }


    @Override
    void computeEffectList(List<Literal> eff, Problem problem) {
        Collection<Predicate> staticPredicates = problem.getDomain().getStaticPredicates();

        //System.err.println("Static predicates: " + staticPredicates);

        if( condition.isStatic(problem, staticPredicates)) {
            if( condition.isStaticallySatisfied(problem, staticPredicates)) {
                effect.computeEffectList(eff, problem);
            }
        } else {
            throw new UnsupportedOperationException("Conditional effects must have static preconditions, but " + condition + " is not static!");
        }
    }


	@Override
	public boolean mentionsPredicate(Predicate pred) {
        return effect.mentionsPredicate(pred);
	}


    public Goal getCondition() {
        return condition;
    }


    public Effect getEffect() {
        return effect;
    }


}
