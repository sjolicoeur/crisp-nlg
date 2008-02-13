package crisp.main;

import java.util.List;

import crisp.converter.CRISPtoPDDL;
import crisp.planner.lazygp.Plan;
import crisp.planner.lazygp.Planner;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;

public class Generate {
	public static void main(String[] args) throws Exception {
		Domain domain = new Domain();
		Problem problem = new Problem();

		/*** read CRISP problem specification and convert it to PDDL domain/problem ***/
		long start = System.currentTimeMillis();
		CRISPtoPDDL.convert(args[0], domain, problem);
		long end = System.currentTimeMillis();

		CRISPtoPDDL.writeToDisk(domain, problem, "./");



		/*** run the planner ***/

        problem.addEqualityLiterals();

		long startPlanner = System.currentTimeMillis();
    	Planner p = new Planner(domain, problem);
    	boolean success = p.computeGraph();
    	long endPlanner = System.currentTimeMillis();

    	List<Plan> plans = p.backwardsSearch();
    	long endPlanner2 = System.currentTimeMillis();


    	System.out.println("\n\n\nFound " + plans.size() + " plan(s):");
    	for( Plan plan : plans ) {
    		System.out.println("\n" + plan);
    	}

    	System.err.println("\n\nRuntime:");
		System.err.println("  conversion:        " + (end-start) + "ms\n");
    	System.out.println("  graph computation: " + (endPlanner-startPlanner) + " ms");
    	System.out.println("  search:            " + (endPlanner2-endPlanner) + " ms");
    	System.out.println("  total planning:    " + (endPlanner2-startPlanner) + " ms");
	}

}
