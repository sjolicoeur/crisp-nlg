package crisp.planner.naive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import crisp.planningproblem.Problem;
import crisp.planningproblem.goal.Conjunction;
import crisp.planningproblem.goal.Goal;
import crisp.planningproblem.goal.Literal;
import crisp.planningproblem.goal.Universal;
import de.saar.chorus.term.Compound;

public class AtomTable {
	private List<Compound> indexToInstance;
	//private Map<Compound,Integer> instanceToIndex;
	
	private Map<String,Map<Compound,Integer>> labelToInstanceToIndex;
	
	private int nextIndex;
	
	private Set<String> forbiddenPredicates;
	
	
	public AtomTable(Problem problem) {
		indexToInstance = new ArrayList<Compound>();
		//instanceToIndex = new HashMap<Compound, Integer>();
		labelToInstanceToIndex = new HashMap<String, Map<Compound,Integer>>();
		nextIndex = 0;
		//this.problem = problem;
		
		forbiddenPredicates = new HashSet<String>();
		analyzeGoal(problem.getGoal());
		
		/*
		// translate problem goal state into goalState array
		List<Literal> goals = problem.getGoal().getGoalList(problem);
		goalState = new boolean[goals.size()];
		for( Literal goal : goals ) {
			goalState[nextIndex] = goal.getPolarity();
			add((Compound) goal.getAtom());
		}
		*/
		
		
		//System.err.println("init: " + nextIndex + " atoms.");
	}

	private void analyzeGoal(Goal goal) {
		if (goal instanceof Conjunction) {
			List<Goal> goals = ((Conjunction) goal).getConjuncts();
			
			for( Goal conj : goals ) {
				if( conj instanceof Universal ) {
					Goal scope = ((Universal) conj).getScope();
					
					if( scope instanceof Literal ) {
						Literal lit = ((Literal) scope);
						if( lit.getPolarity() == false ) {
							forbiddenPredicates.add(((Compound) lit.getAtom()).getLabel());
							continue;
						}
					}
				}
				
				throw new RuntimeException("Can't handle this goal!");
			}
		} else {
			throw new RuntimeException("Can't handle this goal!");
		}
		
	}

	
	private int getIndexForAtom(Compound a) {
		Map<Compound,Integer> instanceToInt = labelToInstanceToIndex.get(a.getLabel());
		
		if( instanceToInt == null ) {
			return -1;
		} else {
			Integer ret = instanceToInt.get(a);

			if( ret == null ) {
				return -1;
			} else {
				return ret;
			}
		}
	}
	
	
	public Compound get(int i) {
		return indexToInstance.get(i);
	}
	
	public void add(Compound a) {
		String label = a.getLabel();
		
		Map<Compound,Integer> instanceToInt = labelToInstanceToIndex.get(label);
		if( instanceToInt == null ) {
			instanceToInt = new HashMap<Compound, Integer>();
			labelToInstanceToIndex.put(label, instanceToInt);
		}
		
		
		indexToInstance.add(a);
		instanceToInt.put(a, nextIndex);
		nextIndex++;
	}
	
	public int size() {
		return nextIndex;
	}

	public boolean[] setTrueAtoms(List<Compound> list) {
		for( Compound c : list ) {
			ensureAtomKnown(c);
		}
		
		boolean[] ret = new boolean[nextIndex];
		for( Compound c : list ) {
			ret[getIndexForAtom(c)] = true;
		}
		
		return ret;
	}
	
	private void ensureAtomKnown(Compound c) {
		if( getIndexForAtom(c) == -1 ) {
			add(c);
		}
	}
	
	public int countGoalMismatches(boolean[] trueAtoms, String pred) {
		int ret = 0;
		
		for( int i = 0; i < trueAtoms.length; i++ ) {
			if( trueAtoms[i]) {
				if( get(i).getLabel().equals(pred)) {
					ret++;
				}
			}
		}
		
		return ret;
	}

	public boolean isGoalState(boolean[] trueAtoms) {
		/*
		for( String pred : forbiddenPredicates ) {
			if( labelToInstanceToIndex.containsKey(pred) ) {
				for( int i : labelToInstanceToIndex.get(pred).values() ) {
					if( (i < trueAtoms.length) && trueAtoms[i] ) {
						return false;
					}
				}
			}
		}
		*/

		
		for( int i = 0; i < trueAtoms.length; i++ ) {
			if( trueAtoms[i] ) {
				if( forbiddenPredicates.contains(get(i).getLabel()) ) {
					return false;
				}
			}
		}
		
		
		return true;
		
		/*
		for( int i = 0; i < goalState.length; i++ ) {
			if( goalState[i] != trueAtoms[i]) {
				return false;
			}
		}
		
		return true;
		*/
	}

	public boolean[] updateTrueAtoms(boolean[] trueAtoms, List<crisp.planningproblem.effect.Literal> effects) {
		int[] effectIndices = new int[effects.size()];
		boolean[] effectPolarities = new boolean[effects.size()];
		int ei = 0;
		
		for( crisp.planningproblem.effect.Literal effect : effects ) {
			Compound c = (Compound) effect.getAtom();
			int index = getIndexForAtom(c);
			
			effectPolarities[ei] = effect.getPolarity();
			
			if( index == -1 ) {
				effectIndices[ei++] = nextIndex;
				add(c);
			} else {
				effectIndices[ei++] = index;
			}
		}
		
		boolean[] ret = new boolean[nextIndex];
		for( int i = 0; i < trueAtoms.length; i++ ) {
			ret[i] = trueAtoms[i];
		}
		
		for( int i = 0; i < effects.size(); i++ ) {
			ret[effectIndices[i]] = effectPolarities[i];
		}
		
		// System.err.println("nextIndex now " + nextIndex);
		
		return ret;
	}
}
