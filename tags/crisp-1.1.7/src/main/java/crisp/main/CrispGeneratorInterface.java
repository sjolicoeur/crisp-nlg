package crisp.main;

import de.saar.penguin.tag.codec.ParserException;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.grammar.Grammar;
import java.io.IOException;
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
    public void setGrammar(Reader xmlGrammarReader) throws ParserException, IOException;
    public void setGrammar(String xmlGrammar) throws ParserException;

    /**
     * Solve a generation problem using a preset domain. 
     * @param xmlProblemReader
     * @return The output sentence as a String
     */
    public String generateSentence(Reader xmlProblemReader) throws CrispGeneratorException;
    public String generateSentence(String xmlProblem) throws CrispGeneratorException;
    public List<String> generateWordSequence(Reader xmlProblemReader) throws CrispGeneratorException;
    public List<String> generateWordSequence(String xmlProblem) throws CrispGeneratorException;
    public DerivationTree generate(Reader xmlProblemReader) throws CrispGeneratorException;
    public DerivationTree generate(String xmlProblem) throws CrispGeneratorException;
}
