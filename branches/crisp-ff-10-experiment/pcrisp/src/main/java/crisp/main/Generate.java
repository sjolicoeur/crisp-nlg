package crisp.main;

import java.util.List;

import crisp.converter.FastCRISPConverter;
import crisp.evaluation.LazyFfInterface;
import crisp.planner.lazyff.Planner;
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
        System.out.println("Grammar parsed in "+ (System.currentTimeMillis()-start) + "ms .");
        
        File problemfile = new File(args[1]);
        
		new FastCRISPConverter().convert(grammar, problemfile, domain, problem);
        
		long end = System.currentTimeMillis();

		new PddlOutputCodec().writeToDisk(domain, problem, "./", domain.getName());



		/*** run the planner ***/

		problem.addEqualityLiterals();

                LazyFfInterface planner = new LazyFfInterface();
                List<Term> plan = planner.runPlanner(domain, problem);

		
		for( Term step : plan ) {
		        System.out.println(step);
		}

		    System.err.println("\n\nRuntime:");
		    System.err.println("  conversion:        " + (end-start) + "ms\n");
		    System.out.println("  preproc: " + planner.getPreprocessingTime() + " ms");
		    System.out.println("  search:            " + planner.getSearchTime() + " ms");
		    System.out.println("  total planning:    " + planner.getTotalTime() + " ms");
		}
	

}
