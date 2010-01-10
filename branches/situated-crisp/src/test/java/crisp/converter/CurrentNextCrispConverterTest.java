package crisp.converter;

import static org.junit.Assert.*;

import java.io.*;

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
    private Domain domain;
    private Problem problem;

    private CrispGrammar grammar;

    @Before
    public void setUp() throws FileNotFoundException, ParserException, IOException, ParserConfigurationException, SAXException {
        CurrentNextCrispConverter converter;
        domain = new Domain();
        problem = new Problem();

        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        grammar = new CrispGrammar();
        codec.parse(new InputStreamReader(getClass().getResourceAsStream("/grammar-scrisp.xml")), grammar);

        Reader problemfile = new InputStreamReader(getClass().getResourceAsStream("/problem-scrisp-give1-generated.xml"));

        converter = new CurrentNextCrispConverter();
        converter.convert(grammar, problemfile, domain, problem);
    }

    @Test
    public void testConvertOnce() {
        assert domain != null;
        assert problem != null;
    }

    @Test
    public void testConvertTwiceFreshGrammar() throws IOException, SAXException, ParserConfigurationException, ParserException {
        Domain domain2 = new Domain();
        Problem problem2 = new Problem();

        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        grammar = new CrispGrammar();
        codec.parse(new InputStreamReader(getClass().getResourceAsStream("/grammar-scrisp.xml")), grammar);


        CurrentNextCrispConverter converter = new CurrentNextCrispConverter();
        Reader problemfile = new InputStreamReader(getClass().getResourceAsStream("/problem-scrisp-give1-generated.xml"));

        converter.convert(grammar, problemfile, domain2, problem2);

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

    @Test
    public void testConvertTwice() throws IOException, SAXException, ParserConfigurationException {
        Domain domain2 = new Domain();
        Problem problem2 = new Problem();
        CurrentNextCrispConverter converter = new CurrentNextCrispConverter();
        Reader problemfile = new InputStreamReader(getClass().getResourceAsStream("/problem-scrisp-give1-generated.xml"));

        converter.convert(grammar, problemfile, domain2, problem2);

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
