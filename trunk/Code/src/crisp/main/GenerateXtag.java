package crisp.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
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
import de.saar.penguin.tag.grammar.LexiconEntry;
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
			doIt(f(args[1]), crispGrammar);
		} else {
			// interactive mode

			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String line;

			do {
				System.out.print("> ");
				line = in.readLine();

				if( line != null ) {
					if( line.startsWith("@")) {
						doIt(f(line.substring(1)), crispGrammar);
					} else if( line.startsWith("?")) {
						if( crispGrammar.getLexiconEntries(line.substring(1)) != null ) {
							for( LexiconEntry entry : crispGrammar.getLexiconEntries(line.substring(1))) {
								System.out.println(entry.tree + "/" + entry.getInventedPredicate() + ": " + crispGrammar.getTree(entry.tree).getSignatureString());
							}
						}
					} else {
						doIt(createCrispProblemReader(line.split(" ")), crispGrammar);
					}
				}
			} while( line != null );
		}
	}

	private static Reader f(String filename) throws FileNotFoundException {
		return new FileReader(new File(filename));
	}

	private static void doIt(Reader problemfile, Grammar<Term> grammar) throws ParserConfigurationException, SAXException, IOException {
		Domain domain = new Domain();
		Problem problem = new Problem();
		//File problemfile = new File(filename);

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

	private static Reader createCrispProblemReader(String[] words) {
		StringBuffer buf = new StringBuffer();
		int ignore = 0;
		int plansize = 10;

		try {
			if( Integer.parseInt(words[words.length-1]) != 0 ) {
				ignore = 1;
				plansize = Integer.parseInt(words[words.length-1]);
			}
		} catch(NumberFormatException e) {

		}

		buf.append("<crispproblem name='xtag' grammar='' cat='S' index='e' plansize='" + plansize + "'>");

		for( int i = 0; i < words.length-ignore; i++ ) {
			buf.append("<world>" + words[i] + "</world>");
			buf.append("<commgoal>" + words[i] + "</commgoal>");
		}

		buf.append("</crispproblem>");

		return new StringReader(buf.toString());
	}


}