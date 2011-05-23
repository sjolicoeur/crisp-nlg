/*
 * @(#)Effect.java created 30.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem.effect;

import java.util.ArrayList;
import java.util.List;

import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;


public abstract class Effect {
    public abstract Effect instantiate(Substitution subst);

    public List<Literal> getEffects(Problem problem) {
        List<Literal> eff = new ArrayList<Literal>();

        computeEffectList(eff, problem);

        return eff;
    }

    /*
    public List<Literal> getNegativeEffects() {
        List<Literal> pos = new ArrayList<Literal>();
        List<Literal> neg = new ArrayList<Literal>();

        computeEffectList(pos, neg);

        return neg;
    }
    */

    public abstract void getPositiveTerms(List<Term> terms);
    
    abstract void computeEffectList(List<Literal> eff, Problem problem);

	public abstract boolean mentionsPredicate(Predicate pred);

}
