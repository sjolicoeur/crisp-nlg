package crisp.result;

import crisp.converter.CurrentNextCrispConverter;
import crisp.planner.external.FfPlannerInterface;
import crisp.planner.external.PlannerInterface;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.codec.ParserException;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;
import de.saar.penguin.tag.grammar.CrispGrammar;
import de.saar.penguin.tag.grammar.SituatedCrispXmlInputCodec;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: koller
 * Date: Jan 18, 2010
 * Time: 7:58:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class CrispDerivationTreeBuilderTest {
    @Test
    public void testModifierOrder() throws Exception, ParserException, SAXException, ParserConfigurationException {
        CurrentNextCrispConverter converter;
        Domain domain = new Domain();
        Problem problem = new Problem();

        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        CrispGrammar grammar = new CrispGrammar();
        codec.parse(new InputStreamReader(getClass().getResourceAsStream("/modifiers-grammar.xml")), grammar);

        Reader problemfile = new InputStreamReader(getClass().getResourceAsStream("/modifiers-problem.xml"));

        converter = new CurrentNextCrispConverter();
        converter.convert(grammar, problemfile, domain, problem);

        CrispDerivationTreeBuilder planDecoder = new CrispDerivationTreeBuilder(grammar);
        PlannerInterface planner = new FfPlannerInterface();
        List<Term> plan = planner.runPlanner(domain, problem);
        DerivationTree derivationTree = planDecoder.buildDerivationTreeFromPlan(plan, domain, "s");
        DerivedTree derivedTree = derivationTree.computeDerivedTree(grammar);

//        System.err.println("\n\nderivation:\n" + derivationTree);
//        System.err.println("\n\nderived:\n" + derivedTree);

        String sent = derivedTree.yield();

        assertEquals("the left blue button sleeps", sent);
    }

    @Test
    public void testModifierOrder2() throws Exception, ParserException, SAXException, ParserConfigurationException {
        CurrentNextCrispConverter converter;
        Domain domain = new Domain();
        Problem problem = new Problem();

        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        CrispGrammar grammar = new CrispGrammar();
        codec.parse(new InputStreamReader(getClass().getResourceAsStream("/modifiers-grammar.xml")), grammar);

        Reader problemfile = new InputStreamReader(getClass().getResourceAsStream("/modifiers-problem2.xml"));

        converter = new CurrentNextCrispConverter();
        converter.convert(grammar, problemfile, domain, problem);

        CrispDerivationTreeBuilder planDecoder = new CrispDerivationTreeBuilder(grammar);
        PlannerInterface planner = new FfPlannerInterface();
        List<Term> plan = planner.runPlanner(domain, problem);
        DerivationTree derivationTree = planDecoder.buildDerivationTreeFromPlan(plan, domain, "s");
        DerivedTree derivedTree = derivationTree.computeDerivedTree(grammar);

//        System.err.println("\n\nderivation:\n" + derivationTree);
//        System.err.println("\n\nderived:\n" + derivedTree);

        String sent = derivedTree.yield();

        assertEquals("the blue left button sleeps", sent);
    }
}
