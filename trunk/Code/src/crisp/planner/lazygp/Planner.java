package crisp.planner.lazygp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import crisp.pddl.PddlParser;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;

public class Planner {
	private static final boolean DEBUG = false;
	private static final int PLANSIZE_LIMIT = 100; // max. number of steps in the plan graph
	private static final int MIN_PLAN_DEPTH = 0;  // min. number of steps in the plan graph
	private static final int RUNS = 1;
	private static final boolean WAIT_FOR_SHARK = false;
	private static final boolean SHOW_SEARCH_TRACE = false;
	private static final boolean SHOW_TIMES_PER_DEPTH = false;

	private final Domain domain;
	private final Problem problem;

	private final List<State> state;
	private final List<ActionLayer> actionlayer;

	private final List<Set<Set<Integer>>> nogoods;

	private long failures, calls, failuresNogood;
	private int minUnsatisfiedGoals;

	public Planner(Domain domain, Problem problem) {
		this.domain = domain;
		this.problem = problem;

		state = new ArrayList<State>();
		actionlayer = new ArrayList<ActionLayer>();
		nogoods = new ArrayList<Set<Set<Integer>>>();

		// set initial state
		state.add(new State(problem));
		nogoods.add(new HashSet<Set<Integer>>());
	}

	private State expandGraphOneStep() {
		State oldState = getFinalState();
		int step = getPlanGraphSize();

		ActionLayer layer = oldState.getNextActionLayer();
		State newState = layer.getNextState();

		state.add(newState);
		actionlayer.add(layer);
		nogoods.add(new HashSet<Set<Integer>>());


		if( DEBUG ) {
			System.out.println("\nAction layer in step " + step + ":");
			System.out.println(layer.toString());

			System.out.println("\nState after step " + step + ":");
			System.out.println(newState.toString());
		}

		return newState;
	}

	public boolean computeGraph() {
		if( DEBUG ) {
			System.out.println("Initial state:");
			System.out.println(state.get(0));
		}

		System.err.print("\nBuilding initial planning graph: ");

		while( !getFinalState().isGoalState() || getPlanGraphSize() < MIN_PLAN_DEPTH ) {
			System.err.print(".");
			expandGraphOneStep();

			// TODO: recognize if we're not making further progress

			if( getPlanGraphSize() >= PLANSIZE_LIMIT ) {
				break;
			}
		}

		System.err.println(" done.");


		if( DEBUG && getFinalState().isGoalState() ) {
			System.out.println("This is a goal state.");
		}

		return getFinalState().isGoalState();
	}

	public List<Plan> backwardsSearch() {
		List<Plan> plans = new ArrayList<Plan>();
		boolean foundPlan = false;

		while( !foundPlan ) {
			failures = 0;
			calls = 0;
			failuresNogood = 0;
			minUnsatisfiedGoals = Integer.MAX_VALUE;

			if( SHOW_TIMES_PER_DEPTH ) {
				System.err.print("\nSearch at depth " + getPlanGraphSize() + ": ");
			}

			long start = System.currentTimeMillis();
			foundPlan = doBackwardsSearch(getFinalState(), getFinalState().getGoalLiterals(), new Plan(), plans);
			long didSearch = System.currentTimeMillis();

			if( SHOW_TIMES_PER_DEPTH ) {
				System.err.println("" + calls + " calls, " + failures + " failures (" + Math.round(100.0*failures/calls) + "%; "
						+ failuresNogood + " due to nogood), "
						+ (didSearch-start) + "ms.");
				printNogoodProfile(getFinalState().getStep());
			}

			if( !foundPlan ) {
				// expand plan
				// TODO recognize when we're not making any progress

				if( getPlanGraphSize() > PLANSIZE_LIMIT ) {
					break;
				}


				expandGraphOneStep();
				long didExpansion = System.currentTimeMillis();

				if( SHOW_TIMES_PER_DEPTH ) {
					System.err.println(" - expand graph, expansion took " + (didExpansion-didSearch) + "ms");
				}

				//System.err.println(getFinalState());

			}
		}

		return plans;
	}

	private void printNogoodProfile(int step) {
		System.err.print("Nogood profile: ");

		for( int i = 0; i <= step; i++ ) {
			System.err.print(nogoods.get(i).size() + " ");
		}

		System.err.println();
	}

	private int countNontrivialOpenGoals(Set<Integer> goals, State state) {
		int ret = 0;

		for( Integer g : goals ) {
			if( !state.isTrivialGoal(g) ) {
				ret++;
			}
		}

		return ret;
	}

