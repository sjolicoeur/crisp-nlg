package crisp.main;

import crisp.converter.CurrentNextCrispConverter;
import crisp.planner.external.FfPlannerInterface;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.codec.ParserException;
import de.saar.penguin.tag.grammar.CrispGrammar;
import de.saar.penguin.tag.grammar.SituatedCrispXmlInputCodec;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;


public class Generate {

    public static final String PROPERTIES_FILE = "crisp.properties";

    public static List<Term> computePlan(String grammarFile, String problemFile) {
        Domain domain = new Domain();
        Problem problem = new Problem();

        /// read CRISP problem specification and convert it to PDDL domain/problem
        long start = System.currentTimeMillis();

        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        CrispGrammar grammar = new CrispGrammar();
        try {
            codec.parse(new FileReader(new File(grammarFile)), grammar);
        } catch (ParserException ex) {
            System.err.println("Could not parse grammar File. Exiting.");
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Could not read grammar File " + grammarFile + ". Exiting.");
            System.exit(1);
        }
        System.out.println("Grammar parsed in " + (System.currentTimeMillis() - start) + "ms .");

        File problemfile = new File(problemFile);
        try {
            new CurrentNextCrispConverter().convert(grammar, problemfile, domain, problem);
        } catch (ParserConfigurationException ex) {
            System.err.println("Error configuring the XML parser. Exiting.");
            System.exit(1);
        } catch (SAXException ex) {
            System.err.println("Could not parse problem File. " + problemFile + ". Exiting.");
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Could not read problem File " + problemFile + ". Exiting.");
        }

        long end = System.currentTimeMillis();

        /*** run the planner ***/
        //problem.addEqualityLiterals();
        FfPlannerInterface planner = new FfPlannerInterface();
        List<Term> plan = null;
        try {
            plan = planner.runPlanner(domain, problem);
        } catch (Exception ex) {
            System.err.println("Could not run the planner: " + ex);
            System.exit(1);
        }

        System.err.println("\n\nRuntime:");
        System.err.println("  conversion:        " + (end - start) + "ms\n");
        System.out.println("  preproc: " + planner.getPreprocessingTime() + " ms");
        System.out.println("  search:            " + planner.getSearchTime() + " ms");
        System.out.println("  total planning:    " + planner.getTotalTime() + " ms");

        return plan;
    }

    public static void usage() {
        System.out.println("crisp.main.Generate [grammar file] [problem file]");
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.println("Wrong number of arguments.");
            usage();
        }

        List<Term> plan = computePlan(args[0], args[1]);
        if (plan == null) {
            System.err.println("Planner returned empty plan. No solution found.");
            System.exit(0);
        }

        System.out.println("Found plan:");
        for (Term step : plan) {
            System.out.println(step);
        }
    }
}
