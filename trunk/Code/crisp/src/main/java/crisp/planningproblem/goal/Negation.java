package crisp.planningproblem.goal;

import java.util.Collection;
import java.util.List;

import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;


public class Negation extends Goal {
	private final Goal subformula;


	public Goal getSubformula() {
		return subformula;
	}

	public Negation(Goal subformula) {
		super();
		this.subformula = subformula;
	}

	@Override
	void computeGoalList(List<Literal> goals, Problem problem) {
		throw new UnsupportedOperationException("Negation goals may only be used in the static antecedent of a conditional effect");

	}

	@Override
	public Goal instantiate(Substitution subst) {
		return new Negation(subformula.instantiate(subst));
	}

	@Override
	public boolean isStatic(Problem problem,
			Collection<Predicate> staticPredicates) {
		return subformula.isStatic(problem, staticPredicates);
	}

	@Override
	public boolean isStaticallySatisfied(Problem problem,
			Collection<Predicate> staticPredicates) {
		return ! subformula.isStaticallySatisfied(problem, staticPredicates);
	}

    @Override
    public void getPositiveTerms(List<Term> terms) {
        return;
    }
    
	@Override
    public String toString() {
		return "~" + subformula.toString();
	}


}
