/*
 * @(#)Literal.java created 30.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem.goal;

import java.util.Collection;
import java.util.List;

import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.parser.TermParser;


public class Literal extends Goal {
    private Term atom;
    private boolean polarity;

    private Literal() {

    }

    public Literal(Term atom, boolean polarity) {
        this.atom = atom;
        this.polarity = polarity;
    }

    public Literal(String atom, boolean polarity) {
    	this.atom = TermParser.parse(atom);
    	this.polarity = polarity;
    }

    @Override
    public Goal instantiate(Substitution subst) {
        Literal ret = new Literal();

        ret.atom = subst.apply(atom);
        ret.polarity = polarity;

        return ret;
    }

    @Override
    public String toString() {
        return (polarity ? "" : "~") + atom.toString();
    }

    public Term getAtom() {
        return atom;
    }

    public boolean getPolarity() {
        return polarity;
    }

    @Override
    void computeGoalList(List<Literal> goals, Problem problem) {
        goals.add(this);
    }

    public void getPositiveTerms(List<Term> terms) {
        terms.add(this.atom);
    }

    
    @Override
    public boolean isStaticallySatisfied(Problem problem, Collection<Predicate> staticPredicates) {
        if( isStatic(problem, staticPredicates)) {
            return problem.getInitialState().contains(atom) == polarity;
        }

        return false;
    }

    @Override
    public boolean isStatic(Problem problem, Collection<Predicate> staticPredicates) {
        for( Predicate pred : staticPredicates ) {
            if( pred.toTerm().getUnifier(atom) != null ) {
                return true;
            }
        }

        return false;
    }

}
