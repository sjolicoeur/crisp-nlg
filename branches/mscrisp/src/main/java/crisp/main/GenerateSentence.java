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
import crisp.profile.CrispProfile;
import crisp.profile.MscrispProfile;
import crisp.profile.ScrispProfile;
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class GenerateSentence {
    public static final String PROPERTIES_FILE = "crisp.properties";
    public static final Map<String, CrispProfile> stringToProfile;
    static {
	Map<String, CrispProfile> map = new HashMap<String, CrispProfile>();
	map.put("scrisp", new ScrispProfile());
	map.put("mscrisp", new MscrispProfile());
	stringToProfile = Collections.unmodifiableMap(map);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Wrong number of arguments.\n");
            usage();
            System.exit(1);
        }
        generateSentence(args[0], args[1], args[2], true);
    }

    /**
     * Prints planner runtime and output details
     * 
     * @param planner
     * @param plan
     * @param time
     * @param sentence
     */
    private static void printPlannerRuntimeInfo(PlannerInterface planner, List<Term> plan, long time, String sentence) {
        System.out.println("\n\nRuntime:");
        System.out.println("  conversion:        " + time + "ms\n");
        System.out.println("  preprocessing:     " + planner.getPreprocessingTime() + " ms");
        System.out.println("  search:            " + planner.getSearchTime() + " ms");
        System.out.println("  total planning:    " + planner.getTotalTime() + " ms\n\n");
        System.out.println("Plan: " + plan + "\n\n");
        if (plan == null) {
            System.out.println("No plan found.");
        }	
        System.out.println("Sentence: " + sentence);
    }
    
    private static void usage() {
        System.out.println("Usage: crisp.main.GenerateSentence [grammar file] [problem file] [profile name]");
        System.out.println("Available profiles: SCRISP, mSCRISP\n");
    }

    /**
     * @param grammarfile
     * @param problemfile
     * @param profilename
     * @param verbal
     * @return The sentence that is generated from a grammar and a problem file under a given crisp profile. 
     * @throws Exception
     */
    public static String generateSentence(String grammarfile, String problemfile, String profilename, boolean verbal) throws Exception {
        
        // load crisp profile
        CrispProfile profile = null;
        if (stringToProfile.containsKey(profilename.toLowerCase())) {
            profile = stringToProfile.get(profilename.toLowerCase());
        } else {
            usage();
            return null;
        }        
        
        InputCodec inputCodec = profile.getInputCodec();
        CrispConverter converter = profile.getCrispConverter();
        PddlOutputCodec outputCodec = profile.getPddlOutputCodec();
        PlannerInterface planner = profile.getPlannerInterface();
        Domain domain = new Domain();
        Problem problem = new Problem();
        CrispGrammar grammar = new CrispGrammar();     

        // read grammar and problem and convert them into planning problem
        long start = System.currentTimeMillis();
        inputCodec.parse(new FileReader(new File(grammarfile)), grammar);
        converter.convert(grammar, new FileReader(new File(problemfile)), domain, problem);
        long end = System.currentTimeMillis();

        // run planner
        List<Term> plan = planner.runPlanner(domain, problem, outputCodec);

        // decode plan into sentence
        String sentence = null;
        if (plan != null) {
            DerivationTreeBuilder planDecoder = new CrispDerivationTreeBuilder(grammar);   
            DerivationTree derivationTree = planDecoder.buildDerivationTreeFromPlan(plan, domain);
            DerivedTree<Term> derivedTree = derivationTree.computeDerivedTree(grammar);
            sentence = derivedTree.yield();
        }
        
        if (verbal) {
            printPlannerRuntimeInfo(planner, plan, (end - start), sentence);
        }
        
        return sentence;
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
