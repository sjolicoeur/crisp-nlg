package crisp.planner.lazygp;

import java.util.List;

import crisp.planningproblem.Action;
import crisp.planningproblem.Problem;
import crisp.planningproblem.effect.Literal;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Substitution;

public class ActionInstance {
	private final Action action;
	private final Substitution subst;
	private final Problem problem;

	private final int[] preconditionIndices;
	private final boolean[] preconditionPolarities;

	private int[] effectIndices = null;
	private boolean[] effectPolarities = null;

	private List<Literal> effects = null;
	private List<crisp.planningproblem.goal.Literal> preconds = null;

	private final int hashcode;

	public ActionInstance(Action action, Substitution subst, List<Integer> argumentsAsIndices, List<Boolean> argumentPolarities, Problem problem) {
		super();
		this.action = action;
		this.subst = subst;
		this.problem = problem;

		// store indices and polarities for this action instance's preconditions
		preconditionIndices = new int[argumentsAsIndices.size()];
		preconditionPolarities = new boolean[argumentsAsIndices.size()];

		for( int i = 0; i < argumentsAsIndices.size(); i++ ) {
			preconditionIndices[i] = argumentsAsIndices.get(i);
			preconditionPolarities[i] = argumentPolarities.get(i);
		}

		hashcode = toString().hashCode();
		// effects are analyzed later (see computeEffects)
	}

	public List<Literal> getEffects() {
		if( effects == null ) {
			//System.err.println("compute effects of " + toString());
			effects = action.instantiate(subst).getEffect().getEffects(problem);
		}

		return effects;
	}

	public List<crisp.planningproblem.goal.Literal> getPreconditions() {
		if( preconds == null ) {
			preconds = action.instantiate(subst).getPrecondition().getGoalList(problem);
		}

		return preconds;
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



	@Override
    public String toString() {
		return action.instantiate(subst).toString();
	}



	@Override
	public int hashCode() {
		// return toString().hashCode();
	    return hashcode;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
            return true;
        }
		if (obj == null) {
            return false;
        }
		if (getClass() != obj.getClass()) {
            return false;
        }

		return toString().equals(obj.toString());
	}

	public int[] getPreconditionIndices() {
		return preconditionIndices;
	}

	public boolean[] getPreconditionPolarities() {
		return preconditionPolarities;
	}

	public int[] getEffectIndices() {
		return effectIndices;
	}

	public boolean[] getEffectPolarities() {
		return effectPolarities;
	}

	public void computeEffects(AtomTable table) {
		if( effects == null ) {
			// only process the effects of each action instance once!
			List<crisp.planningproblem.effect.Literal> effects = getEffects();
			int numEffects = effects.size();
			int nextEffect = 0;

			effectIndices = new int[numEffects];
			effectPolarities = new boolean[numEffects];

			for( crisp.planningproblem.effect.Literal effect : effects ) {
				Compound c = (Compound) effect.getAtom();
				int index = table.getIndexForAtom(c);

				if( index == -1 ) {
					index = table.size();
					table.add(c);
				}

				effectIndices[nextEffect] = index;
				effectPolarities[nextEffect++] = effect.getPolarity();
			}
		}
	}


}
