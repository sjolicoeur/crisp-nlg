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

    @Test
    public void testWordOrder() throws CrispGeneratorException {
	String sentence = crispGenerator.generateSentence(problemReader);
        assertEquals("moveonestep and turnright then movetwosteps and movethreesteps then push the red button", sentence);
    }

}
