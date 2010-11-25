package crisp.converter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.grammar.CrispGrammar;
import de.saar.penguin.tag.grammar.Grammar;

public interface CrispConverter {

    public void convert(Grammar<Term> grammar, Reader fileReader, Domain domain, Problem problem) throws ParserConfigurationException, SAXException, IOException;
    public void convert(Grammar<Term> grammar, File problemfile, Domain domain, Problem problem) throws ParserConfigurationException, SAXException, IOException; 

}
