package crisp.planner.lazygp;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class ByRemainingGoalsComparator implements Comparator<ActionInstance> {
	private Map<ActionInstance, Set<Integer>> goalsPerInstance;
	private State finalState;

	public ByRemainingGoalsComparator(Map<ActionInstance, Set<Integer>> goalsPerInstance, State finalState) {
		this.goalsPerInstance = goalsPerInstance;
		this.finalState = finalState;
	}

	public int compare(ActionInstance arg0, ActionInstance arg1) {
		int step0 = finalState.getFirstPossibleStateWithPreconditions(arg0);
		int step1 = finalState.getFirstPossibleStateWithPreconditions(arg1);
		
		assert step0 != -1;
		assert step1 != -1;
		
		if( step0 != step1 ) {
			return step1 - step0;
		} else {
			return goalsPerInstance.get(arg0).size() - goalsPerInstance.get(arg1).size();
		}
	}

}