	// returns true iff found plan
	private boolean doBackwardsSearch(State state, Set<Integer> goals, Plan partialPlan, List<Plan> plans) {
		if( SHOW_SEARCH_TRACE ) {
			System.err.println("\n\n" + partialPlan);
			System.err.println("goals: " + state.decodeLiterals(goals, new HashSet<Integer>()));
		}


		//System.err.println("state: " + state);

		boolean finished = true;
		int len = 2 * getFinalState().getTable().size();

		calls++;
		minUnsatisfiedGoals = Math.min(minUnsatisfiedGoals, countNontrivialOpenGoals(goals, state));

		if( nogoods.get(state.getStep()).contains(goals) ) {
			// System.err.println("   -> goals known to be no-good for this step");
			failuresNogood++;
			return false;
		}

		for( Integer goal : goals ) {
			if( !state.isTrivialGoal(goal)) {
				finished = false;
			}

			if( state.isUnsatisfiableGoal(goal)) {
				//System.err.println("   -> goal " + state.decodeLiteral(goal) + " is unsatisfiable in this state");
				recordNogood(state, goals);
				return false;
			}
		}


		if( finished ) {
			//System.err.println("\n\n\n\n\n  ***** FOUND A PLAN! *****");
			Plan plan = new Plan(partialPlan);
			Collections.reverse(plan);
			plans.add(plan);

			return true;
		} else if( state.isInitialState() ) {
			//System.err.println("  @@@@ REACHED AN INITIAL STATE, BUT HAVE GOALS LEFT OVER @@@@");

			//System.err.println("  -> fail");
			recordNogood(state, goals);
			return false;
		} else {
			boolean ret = false;
			List<ActionInstance> usefulInstances = new ArrayList<ActionInstance>(state.getUsefulActionInstances(goals, len));
			Map<ActionInstance,Set<Integer>> goalsPerInstance = new HashMap<ActionInstance, Set<Integer>>();
			//System.err.println("useful: " + usefulInstances);

			for( ActionInstance inst : usefulInstances ) {
				Set<Integer> updatedGoals = state.updateGoals(goals, inst);
				int count = 0;
				goalsPerInstance.put(inst, updatedGoals);
				//System.err.println("     action candidate " + inst + " / remaining: " + goalsPerInstance.get(inst).size());
				//System.err.println("      goals: " + state.decodeLiterals(updatedGoals, new HashSet<Integer>()));
			}

			Collections.sort(usefulInstances, new ByRemainingGoalsComparator(goalsPerInstance, state));

			for( ActionInstance inst : usefulInstances ) {
				Set<Integer> newGoals = state.updateGoals(goals, inst);

				partialPlan.add(inst);
				boolean thisCall = doBackwardsSearch(state.getPreviousState(), newGoals, partialPlan, plans);
				partialPlan.remove(partialPlan.size()-1);

				ret = ret || thisCall;

				if( !ret ) {
					failures++;
				}

				// abort after finding first plan
				if( ret ) {
					return true;
				}

				//System.err.println("  -> no useful instances");

			}

			if( !ret ) {
				recordNogood(state, goals);
			}

			return ret;
		}
	}

	private void recordNogood(State state, Set<Integer> goals) {
		nogoods.get(state.getStep()).add(goals);
	}

	public static void main(String[] args) throws Exception {
        Domain domain = new Domain();
        Problem problem = new Problem();

        PddlParser.parse(new File(args[0]), domain, new File(args[1]), problem);

        if( WAIT_FOR_SHARK ) {
        	System.out.print("Press return to start:");
        	System.out.flush();
        	new BufferedReader(new InputStreamReader(System.in)).readLine();
        }


        for( int run = 0; run < RUNS; run++ ) {
        	long start = System.currentTimeMillis();
        	Planner p = new Planner(domain, problem);
        	boolean success = p.computeGraph();
        	long end = System.currentTimeMillis();

        	//System.err.println(p);

        	//System.out.println("Runtime: " + (end-start) + "ms\n");

        	List<Plan> plans = p.backwardsSearch();
        	long end2 = System.currentTimeMillis();
//      	System.err.println("Plans: " + plans);


        	System.out.println("\n\n\nFound " + plans.size() + " plan(s):");
        	for( Plan plan : plans ) {
        		System.out.println("\n" + plan);
        	}

        	System.out.println("\n\nRuntime:");
        	System.out.println("  graph computation: " + (end-start) + " ms");
        	System.out.println("  search:            " + (end2-end) + " ms");
        	System.out.println("  total:             " + (end2-start) + " ms");
        }

        if( WAIT_FOR_SHARK ) {
        	System.out.print("Press return to end:");
        	System.out.flush();
        	new BufferedReader(new InputStreamReader(System.in)).readLine();
        }

	}



	private State getFinalState() {
		return state.get(getPlanGraphSize());
	}

	private int getPlanGraphSize() {
		return actionlayer.size();
	}

	@Override
    public String toString() {
		StringBuilder buf = new StringBuilder();

		for( int i = 0; i < actionlayer.size(); i++ ) {
			buf.append("State " + i);
			if( state.get(i).isGoalState() ) {
				buf.append(" (goal state)");
			}
			buf.append(":\n========================\n" + state.get(i).toString() + "\n");
			buf.append("Action layer " + i + ":\n========================\n" + actionlayer.get(i).toString() + "\n");
		}

		buf.append("State " + actionlayer.size());
		if( state.get(actionlayer.size()).isGoalState() ) {
			buf.append(" (goal state)");
		}
		buf.append(":\n========================\n" + state.get(actionlayer.size()).toString() + "\n");

		return buf.toString();
	}
}
