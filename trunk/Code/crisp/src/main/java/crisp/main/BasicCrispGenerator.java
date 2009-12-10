package crisp.main;

import crisp.converter.grammar.TAGrammar;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.grammar.Grammar;
import java.io.Reader;
import java.util.List;

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

    Grammar grammar;

    public BasicCrispGenerator(){
        
    }

    public void setGrammar(Grammar grammar) {
        this.grammar = grammar;
    }

    public void setGrammar(Reader xmlGrammarReader) {
        
    }

    public void setGrammar(String xmlGrammar) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String generateSentence(Reader xmlProblemReader) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String generateSentence(String xmlProblem) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<String> generateWordSequence(Reader xmlProblemReader) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<String> generateWordSequence(String xmlProblem) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public DerivationTree generate(Reader xmlProblemReader) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public DerivationTree generate(String xmlProblem) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setGrammar(crisp.result.Grammar grammar) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
