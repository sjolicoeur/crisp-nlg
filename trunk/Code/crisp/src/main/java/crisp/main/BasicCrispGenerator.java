package crisp.main;

import crisp.converter.CurrentNextCrispConverter;
import crisp.planner.external.FfPlannerInterface;
import crisp.planner.external.PlannerInterface;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.result.CrispDerivationTreeBuilder;
import crisp.result.DerivationTreeBuilder;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.codec.ParserException;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;
import de.saar.penguin.tag.grammar.CrispGrammar;
import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.grammar.SituatedCrispXmlInputCodec;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple implementation of the @{CRISPGeneratorInterface} using the current state
 * of the standard CRISP generation system (with "steps" andwithout pragmatics or
 * context or probabilistic grammars). It also creates a new PDDL domain for every
 * problem instance and thus does not benefit from Konstantinas optimizations.
 * <p/>
 * Use this class only through the @{CRISPGeneratorInterface} as it is likely to be modified
 * very soon.
 *
 * @author dbauer
 */
public class BasicCrispGenerator implements CrispGeneratorInterface {
    CrispGrammar grammar = null;
    CurrentNextCrispConverter converter;
    PlannerInterface planner;
    DerivationTreeBuilder planDecoder = null;
    private List<Term> plan = new ArrayList<Term>();
    private Domain domain;
    private Problem problem;

    public BasicCrispGenerator(String ffFlags) {
        converter = new CurrentNextCrispConverter();
        planner = new FfPlannerInterface(ffFlags);
    }

    public BasicCrispGenerator() {
        this("");
    }

    // *** Methods to load the grammar ***

    public void setGrammar(Grammar grammar) {
        this.grammar = (CrispGrammar) grammar;
        planDecoder = new CrispDerivationTreeBuilder(grammar);
    }

    private void setGrammar(CrispGrammar grammar) {
        this.grammar = grammar;
        planDecoder = new CrispDerivationTreeBuilder(grammar);
    }

    public void setGrammar(Reader xmlGrammarReader) throws IOException, ParserException {
        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        CrispGrammar newGrammar = new CrispGrammar();
        codec.parse(xmlGrammarReader, newGrammar);
        setGrammar(newGrammar);
    }

    public void setGrammar(String xmlGrammar) throws ParserException {
        try {
            setGrammar(new StringReader(xmlGrammar));
        } catch (IOException e) { // Should never happen
            System.err.println("Could not create String Reader from XML String.");
            System.exit(1);
        }
    }

    public String generateSentence(Reader xmlProblemReader) throws CrispGeneratorException {
        DerivationTree derivationTree = generate(xmlProblemReader);
        DerivedTree derivedTree = derivationTree.computeDerivedTree(grammar);

        String ret = derivedTree.yield();
        return ret;
    }

    public String generateSentence(String xmlProblem) throws CrispGeneratorException {
        return generateSentence(new StringReader(xmlProblem));
    }

    public List<String> generateWordSequence(Reader xmlProblemReader) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<String> generateWordSequence(String xmlProblem) throws CrispGeneratorException {
        throw new UnsupportedOperationException("Not supported yet.");

    }

    public List<Term> getCrispPlan(Reader xmlProblemReader) throws CrispGeneratorException {
        if (plan.size() == 0) {
            domain = new Domain();
            problem = new Problem();

            // Check if grammar is loaded
            if (grammar == null) {
                throw new CrispGeneratorException("No grammar set.");
            }

            // Convert CRISP Problem to PDDL
            try {
                converter.convert(grammar, xmlProblemReader, domain, problem);
            } catch (Exception e) {
                throw new CrispGeneratorException("Error parsing problem XML specification: " + e);
            }

            // run the planner
            try {
                plan = planner.runPlanner(domain, problem);
            } catch (Exception e) {
//                e.printStackTrace();
                throw new CrispGeneratorException("Error running the planner.", e);
            }
        }

        return plan;
    }

    public List<Term> getCrispPlan(String xmlProblem) throws CrispGeneratorException {
        Reader xmlProblemReader = new StringReader(xmlProblem);

        if (plan.size() == 0) {
            domain = new Domain();
            problem = new Problem();

            // Check if grammar is loaded
            if (grammar == null) {
                throw new CrispGeneratorException("No grammar set.");
            }

            // Convert CRISP Problem to PDDL
            try {
                converter.convert(grammar, xmlProblemReader, domain, problem);
            } catch (Exception e) {
                throw new CrispGeneratorException("Error parsing problem XML specification: " + e);
            }

            // run the planner
            try {
                plan = planner.runPlanner(domain, problem);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CrispGeneratorException("Error running the planner.");
            }
        }

        return plan;
    }

    public DerivationTree generate(Reader xmlProblemReader) throws CrispGeneratorException {
        plan = getCrispPlan(xmlProblemReader);

        // Decode the plan
        DerivationTree result = planDecoder.buildDerivationTreeFromPlan(plan, domain);
//        System.out.println("\nDerivation tree:\n" + result);
        return result;
    }

    public DerivationTree generate(String xmlProblem) throws CrispGeneratorException {
        plan = getCrispPlan(xmlProblem);

        // Decode the plan
        DerivationTree result = planDecoder.buildDerivationTreeFromPlan(plan, domain);
//        System.out.println("\nDerivation tree:\n" + result);
        return result;
    }


    public static void usage() {
        System.out.println("java crisp.main.BasicCrispGenerator [CRISP grammar file] [CRISP problem file]");
    }

    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Wrong number of command linearguments.");
            usage();
            System.exit(1);
        }

        CrispGeneratorInterface generator = new BasicCrispGenerator();
        try {
            generator.setGrammar(new FileReader(new File(args[0])));
        } catch (Exception e) {
            System.err.println("Could not open or read grammar XML file " + args[0]);
            e.printStackTrace();
            System.exit(1);
        }
        String sentence = "";
        try {
            sentence = generator.generateSentence(new FileReader(new File(args[1])));
        } catch (CrispGeneratorException e) {
            System.err.println("Could not run the CRISP generation system.");
            e.printStackTrace();
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.err.println("Could not open problem XML file " + args[2]);
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("RESULT:" + sentence);
    }

}
