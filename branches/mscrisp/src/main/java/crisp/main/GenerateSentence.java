package crisp.main;

import crisp.converter.CrispConverter;
import crisp.converter.CurrentNextConverterWithCosts;
import crisp.converter.CurrentNextCrispConverter;
import crisp.converter.FastCRISPConverter;
import crisp.planner.external.FfPlannerInterface;
import crisp.planner.external.LazyFfInterface;
import crisp.planner.external.MetricFfPlannerInterface;
import crisp.planner.external.PlannerInterface;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.CostPddlOutputCodec;
import crisp.planningproblem.codec.FluentsPddlOutputCodec;
import crisp.planningproblem.codec.PddlOutputCodec;
import crisp.result.CrispDerivationTreeBuilder;
import crisp.result.DerivationTreeBuilder;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.codec.CrispXmlInputCodec;
import de.saar.penguin.tag.codec.InputCodec;
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
        System.out.println("Usage: crisp.main.GenerateSentence [grammar file] [problem file] [input codec] [converter] [output codec] [planner] [decoder]");
        System.out.println("Input codec options: crispInput, situatedCrispInput");
        System.out.println("Converter options: fastConverter, currentNextConverter, currentNextConverterWithCosts");
        System.out.println("Output codec options: pddlOutput, costPddlOutput, fluentsPddlOutput");
        System.out.println("Planner options: ffPlanner, metricFfPlanner, lazyFFPlanner");
        System.out.println("Decoder options: crispDecoder\n");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 7) {
            System.err.println("Wrong number of arguments.\n");
            usage();
            System.exit(1);
        }

        Domain domain = new Domain();
        Problem problem = new Problem();
        CrispGrammar grammar = new CrispGrammar();
        
        // read command line arguments
        InputCodec inputCodec = null;        
        if (args[2].equalsIgnoreCase("crispInput")) {
            inputCodec = new CrispXmlInputCodec();
        } else if (args[2].equalsIgnoreCase("situatedCrispInput")) {	
            inputCodec = new SituatedCrispXmlInputCodec();
        } else {
            System.err.println("Wrong input codec.\n");
            usage();
            System.exit(1);
        }
        
        CrispConverter converter = null;
        if (args[3].equalsIgnoreCase("fastConverter")) {
            converter = new FastCRISPConverter();
        } else if (args[3].equalsIgnoreCase("currentNextConverter")) {
            converter = new CurrentNextCrispConverter();
        } else if (args[3].equalsIgnoreCase("currentNextConverterWithCosts")) {
            converter = new CurrentNextConverterWithCosts();
        } else {
            System.err.println("Wrong converter.\n");
            usage();
            System.exit(1);
        }
        
        PddlOutputCodec outputCodec = null;
        if (args[4].equalsIgnoreCase("pddlOutput")) {
            outputCodec = new PddlOutputCodec();
        } else if (args[4].equalsIgnoreCase("costPddlOutput")) {
            outputCodec = new CostPddlOutputCodec();
        } else if (args[4].equalsIgnoreCase("fluentsPddlOutput")) {
            outputCodec = new FluentsPddlOutputCodec();
        } else {
            System.err.println("Wrong output codec.\n");
            usage();
            System.exit(1);            
        }
        
        PlannerInterface planner = null;
        if (args[5].equalsIgnoreCase("ffPlanner")) {
            planner = new FfPlannerInterface("-B -T -H");
        } else if (args[5].equalsIgnoreCase("metricFFPlanner")) {
            planner = new MetricFfPlannerInterface("-B");	// available flags: "-B" "-E" "-T" "-H"; also "-g 5 -h 1" or similar to switch to FF.quality 
        } else if (args[5].equalsIgnoreCase("lazyFFPlanner")) {
            planner = new LazyFfInterface();
        } else {
            System.err.println("Wrong planner.\n");
            usage();
            System.exit(1);            
        }
        
        DerivationTreeBuilder planDecoder = null;
        if (args[6].equalsIgnoreCase("crispDecoder")) {
            planDecoder = new CrispDerivationTreeBuilder(grammar);
        } else {
            System.err.println("Wrong decoder.\n");
            usage();
            System.exit(1);            
        }

        // read grammar and problem and convert them into planning problem
        long start = System.currentTimeMillis();
        inputCodec.parse(new FileReader(new File(args[0])), grammar);
        converter.convert(grammar, new FileReader(new File(args[1])), domain, problem);
        long end = System.currentTimeMillis();

        // run planner
        List<Term> plan = planner.runPlanner(domain, problem, outputCodec);

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
            DerivationTree derivationTree = planDecoder.buildDerivationTreeFromPlan(plan, domain);

            DerivedTree<Term> derivedTree = derivationTree.computeDerivedTree(grammar);
            String sent = derivedTree.yield();

            System.out.println("Sentence: " + sent);
        }
    }

    @Deprecated
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
        List<Term> plan = planner.runPlanner(domain, problem, new PddlOutputCodec());

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
