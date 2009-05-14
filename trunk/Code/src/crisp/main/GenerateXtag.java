package crisp.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import crisp.converter.FastCRISPConverter;
import crisp.planner.lazygp.Plan;
import crisp.planner.lazygp.Planner;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PddlOutputCodec;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.codec.xtag.XtagInputCodec;
import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.grammar.converter.XtagToCrispConverter;

public class GenerateXtag {
	private static final int PLANNER_ITERATIONS = 1;

	public static void main(String[] args) throws Exception {
		System.err.println("Reading XTAG grammar ...");
		File xtagPath = new File(args[0]);
		Grammar<String> xtagGrammar = new Grammar<String>();
		new XtagInputCodec().parse(xtagPath, xtagGrammar);

		System.err.println("Converting into CRISP format ...");
		Grammar<Term> crispGrammar = new XtagToCrispConverter().convertXtagToCrisp(xtagGrammar);

		if( args.length > 1 ) {
			doIt(args[1], crispGrammar);
		} else {
			// interactive mode

			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String line;

			do {
				System.out.print("> ");
				line = in.readLine();

				if( line != null ) {
					doIt(line, crispGrammar);
				}
			} while( line != null );
		}
	}

	private static void doIt(String filename, Grammar<Term> grammar) throws ParserConfigurationException, SAXException, IOException {
		Domain domain = new Domain();
		Problem problem = new Problem();
		File problemfile = new File(filename);

		long start = System.currentTimeMillis();		
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