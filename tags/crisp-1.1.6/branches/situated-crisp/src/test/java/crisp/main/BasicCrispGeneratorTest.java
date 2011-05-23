package crisp.main;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.saar.penguin.tag.codec.ParserException;


public class BasicCrispGeneratorTest {
    private BasicCrispGenerator crispGenerator;
    private Reader problemReader;

    @Before
    public void setUp() throws FileNotFoundException, ParserException, IOException, ParserConfigurationException, SAXException {
	    Reader grammarReader = new InputStreamReader(getClass().getResourceAsStream("/grammar-scrisp.xml"));
	    problemReader = new InputStreamReader(getClass().getResourceAsStream("/problem-scrisp-give1-generated-2.xml")); 

	    crispGenerator = new BasicCrispGenerator("-B");
	    crispGenerator.setGrammar(grammarReader);
    }

    /*
     * This was meant as a test for word order. The intended output was "... then movefoursteps and moveonestep then ...".
     * Right now (July 10), we cannot reproduce in what way this is a word order test -- the word order seems fine, it's
     * just that FF finds a different plan. So we consider this test okay for now, and will come back to the word order
     * problem if we ever find a compelling problem example again. - AK, KG, Jul 10.
     */
    @Test
    public void testNotReallyWordOrder() throws CrispGeneratorException {
	String sentence = crispGenerator.generateSentence(problemReader);
        assertEquals("moveonestep and turnright then movetwosteps and movethreesteps then push the red button", sentence);
    }

}
