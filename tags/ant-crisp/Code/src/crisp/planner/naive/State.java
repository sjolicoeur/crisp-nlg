package crisp.planner.naive;

import java.util.ArrayList;
import java.util.List;

import crisp.planningproblem.Action;
import crisp.planningproblem.Problem;
import crisp.planningproblem.goal.Literal;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Substitution;

public class State {
	//private Set<Compound> trueAtoms;
	boolean[] trueAtoms; 
	private List<ActionInstance> plan;
	private Problem problem;
	private AtomTable table;
	
	public State(Problem problem) {
		this.problem = problem;
		
		table = new AtomTable(problem);
		trueAtoms = table.setTrueAtoms((List) problem.getInitialState());
		
		//trueAtoms = new HashSet<Compound>((List) problem.getInitialState());  // TODO - argh
		
		plan = new ArrayList<ActionInstance>();
	}
	
	public State(State oldState, ActionInstance inst) {
		problem = oldState.problem;
		// trueAtoms = new HashSet<Compound>(oldState.trueAtoms);
		plan = new ArrayList<ActionInstance>(oldState.plan);
		table = oldState.table;
		
		trueAtoms = inst.apply(oldState);
		
		plan.add(inst);
	}
	
	public boolean isGoalState() {
		return table.isGoalState(trueAtoms);
		
		/*
		for( Literal lit : problem.getGoal().getGoalList(problem)) {
			if( lit.getPolarity() != trueAtoms.contains((Compound) lit.getAtom())) {
				return false;
			}
		}
		
		return true;
		*/
	}
	
	
	
	public List<ActionInstance> getApplicableActionInstances() {
		List<ActionInstance> ret = new ArrayList<ActionInstance>();
		
		for( Action a : problem.getDomain().getActions() ) {
			computeApplicableInstances(a.getPrecondition().getGoalList(problem), new Substitution(), a, ret);
		}
		
		return ret;
	}
	
	
	

	private void computeApplicableInstances(List<Literal> goalList, Substitution subst, Action a, List<ActionInstance> ret) {
		if( goalList.isEmpty() ) {
			ret.add(new ActionInstance(a, subst, problem));
		} else {
			Literal goal = goalList.remove(goalList.size()-1);
			Compound c = (Compound) subst.apply(goal.getAtom());
			
			if( goal.getPolarity() ) {
				for( int i = 0; i < trueAtoms.length; i++ ) {
					// OPTIMIZATION: only look at atoms for correct predicate.
					if( trueAtoms[i] ) {
						Compound trueAtom = table.get(i);
						Substitution unifier = trueAtom.getUnifier(c);

						if( unifier != null ) {
							computeApplicableInstances(goalList, subst.concatenate(unifier), a, ret);
						}
					}
				}
			} else {
				throw new RuntimeException("I don't know how to deal with negative preconditions!");
			}
			
			goalList.add(goal);
		}
	}

	/*
	public void addTrueAtom(Compound compound) {
		trueAtoms.add(compound);
	}
	
	public void removeTrueAtom(Compound compound) {
		trueAtoms.remove(compound);
	}
	*/

	public List<ActionInstance> getPlan() {
		return plan;
	}
	
	public Problem getProblem() {
		return problem;
	}
	
	boolean[] getTrueAtoms() {
		return trueAtoms;
	}
	
	AtomTable getAtomTable() {
		return table;
	}
	
	/*
	public Set<Compound> getTrueAtoms() {
		return trueAtoms;
	}
	*/
	
	public String toString() {
		StringBuilder ret = new StringBuilder();
		
		ret.append("State after plan " + plan.toString() + ":\n");
		
		/*
		for( Compound atom : trueAtoms ) {
			if( !atom.getLabel().equals("**equals**")) {
				ret.append("  " + atom + "\n");
			}
		}
		*/
		
		for( int i = 0; i < trueAtoms.length; i++ ) {
			if( trueAtoms[i] ) {
				Compound atom = table.get(i);
				if( !atom.getLabel().equals("**equals**")) {
					ret.append("  " + atom + "\n");
				}
			}
		}
		
		return ret.toString() + "\n";
	}
}
