package crisp.preprocessing;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import crisp.pddl.PddlParser;
import crisp.planner.lazygp.ActionLayer;
import crisp.planner.lazygp.State;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;

public class GraphplanPreprocessingExperiment {
    private static final boolean DEBUG = false;
    private static final boolean SHOW_STATES = false;
    private static final int PLANSIZE_LIMIT = 100; // max. number of steps in the plan graph
    private static final int MIN_PLAN_DEPTH = 0;  // min. number of steps in the plan graph
    private static final int RUNS = 1;
    private static final boolean WAIT_FOR_SHARK = false;
    private static final boolean SHOW_SEARCH_TRACE = false;
    private static final boolean SHOW_TIMES_PER_DEPTH = true;
    private static final boolean VERBOSE = false;

    private final Domain domain;
    private final Problem problem;

    private final List<State> state;
    private final List<ActionLayer> actionlayer;

    private final List<Set<Set<Integer>>> nogoods;

    private long failures, calls, failuresNogood;
    private int minUnsatisfiedGoals;

    public static void main(String[] args) throws Exception {
        Domain domain = new Domain();
        Problem problem = new Problem();

        PddlParser.parse(new File(args[0]), domain, new File(args[1]), problem);

        for( int run = 0; run < RUNS; run++ ) {
            long start = System.currentTimeMillis();
            GraphplanPreprocessingExperiment p = new GraphplanPreprocessingExperiment(domain, problem);
            boolean success = p.computeGraph();
            long end = System.currentTimeMillis();

            State finalState = p.getFinalState();

            System.out.println("\n\nRuntime:");
            System.out.println("  graph computation: " + (end-start) + " ms");

            System.out.println("Atoms in final graphplan state:");
            System.out.println(finalState.myToString());
        }
    }


    public GraphplanPreprocessingExperiment(Domain domain, Problem problem) {
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
        }

        if( SHOW_STATES ) {
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

        if( VERBOSE ) {
            System.err.print("\nBuilding initial planning graph: ");
        }

        while( !getFinalState().isGoalState() || getPlanGraphSize() < MIN_PLAN_DEPTH ) {
            if( VERBOSE ) {
                System.err.print(".");
            }
            expandGraphOneStep();

            // TODO: recognize if we're not making further progress

            if( getPlanGraphSize() >= PLANSIZE_LIMIT ) {
                break;
            }
        }

        if( VERBOSE ) {
            System.err.println(" done.");
        }


        if( DEBUG && getFinalState().isGoalState() ) {
            System.out.println("This is a goal state.");
        }

        return getFinalState().isGoalState();
    }




    private State getFinalState() {
        return state.get(getPlanGraphSize());
    }

    private int getPlanGraphSize() {
        return actionlayer.size();
    }
}
