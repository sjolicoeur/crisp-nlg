/*
 * @(#)Universal.java created 01.10.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem.effect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.SubstitutionIterator;
import crisp.planningproblem.TypedVariableList;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;


public class Universal extends Effect {
    private final TypedVariableList variables;
    private final Effect effect;

    public Universal(TypedVariableList variables, Effect effect ) {
        this.variables = variables;
        this.effect = effect;
    }



    @Override
    public Effect instantiate(Substitution subst) {
        Map<Variable,Term> valuesForBoundVariables = new HashMap<Variable,Term>();

        for( Variable v : variables ) {
            if( subst.appliesTo(v) ) {
                valuesForBoundVariables.put(v, subst.apply(v));
                subst.remove(v);
            }
        }

        Universal ret = new Universal(variables, effect.instantiate(subst));

        for( Map.Entry<Variable,Term> entry : valuesForBoundVariables.entrySet() ) {
            subst.addSubstitution(entry.getKey(), entry.getValue());
        }

        return ret;
    }

    @Override
    public String toString() {
        return "forall(" + variables + ", " + effect + ")";
    }


    @Override
    void computeEffectList(List<Literal> eff, Problem problem) {
        Domain domain = problem.getDomain();
    	Iterator<Substitution> substitutions = new SubstitutionIterator(variables, domain.getUniverse(), domain.getTypeHierarchy());

    	while( substitutions.hasNext() ) {
    		effect.instantiate(substitutions.next()).computeEffectList(eff, problem);
    	}
    }



	@Override
	public boolean mentionsPredicate(Predicate pred) {
        return effect.mentionsPredicate(pred);
	}



    public TypedVariableList getVariables() {
        return variables;
    }



    public Effect getScope() {
        return effect;
    }



}
