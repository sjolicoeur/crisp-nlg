package crisp.main;

import java.util.List;

import crisp.converter.FastCRISPConverter;
import crisp.planner.lazygp.Plan;
import crisp.planner.lazygp.Planner;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PddlOutputCodec;
import crisp.result.CrispDerivationTreeBuilder;
import crisp.result.DerivationTreeBuilder;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.codec.CrispXmlInputCodec;
import de.saar.penguin.tag.derivation.DerivedTree;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.grammar.Grammar;
import java.io.File;
import java.io.FileReader;

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

                long start = System.currentTimeMillis();
                CrispXmlInputCodec codec = new CrispXmlInputCodec();
		Grammar<Term> grammar = new Grammar<Term>();
		codec.parse(new FileReader(new File(args[0])), grammar);
        System.out.println("Grammar parsed in "+ (System.currentTimeMillis()-start) + "ms .");

            File problemFile = new File(args[1]);

		/*** read CRISP problem specification and convert it to PDDL domain/problem ***/
		start = System.currentTimeMillis();
		new FastCRISPConverter().convert(grammar, problemFile, domain, problem);
		long end = System.currentTimeMillis();

		new PddlOutputCodec().writeToDisk(domain, problem, "./", domain.getName());

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

    	System.out.println("\n\n\nFound " + plans.size() + " plan(s):");
    	for( Plan plan : plans ) {
    		System.out.println(plan);

                DerivationTreeBuilder derivationTreeBuilder = new CrispDerivationTreeBuilder(grammar);
            //DerivationTree derivTree = derivationTreeBuilder.buildDerivationTreeFromPlan(plan, domain);
            //System.out.println(derivTree);
            //    		System.out.println("\nBuilt derivation tree:\n"+derivTree);
            //DerivedTree derivedTree = derivTree.computeDerivedTree(grammar);
            //System.out.println("\nBuilt derived tree:\n"+derivedTree);
            //System.out.println("\nFinal sentence:");
            //System.out.println(derivedTree.yield());

    		// Build the derivation tree
    	}

    	System.err.println("\nRuntime:");
		System.err.println("  conversion:        " + (end-start) + "ms\n");
    	System.out.println("  graph computation: " + (endPlanner-startPlanner) + " ms");
    	System.out.println("  search:            " + (endPlanner2-endPlanner) + " ms");
    	System.out.println("  total planning:    " + (endPlanner2-startPlanner) + " ms");
	}

}
