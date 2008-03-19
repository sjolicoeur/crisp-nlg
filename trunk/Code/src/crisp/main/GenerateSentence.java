package crisp.main;

import java.util.List;

import crisp.converter.CRISPtoPDDL;
import crisp.planner.lazygp.ActionInstance;
import crisp.planner.lazygp.Plan;
import crisp.planner.lazygp.Planner;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.result.DerivationTree;
import crisp.result.Grammar;
import crisp.result.TreeNode;
import de.saar.chorus.term.Substitution;

/**
 * This is Mark Wilding's version of Generate. This alternative front-end
 * extends the original project to allow the final sentence to be 
 * output, rather than just a plan. This is done using classes in 
 * the package crisp.sentence.
 * 
 * @author Mark Wilding
 *
 */
public class GenerateSentence {
	public static void main(String[] args) throws Exception {
		/*
		 * First we do the same as Generate, calling the planner to
		 * plan our sentence.
		 */
		Domain domain = new Domain();
		Problem problem = new Problem();
		String problemFile = args[0];

		/*** read CRISP problem specification and convert it to PDDL domain/problem ***/
		long start = System.currentTimeMillis();
		CRISPtoPDDL.convert(problemFile, domain, problem);
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

    	/*
    	 * Here we use crisp.result to produce the final 
    	 * sentence and the derivation tree.
    	 */
    	Grammar grammar = new Grammar(problemFile);

    	System.out.println("\n\n\nFound " + plans.size() + " plan(s):");
    	for( Plan plan : plans ) {
    		System.out.println(plan);

    		// Build the derivation tree
    		DerivationTree derivation = new DerivationTree("S", plan, grammar);
    		System.out.println("\nBuilt derivation tree:\n"+derivation);
    		
    		// Derive the TAG tree from the derivation tree
    		TreeNode derived = derivation.buildDerivedTree();
    		System.out.println("\nBuilt derived tree:\n"+derived);
    		
    		// Output the sentence
    		System.out.println("\nFinal sentence:");
    		System.out.println(derived.getSentenceString());
    	}

    	System.err.println("\nRuntime:");
		System.err.println("  conversion:        " + (end-start) + "ms\n");
    	System.out.println("  graph computation: " + (endPlanner-startPlanner) + " ms");
    	System.out.println("  search:            " + (endPlanner2-endPlanner) + " ms");
    	System.out.println("  total planning:    " + (endPlanner2-startPlanner) + " ms");
	}

}
