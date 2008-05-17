package crisp.planner.naive;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import crisp.pddl.PddlParser;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;

public class Planner {
	private Queue<State> agenda = new LinkedList<State>();
	private int numStates;
	
	Planner(Domain domain, Problem problem) {
		agenda.add(new State(problem));
	}
	
	State findShortestPlanBFS() {
		numStates = 1;
		
		while( !agenda.isEmpty() && !agenda.peek().isGoalState() ) {
			State state = agenda.remove();
			
			List<ActionInstance> instances = state.getApplicableActionInstances();
			for( ActionInstance inst : instances ) {
				State newState = new State(state, inst);
				agenda.add(newState);
				numStates++;
			}
		}
		
		if( !agenda.isEmpty() && agenda.peek().isGoalState() ) {
			return agenda.remove();
		}
		
		return null;
	}
	
	State findShortestPlanSpud() {
		SpudComparator comp = new SpudComparator();
		numStates = 1;
		
		while( !agenda.isEmpty() && !agenda.peek().isGoalState() ) {
			State state = agenda.remove();
			
			List<ActionInstance> instances = state.getApplicableActionInstances();
			State greedyChoice = null;
			
			for( ActionInstance inst : instances ) {
				State newState = new State(state, inst);
				
				if( (greedyChoice == null) || (comp.compare(newState, greedyChoice) < 0)) {
					greedyChoice = newState;
				}
				
				numStates++;
			}
			
			if( greedyChoice == null ) {
				System.err.println("Greedy search failed, last partial plan was:");
				printPlan(state.getPlan());
				return null;
			}
			agenda.add(greedyChoice);
		}
		
		if( !agenda.isEmpty() && agenda.peek().isGoalState() ) {
			return agenda.remove();
		}
		
		return null;
	}

	public static void main(String[] args) throws Exception {
        Domain domain = new Domain();
        Problem problem = new Problem();
        
        PddlParser.parse(new File(args[0]), domain, new File(args[1]), problem);
        
        long start_time = System.currentTimeMillis();
        Planner p = new Planner(domain, problem);
        State goalState = p.findShortestPlanSpud();
        long end_time = System.currentTimeMillis();
        
        if( goalState == null ) {
        	System.err.println("Couldn't find a plan!");
        } else {
        	List<ActionInstance> plan = goalState.getPlan();
        	printPlan(plan);
        }
        
        System.err.println("Total runtime: " + (end_time - start_time) + "ms");
        System.err.println("Computed " + p.numStates + " states");
	}
	
	private static void printPlan(List<ActionInstance> plan) {

    	
    	for( int i = 0; i < plan.size(); i++ ) {
    		System.out.println("" + i + ": " + plan.get(i));
    	}
	}
}
