package crisp.planner.naive;

import java.util.List;

import crisp.planningproblem.Action;
import crisp.planningproblem.Problem;
import crisp.planningproblem.effect.Literal;
import de.saar.chorus.term.Substitution;

public class ActionInstance {
	private Action action;
	private Substitution subst;
	private Problem problem;
	
	
	
	public ActionInstance(Action action, Substitution subst, Problem problem) {
		super();
		this.action = action;
		this.subst = subst;
		this.problem = problem;
	}



	public boolean[] apply(State oldState) {
		List<Literal> effects = action.instantiate(subst).getEffect().getEffects(problem);
		return  oldState.getAtomTable().updateTrueAtoms(oldState.getTrueAtoms(), effects);
		
		/*
		for( Literal lit : effects ) {
			if( lit.getPolarity() ) {
				state.addTrueAtom((Compound) lit.getAtom());
			} else {
				state.removeTrueAtom((Compound) lit.getAtom());
			}
		}
		*/
	}
	
	
	
	public Action getAction() {
		return action;
	}



	public Problem getProblem() {
		return problem;
	}



	public Substitution getSubst() {
		return subst;
	}



	public String toString() {
		return action.instantiate(subst).toString();
	}



	@Override
	public int hashCode() {
		return toString().hashCode();
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		return toString().equals(obj.toString());
	}
	
	
}
