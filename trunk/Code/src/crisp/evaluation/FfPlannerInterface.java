package crisp.evaluation;

import java.lang.ProcessBuilder;
import java.lang.Process;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import crisp.converter.FastCRISPConverter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PddlOutputCodec;

import crisp.evaluation.ffplanparser.FfPlanParser;

import crisp.result.PCrispDerivationTreeBuilder;
import crisp.result.CrispDerivationTreeBuilder;
import crisp.result.DerivationTreeBuilder;

import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.codec.PCrispXmlInputCodec;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;
import de.saar.penguin.tag.visualize.JGraphVisualizer;

import de.saar.chorus.term.Term; 

import java.util.List;

//import javax.swing.JFrame;
//import org.jgraph.JGraph;


public class FfPlannerInterface implements PlannerInterface {
    
    public static final String FF_BIN = "/proj/penguin/planners/FF-v2.3/ff-linux64";
       
    public static final String TEMPDOMAIN_FILE = "tmpdomain.lisp";
    public static final String TEMPPROBLEM_FILE = "tmpproblem.lisp";
    public static final String TEMPRESULT_FILE = "tmpresult";
    
    private long preprocessingTime;
    private long searchTime;
    
    FfPlannerInterface() {
        preprocessingTime = 0;
        searchTime = 0;
    }
    
    public List<Term> runPlanner(Domain domain, Problem problem) throws Exception {        
        
        long start;
        long end;
        
        new PddlOutputCodec().writeToDisk(domain, problem, new FileWriter(new File(TEMPDOMAIN_FILE)),
                                                           new FileWriter(new File(TEMPPROBLEM_FILE)));
                                                                   
          
        // Run the FfPlanner
        ProcessBuilder ff_pb = new ProcessBuilder(FF_BIN, "-o "+TEMPDOMAIN_FILE, "-f "+TEMPPROBLEM_FILE);
        Process ffplanner = ff_pb.start();        
        ffplanner.waitFor();
        if (ffplanner.exitValue() != 0) {
            throw new RuntimeException("FF in "+FF_BIN+" terminated inappropriately.");
        }        
        
        Reader resultReader = new BufferedReader(new InputStreamReader(ffplanner.getInputStream()));
        try{
            FfPlanParser parser = new FfPlanParser(resultReader);
            return parser.plan();
        } catch(Exception e) {
            System.err.println("Exception while parsing planner input.");
            return null;
        }
                                                                                           
    
    }
    
     
    public List<Term> runPlanner(Domain domain, Problem problem, long timeout) throws Exception {
        //TODO: implement timeout for SGPlan
        return runPlanner(domain, problem);
    }

    public long getPreprocessingTime() {
        return preprocessingTime;
    }
    
    public long getSearchTime() {
        return searchTime;
    }
    
    public long getTotalTime() {
        return preprocessingTime + searchTime; 
    }
    
    
    public static void usage(){
        System.out.println("Usage: java crisp.evaluation.LamaPlannerInterface [CRISP grammar] [CIRISP problem]");
    }
    
    public static void main(String[] args) throws Exception{
        
         
        if (args.length<1) {
            System.err.println("No crisp problem specified");
            usage();
            System.exit(1);
        }
        
        // TODO some exception handling
        
		Domain domain = new Domain();
		Problem problem = new Problem();

		long start = System.currentTimeMillis();
        
        
        System.out.println("Reading grammar...");
        PCrispXmlInputCodec codec = new PCrispXmlInputCodec();
		ProbabilisticGrammar<Term> grammar = new ProbabilisticGrammar<Term>();	
		codec.parse(new File(args[0]), grammar);         
 
        File problemfile = new File(args[1]);
                
        System.out.println("Generating planning problem...");
		//new FastCRISPConverter().convert(grammar, problemfile, domain, problem);
        new FastCRISPConverter().convert(grammar, problemfile, domain, problem);

		long end = System.currentTimeMillis();

		System.out.println("Total runtime for problem generation: " + (end-start) + "ms");

        //System.out.println("Domain: " + domain );
		//System.out.println("Problem: " + problem);
            
        System.out.println("Running planner ... ");
        PlannerInterface planner = new FfPlannerInterface();
        List<Term> plan = planner.runPlanner(domain,problem);
        System.out.println(plan);
        DerivationTreeBuilder derivationTreeBuilder = new CrispDerivationTreeBuilder(grammar);
        DerivationTree derivTree = derivationTreeBuilder.buildDerivationTreeFromPlan(plan, domain);
        System.out.println(derivTree);        
        DerivedTree derivedTree = derivTree.computeDerivedTree(grammar);
        System.out.println(derivedTree);
        System.out.println(derivedTree.yield());
        /*
        System.out.println(grammar.getTree("t27").lexicalize(grammar.getLexiconEntry("yielding","t27")));
        System.out.println(grammar.getTree("t26").lexicalize(grammar.getLexiconEntry("d_dot_","t26")));
        
        DerivationTree derivTree = new DerivationTree();
        String node = derivTree.addNode(null,null, "t27", grammar.getLexiconEntry("yielding","t27"));
        derivTree.addNode(node, "n1", "t26", grammar.getLexiconEntry("d_dot_","t26"));
        DerivedTree derivedTree = derivTree.computeDerivedTree(grammar);
        */
  
        /*
        JFrame f = new JFrame("TAG viewer:");
        JGraph g = new JGraph();        
        JGraphVisualizer v = new JGraphVisualizer();        
        v.draw(derivedTree, g);        
               
        f.add(g);
        f.pack();
	    f.setVisible(true);	               
	    v.computeLayout(g);       
	    f.pack();           
        */
    }
    
    
}
