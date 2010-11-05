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
import java.io.Reader;
import java.util.List;

public class GenerateSentence {
    public static final String PROPERTIES_FILE = "crisp.properties";

    public static void usage() {
        System.out.println("crisp.main.GenerateSentence [grammar file] [problem file]");
    }

    public static void main(String[] args) throws Exception {
        Domain domain = new Domain();
        Problem problem = new Problem();
        CrispGrammar grammar = new CrispGrammar();
        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        CurrentNextCrispConverter converter = new CurrentNextCrispConverter();

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
        System.out.println("  preprocessing:     " + planner.getPreprocessingTime() + " ms");
        System.out.println("  search:            " + planner.getSearchTime() + " ms");
        System.out.println("  total planning:    " + planner.getTotalTime() + " ms\n\n");

        System.out.println("Plan: " + plan + "\n\n");



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
    }

    public static String generateSentence(Reader grammarReader, Reader problemReader) throws Exception {
        Domain domain = new Domain();
        Problem problem = new Problem();
        CrispGrammar grammar = new CrispGrammar();
        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        CurrentNextCrispConverter converter = new CurrentNextCrispConverter();

        // read grammar and problem and convert them into planning problem
        codec.parse(grammarReader, grammar);
        converter.convert(grammar, problemReader, domain, problem);

        // run planner
        PlannerInterface planner = new FfPlannerInterface();
        List<Term> plan = planner.runPlanner(domain, problem);

        // decode plan into sentence
        if (plan == null) {
            return null;
        } else {
            DerivationTreeBuilder planDecoder = new CrispDerivationTreeBuilder(grammar);
            DerivationTree derivationTree = planDecoder.buildDerivationTreeFromPlan(plan, domain);

            DerivedTree<Term> derivedTree = derivationTree.computeDerivedTree(grammar);
            String sent = derivedTree.yield();

            return sent;
        }
    }
}
