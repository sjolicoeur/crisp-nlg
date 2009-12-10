package crisp.main;

import crisp.converter.FastCRISPConverter;
import crisp.converter.grammar.TAGrammar;
import crisp.evaluation.FfPlannerInterface;
import crisp.evaluation.PlannerInterface;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.result.CrispDerivationTreeBuilder;
import crisp.result.DerivationTreeBuilder;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.codec.CrispXmlInputCodec;
import de.saar.penguin.tag.codec.InputCodec;
import de.saar.penguin.tag.codec.ParserException;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;
import de.saar.penguin.tag.grammar.Grammar;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple implementation of the @{CRISPGeneratorInterface} using the current state
 * of the standard CRISP generation system (with "steps" andwithout pragmatics or
 * context or probabilistic grammars). It also creates a new PDDL domain for every
 * problem instance and thus does not benefit from Konstantinas optimizations. 
 * 
 * Use this class only through the @{CRISPGeneratorInterface} as it is likely to be modified
 * very soon.
 * 
 * @author dbauer
 */
public class BasicCrispGenerator implements CrispGeneratorInterface {

    Grammar grammar = null;
    FastCRISPConverter converter;
    PlannerInterface planner;
    DerivationTreeBuilder planDecoder = null;

    public BasicCrispGenerator() {
        converter = new FastCRISPConverter();
        planner = new FfPlannerInterface();
    }

    // *** Methods to load the grammar ***
    public void setGrammar(Grammar grammar) {
        this.grammar = grammar;
        planDecoder = new CrispDerivationTreeBuilder(grammar);
    }

    public void setGrammar(Reader xmlGrammarReader) throws IOException, ParserException {
        InputCodec codec = new CrispXmlInputCodec();
        Grammar newGrammar = new Grammar();
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
        return derivedTree.yield();
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

    public DerivationTree generate(Reader xmlProblemReader) throws CrispGeneratorException {
        // Check if grammar is loaded
        if (grammar == null) {
            throw new CrispGeneratorException("No grammar set.");
        } 

        // Convert CRISP Problem to PDDL
        Domain domain = new Domain();
        Problem problem = new Problem();
        try {
            converter.convert(grammar, xmlProblemReader, domain, problem);
        } catch (Exception e) {
            throw new CrispGeneratorException("Error parsing problem XML specification: "+e);
        }

        // Run the planner
        List<Term> plan = null;
        try {
           plan = planner.runPlanner(domain, problem);
        } catch (Exception e) {
            System.out.println("Error running the planner: "+e);
        }

        DerivationTree result = planDecoder.buildDerivationTreeFromPlan(plan, domain);
        return result;
    }

    public DerivationTree generate(String xmlProblem) throws CrispGeneratorException {
        return generate(new StringReader(xmlProblem));
    }



    public static void usage(){
        System.out.println("java crisp.main.BasicCrispGenerator [CRISP grammar file] [CRISP problem file]");
    }

    public static void main(String[] args){

        if (args.length != 2) {
            System.err.println("Wrong number of command linearguments.");
            usage();
            System.exit(1);
        }

        CrispGeneratorInterface generator = new BasicCrispGenerator();
        try {
            generator.setGrammar(new FileReader(new File(args[1])));        
        } catch (Exception e) {
            System.err.println("Could not open or read grammar XML file "+args[0]);
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
            System.err.println("Could not open problem XML file "+args[2]);
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println(sentence);
    }

}
