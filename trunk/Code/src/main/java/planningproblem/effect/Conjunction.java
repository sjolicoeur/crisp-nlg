/*
 * @(#)Conjunction.java created 30.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem.effect;

import java.util.ArrayList;
import java.util.List;

import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import de.saar.basic.StringTools;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;


public class Conjunction extends Effect {
    private final List<Effect> conjuncts;

    public Conjunction() {
        this.conjuncts = new ArrayList<Effect>();
    }

    public Conjunction(List<Effect> conjuncts) {
        this.conjuncts = conjuncts;
    }

    

    
    @Override
    public Effect instantiate(Substitution subst) {
        Conjunction ret = new Conjunction();

        for( Effect g : conjuncts ) {
            ret.conjuncts.add(g.instantiate(subst));
        }

        return ret;
    }

    @Override
    public String toString() {
        return "and(" + StringTools.join(conjuncts, ",") + ")";
    }

    @Override
    void computeEffectList(List<Literal> eff, Problem problem) {
        for( Effect sub : conjuncts ) {
            sub.computeEffectList(eff, problem);
        }
    }

    @Override
    public void getPositiveTerms(List<Term> terms){
        for( Effect sub : conjuncts) {
            sub.getPositiveTerms(terms);
        }
    }
    
	@Override
	public boolean mentionsPredicate(Predicate pred) {
        for( Effect sub : conjuncts ) {
            if( sub.mentionsPredicate(pred)) {
                return true;
            }
        }

		return false;
	}

    public List<Effect> getConjuncts() {
        return conjuncts;
    }

}
