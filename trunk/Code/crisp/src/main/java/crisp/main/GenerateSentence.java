package crisp.main;

import crisp.converter.CurrentNextCrispConverter;
import crisp.planner.external.FfPlannerInterface;
import crisp.planner.external.PlannerInterface;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.result.CrispDerivationTreeBuilder;
import crisp.result.DerivationTreeBuilder;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;
import de.saar.penguin.tag.grammar.CrispGrammar;
import de.saar.penguin.tag.grammar.SituatedCrispXmlInputCodec;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class GenerateSentence {

    public static void usage() {
        System.out.println("crisp.main.GenerateSentence [grammar file] [problem file]");
    }

    public static void main(String[] args) throws Exception {
        Domain domain = new Domain();
        Problem problem = new Problem();
        CrispGrammar grammar = new CrispGrammar();
        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        CurrentNextCrispConverter converter = new CurrentNextCrispConverter();
        ;

        if (args.length != 2) {
            System.err.println("Wrong number of arguments.");
            usage();
            System.exit(1);
        }

        // read grammar and problem and convert them into planning problem
        long start = System.currentTimeMillis();
        codec.parse(new FileReader(new File(args[0])), grammar);
        converter.convert(grammar, new FileReader(new File(args[1])), domain, problem);
        long end = System.currentTimeMillis();

        // run planner
        PlannerInterface planner = new FfPlannerInterface();
        List<Term> plan = planner.runPlanner(domain, problem);

        // print runtime statistics
        System.out.println("\n\nRuntime:");
        System.out.println("  conversion:        " + (end - start) + "ms\n");
        System.out.println("  preproc:           " + planner.getPreprocessingTime() + " ms");
        System.out.println("  search:            " + planner.getSearchTime() + " ms");
        System.out.println("  total planning:    " + planner.getTotalTime() + " ms\n\n");

//        System.out.println(plan + "\n\n\n");



        // decode plan into sentence
        if (plan == null) {
            System.out.println("No plan found.");
        } else {
            DerivationTreeBuilder planDecoder = new CrispDerivationTreeBuilder(grammar);
            DerivationTree derivationTree = planDecoder.buildDerivationTreeFromPlan(plan, domain);

            DerivedTree<Term> derivedTree = derivationTree.computeDerivedTree(grammar);
            String sent = derivedTree.yield();

            System.out.println("Sentence: " + sent);
        }



        /*

        Properties crispProps = new Properties();
        crispProps.load(new FileReader(new File(Generate.PROPERTIES_FILE)));

        if (args.length != 2) {
        System.err.println("Wrong number of arguments.");
        usage();
        System.exit(1);
        }

        Domain domain = new Domain();
        Problem problem = new Problem();

        /// read CRISP problem specification and convert it to PDDL domain/problem
        long start = System.currentTimeMillis();

        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        CrispGrammar grammar = new CrispGrammar();
        try {
        codec.parse(new FileReader(new File(args[0])), grammar);
        } catch (ParserException ex) {
        System.err.println("Could not parse grammar File. Exiting.");
        System.exit(1);
        } catch (IOException ex) {
        System.err.println("Could not read grammar File " + args[0] + ". Exiting.");
        System.exit(1);
        }
        System.out.println("Grammar parsed in " + (System.currentTimeMillis() - start) + "ms .");

        File problemfile = new File(args[1]);
        try {
        new CurrentNextCrispConverter().convert(grammar, problemfile, domain, problem);
        } catch (ParserConfigurationException ex) {
        System.err.println("Error configuring the XML parser. Exiting.");
        System.exit(1);
        } catch (IOException ex) {
        System.err.println("Could not read problem File " + args[1] + ". Exiting.");
        }

        long end = System.currentTimeMillis();


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




        if (plan == null) {
        System.err.println("Planner returned empty plan. No solution found.");
        System.exit(0);
        }
         *
         */
    }
}
