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
		
		int synMismatch1=0, synMismatch2=0, distrMismatch1=0, distrMismatch2=0, needToExpress1=0, needToExpress2=0;
		
		for( int i = 0; i < ta1.length; i++ ) {
			if( ta1[i]) {
				String label = tab1.get(i).getLabel();
				
				if( label.equals("subst")) {
					synMismatch1++;
				} else if( label.equals("distractor")) {
					distrMismatch1++;
				} else if( label.startsWith("needtoexpress-")) {
					needToExpress1++;
				}
			}
		}
		
		for( int i = 0; i < ta2.length; i++ ) {
			if( ta2[i]) {
				String label = tab2.get(i).getLabel();
				
				if( label.equals("subst")) {
					synMismatch2++;
				} else if( label.equals("distractor")) {
					distrMismatch2++;
				} else if( label.startsWith("needtoexpress-")) {
					needToExpress2++;
				}
			}
		}
		
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
