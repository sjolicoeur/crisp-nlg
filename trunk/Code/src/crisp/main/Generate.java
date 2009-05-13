package crisp.main;

import java.util.List;

import crisp.converter.FastCRISPConverter;
import crisp.planner.lazygp.Plan;
import crisp.planner.lazygp.Planner;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PddlOutputCodec;

import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.codec.CrispXmlInputCodec;

import de.saar.chorus.term.Term; 

import java.io.File;
import java.io.FileReader;


public class Generate {
    private static final int PLANNER_ITERATIONS = 1;

	public static void main(String[] args) throws Exception {
		Domain domain = new Domain();
		Problem problem = new Problem();

		/*** read CRISP problem specification and convert it to PDDL domain/problem ***/
		long start = System.currentTimeMillis();		
                
        CrispXmlInputCodec codec = new CrispXmlInputCodec();
		Grammar<Term> grammar = new Grammar<Term>();	
		codec.parse(new FileReader(new File(args[0])), grammar);         
 
        File problemfile = new File(args[1]);
        
		FastCRISPConverter.convert(grammar, problemfile, domain, problem);
        
		long end = System.currentTimeMillis();

		new PddlOutputCodec().writeToDisk(domain, problem, "./", domain.getName());



		/*** run the planner ***/

		problem.addEqualityLiterals();

		for( int i = 0; i < PLANNER_ITERATIONS; i++ ) {
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

}
