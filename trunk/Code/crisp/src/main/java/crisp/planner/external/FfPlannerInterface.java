package crisp.planner.external;


import crisp.evaluation.ffplanparser.ParseException;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;


import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PddlOutputCodec;

import crisp.evaluation.ffplanparser.FfPlanParser;



import de.saar.chorus.term.Term;

import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import javax.swing.JFrame;
//import org.jgraph.JGraph;


public class FfPlannerInterface implements PlannerInterface {
    public static final String TEMPDOMAIN_FILE = "/tmp/tmpdomain.lisp";
    public static final String TEMPPROBLEM_FILE = "/tmp/tmpproblem.lisp";
    public static final String TEMPRESULT_FILE = "/tmp/tmpresult";

    private String ffFlags;

    private long preprocessingTime;
    private long searchTime;
    private String binaryLocation;

    public FfPlannerInterface(String ffFlags) {
        this.ffFlags = ffFlags;
        preprocessingTime = 0;
        searchTime = 0;

        try {
            Properties crispProps = new Properties();
            crispProps.load(new FileReader(new File(crisp.main.GenerateSentence.PROPERTIES_FILE)));
            binaryLocation = crispProps.getProperty("FfBinary");
        } catch (IOException ex) {
            throw new RuntimeException("An error occurred while trying to read the properties file: " + ex.getMessage());
        }

    }

    public FfPlannerInterface() {
        this("");
    }

    public List<Term> runPlanner(Domain domain, Problem problem) throws Exception {

        long start;
        long end;

        new PddlOutputCodec().writeToDisk(domain, problem, new PrintWriter(new FileWriter(new File(TEMPDOMAIN_FILE))),
                new PrintWriter(new FileWriter(new File(TEMPPROBLEM_FILE))));


        // Run the FfPlanner
        Process ffplanner = Runtime.getRuntime().exec(binaryLocation + " " + ffFlags + " -o " + TEMPDOMAIN_FILE + " -f " + TEMPPROBLEM_FILE);
        ffplanner.waitFor();
        Reader resultReader = new BufferedReader(new InputStreamReader(ffplanner.getInputStream()));

        StringWriter str = new StringWriter();
        char[] buffer = new char[100];
        while (resultReader.read(buffer) != -1) {
            str.write(buffer);
        }

        if (ffplanner.exitValue() != 0) {
            throw new RuntimeException("FF in " + binaryLocation + " terminated inappropriately. Exit Value was " + ffplanner.exitValue() + ".\n" + str.toString());
        }

        List<Term> plan = parsePlan(str.toString());
        return plan;

    }


    private List<Term> parsePlan(String string) {
        Pattern p = Pattern.compile(".*found legal plan as follows(.*)time spent.*?(\\S+) seconds instantiating.*?(\\S+) seconds searching.*", Pattern.DOTALL);
        Matcher m = p.matcher(string);

        if (!m.matches()) {
            return null;
        } else {
            try {
                FfPlanParser parser = new FfPlanParser(new StringReader(m.group(1)));
                List<Term> ret = parser.plan();

                preprocessingTime = (long) (1000*Double.parseDouble(m.group(2)));
                searchTime = (long) (1000*Double.parseDouble(m.group(3)));


                return ret;
            } catch (ParseException ex) {
                ex.printStackTrace();
                return null;
            }
        }

    }


    public List<Term> runPlanner(Domain domain, Problem problem, long timeout) throws Exception {
        //TODO: implement timeout for SGPlan
        return runPlanner(domain, problem);
    }

    public long getPreprocessingTime() {
        return preprocessingTime;
    }

    public long getSearchTime() {
        return searchTime;
    }

    public long getTotalTime() {
        return preprocessingTime + searchTime;
    }


    public static void usage() {
        System.out.println("Usage: java crisp.evaluation.LamaPlannerInterface [CRISP grammar] [CIRISP problem]");
    }


    /*
    public static void main(String[] args) throws Exception {


        if (args.length < 1) {
            System.err.println("No crisp problem specified");
            usage();
            System.exit(1);
        }

        // TODO some exception handling

        Domain domain = new Domain();
        Problem problem = new Problem();

        long start = System.currentTimeMillis();


        System.out.println("Reading grammar...");
        PCrispXmlInputCodec codec = new PCrispXmlInputCodec();
        ProbabilisticGrammar<Term> grammar = new ProbabilisticGrammar<Term>();
        codec.parse(new File(args[0]), grammar);

        File problemfile = new File(args[1]);

        System.out.println("Generating planning problem...");
        //new FastCRISPConverter().convert(grammar, problemfile, domain, problem);
        new ProbCRISPConverter().convert(grammar, problemfile, domain, problem);

        long end = System.currentTimeMillis();

        System.out.println("Total runtime for problem generation: " + (end - start) + "ms");

        //System.out.println("Domain: " + domain );
        //System.out.println("Problem: " + problem);

        System.out.println("Running planner ... ");
        PlannerInterface planner = new FfPlannerInterface();
        List<Term> plan = planner.runPlanner(domain, problem);
        System.out.println(plan);
        DerivationTreeBuilder derivationTreeBuilder = new CrispDerivationTreeBuilder(grammar);
        DerivationTree derivTree = derivationTreeBuilder.buildDerivationTreeFromPlan(plan, domain);
        System.out.println(derivTree);
        DerivedTree derivedTree = derivTree.computeDerivedTree(grammar);
        System.out.println(derivedTree);
        System.out.println(derivedTree.yield());

     }


     * 
     */

}
