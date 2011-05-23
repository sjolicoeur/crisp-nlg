package crisp.planner.naive;

import java.util.Comparator;

public class SpudComparator implements Comparator<State> {
	/*
	private int countMismatches(State state, String label) {
		Problem problem = state.getProblem();
		List<Literal> goals = problem.getGoal().getGoalList(problem);
		Set<Compound> stateAtoms = null; //state.getTrueAtoms();
		int count = 0;
		
		for( Literal goal : goals ) {
			if( (goal.getPolarity() != stateAtoms.contains((Compound) goal.getAtom()))
					&& ((Compound) goal.getAtom()).getLabel().startsWith(label) ) {
				count++;
			}
		}
		
		return count;
	}
	*/
	
	public int compare(State arg1, State arg2) {
		boolean[] ta1 = arg1.getTrueAtoms(), ta2 = arg2.getTrueAtoms();
		AtomTable tab1 = arg1.getAtomTable(), tab2 = arg2.getAtomTable();
		
		int synMismatch1 = tab1.countGoalMismatches(ta1, "subst"),
		synMismatch2 = tab2.countGoalMismatches(ta2, "subst"),
		distrMismatch1 = tab1.countGoalMismatches(ta1, "distractor"),
		distrMismatch2 = tab2.countGoalMismatches(ta2, "distractor"),
		needToExpress1 = tab1.countGoalMismatches(ta1, "needtoexpress-1") + tab1.countGoalMismatches(ta1, "needtoexpress-2") + tab1.countGoalMismatches(ta1, "needtoexpress-3"),
		needToExpress2 = tab2.countGoalMismatches(ta2, "needtoexpress-1") + tab2.countGoalMismatches(ta2, "needtoexpress-2") + tab2.countGoalMismatches(ta2, "needtoexpress-3");
		
		/*
		System.err.println("    state comparison: syn " + synMismatch1 + " vs " + synMismatch2 
				+ ", distr " + distrMismatch1 + " vs " + distrMismatch2
				+ ", nte " + needToExpress1 + " vs " + needToExpress2);
				*/
				
		
		if( needToExpress1 != needToExpress2 ) {
			return needToExpress1 - needToExpress2;
		}
		
		if( distrMismatch1 != distrMismatch2 ) {
			return distrMismatch1 - distrMismatch2;
		}
		
		if( synMismatch1 != synMismatch2 ) {
			return synMismatch1 - synMismatch2;
		}
		
		return 0;
	}

}
