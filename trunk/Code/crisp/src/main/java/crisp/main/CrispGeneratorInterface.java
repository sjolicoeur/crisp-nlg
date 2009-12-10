package crisp.main;

import crisp.result.Grammar;
import de.saar.penguin.tag.derivation.DerivationTree;
import java.io.Reader;
import java.util.List;

/**
 * An interface that makes it easy to use a CRISP generation system
 * as part of other java projects (e.g. a GIVE NLG Server).
 * @author dbauer
 */
public interface CrispGeneratorInterface {

    /**
     * Preset a CRISP domain which is used to solve all generation problems.
     * @param xmlDomainReader A reader that accesses a CRISP grammar specification in XML format.
     */
    public void setGrammar(Grammar grammar);
    public void setGrammar(Reader xmlGrammarReader);
    public void setGrammar(String xmlGrammar);

    /**
     * Solve a generation problem using a preset domain. 
     * @param xmlProblemReader
     * @return The output sentence as a String
     */
    public String generateSentence(Reader xmlProblemReader);
    public String generateSentence(String xmlProblem);
    public List<String> generateWordSequence(Reader xmlProblemReader);
    public List<String> generateWordSequence(String xmlProblem);
    public DerivationTree generate(Reader xmlProblemReader);
    public DerivationTree generate(String xmlProblem);
}
