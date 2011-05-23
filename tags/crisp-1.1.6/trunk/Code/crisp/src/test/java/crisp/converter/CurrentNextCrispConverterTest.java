package crisp.converter;

import static org.junit.Assert.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import crisp.planner.external.FfPlannerInterface;
import crisp.planner.external.PlannerInterface;
import crisp.result.CrispDerivationTreeBuilder;
import crisp.result.DerivationTreeBuilder;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;
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
    private SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
    private DerivationTreeBuilder planDecoder;
    private CrispGrammar grammar;

    /*
    @Before
    public void setUp() throws FileNotFoundException, ParserException, IOException, ParserConfigurationException, SAXException {
        CurrentNextCrispConverter converter;
        domain = new Domain();
        problem = new Problem();

        grammar = new CrispGrammar();
        codec.parse(new InputStreamReader(getClass().getResourceAsStream("/grammar-scrisp.xml")), grammar);

        Reader problemfile = new InputStreamReader(getClass().getResourceAsStream("/problem-scrisp-give1-generated.xml"));

        converter = new CurrentNextCrispConverter();
        converter.convert(grammar, problemfile, domain, problem);

        planDecoder = new CrispDerivationTreeBuilder(grammar);
    }
     * 
     */

    private void loadAndConvert(String grammarResourceName, String problemResourceName) throws ParserException, IOException, ParserConfigurationException, SAXException {
        CurrentNextCrispConverter converter;
        domain = new Domain();
        problem = new Problem();

        grammar = new CrispGrammar();
        codec.parse(new InputStreamReader(getClass().getResourceAsStream(grammarResourceName)), grammar);

        Reader problemfile = new InputStreamReader(getClass().getResourceAsStream(problemResourceName));

        converter = new CurrentNextCrispConverter();
        converter.convert(grammar, problemfile, domain, problem);

        planDecoder = new CrispDerivationTreeBuilder(grammar);
    }

    @Test
    public void testConvertOnce() throws Exception {
        loadAndConvert("/grammar-scrisp.xml", "/problem-scrisp-give1-generated.xml");
        assert domain != null;
        assert problem != null;
    }

    @Test
    public void testConvertTwiceFreshGrammar() throws IOException, SAXException, ParserConfigurationException, ParserException {
        loadAndConvert("/grammar-scrisp.xml", "/problem-scrisp-give1-generated.xml");

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
    public void testConvertTwice() throws Exception {
        loadAndConvert("/grammar-scrisp.xml", "/problem-scrisp-give1-generated.xml");

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

    @Test
    public void testDerivationTree() throws Exception {
        loadAndConvert("/grammar-scrisp.xml", "/problem-scrisp-give1-generated.xml");

        PlannerInterface planner = new FfPlannerInterface();
        List<Term> plan = planner.runPlanner(domain, problem);
        DerivationTree result = planDecoder.buildDerivationTreeFromPlan(plan, domain);

        assert result != null;

        List<String> results = new ArrayList<String>();

        results.add("u3");
        assertEquals(results, result.getChildren("u2").get("n4"));

        results.clear();
        results.add("u5");
        assertEquals(results, result.getChildren("u4").get("n4"));
    }

    @Test
    public void testCorrectSentence() throws Exception {
        loadAndConvert("/grammar-scrisp.xml", "/problem-scrisp-give1-generated.xml");
        String sent = planAndExtract();

        assertEquals("movetwosteps and turnleft then push the green button", sent);
    }

    @Test
    public void testLongerScrispSentence() throws Exception {
        loadAndConvert("/grammar-scrisp.xml", "/problem-scrisp-give1-generated-2.xml");
        String sent = planAndExtract();

        assertEquals("moveonestep and turnright then movetwosteps and movethreesteps then push the red button", sent);
    }

    private String planAndExtract() throws Exception {
        PlannerInterface planner = new FfPlannerInterface();
        List<Term> plan = planner.runPlanner(domain, problem);
        DerivationTree derivationTree = planDecoder.buildDerivationTreeFromPlan(plan, domain);
        DerivedTree derivedTree = derivationTree.computeDerivedTree(grammar);
        String sent = derivedTree.yield();
        return sent;
    }

    @Test
    public void testTransitive() throws Exception {
        loadAndConvert("/modifiers-grammar.xml", "/modifiers-transitive-problem.xml");
        String sent = planAndExtract();

        assertEquals("the blue button likes the red button", sent);
    }
}
