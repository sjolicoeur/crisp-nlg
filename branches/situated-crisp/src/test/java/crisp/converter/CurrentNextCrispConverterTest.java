package crisp.converter;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import crisp.planningproblem.Action;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import de.saar.penguin.tag.codec.ParserException;
import de.saar.penguin.tag.grammar.CrispGrammar;
import de.saar.penguin.tag.grammar.SituatedCrispXmlInputCodec;

public class CurrentNextCrispConverterTest {
    CurrentNextCrispConverter converter;
    Domain domain;
    Problem problem;
    Domain domain2;
    Problem problem2;
    
    @Before
    public void setUp() throws FileNotFoundException, ParserException, IOException, ParserConfigurationException, SAXException {
        domain = new Domain();
        problem = new Problem();

        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        CrispGrammar grammar = new CrispGrammar();
        codec.parse(new FileReader(new File("grammar-scrisp.xml")), grammar);
        File problemfile = new File("problem-scrisp-give1-generated.xml");
        converter = new CurrentNextCrispConverter();
        converter.convert(grammar, problemfile, domain, problem);
        domain2 = new Domain();
        problem2 = new Problem();
        converter.convert(grammar, problemfile, domain2, problem2);
    }
    
    @Test
    public void testConvert() {
	for (Action action : domain.getActions()) {
	    for (Action action2 : domain2.getActions()) {
		if (action.getPredicate().equals(action2.getPredicate())) {
		    
		    // this assertion might be generally questionable, since literals may be ordered differently, 
		    // however it works for our problem
		    assertEquals(action.getPrecondition(), action2.getPrecondition());
		    assertEquals(action.getEffect(), action2.getEffect());
		}
	    }
	}
    }

}
