/*
 * @(#)Literal.java created 30.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem.effect;

import java.util.List;

import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.parser.TermParser;

public class Literal extends Effect {
    private Term atom;
    private boolean polarity;

    public Term getAtom() {
        return atom;
    }

    public boolean getPolarity() {
        return polarity;
    }

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
    public void getPositiveTerms(List<Term> terms){
        if (polarity) {
            terms.add(this.atom);
        }
    }

    @Override
    public Effect instantiate(Substitution subst) {
        Literal ret = new Literal();

        ret.atom = subst.apply(atom);
        ret.polarity = polarity;

        return ret;
    }

    @Override
    public String toString() {
        return (polarity ? "" : "~") + atom.toString();
    }

    @Override
    void computeEffectList(List<Literal> eff, Problem problem) {
        eff.add(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Literal) {
            Literal lit = (Literal) o;

            return atom.equals(lit.atom) && (polarity == lit.polarity);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return atom.hashCode() + (polarity ? 1 : 0);
    }

	@Override
	public boolean mentionsPredicate(Predicate pred) {
		/*
		 *
		 if( pred.getLabel().equals("confusable")) {
			System.err.println("comparison of " + pred + " against " + atom);
		}
		*/

		Compound cterm = (Compound) atom;

		return pred.getLabel().equals(cterm.getLabel())
			&& pred.getVariables().size() == cterm.getSubterms().size();
	}


}
