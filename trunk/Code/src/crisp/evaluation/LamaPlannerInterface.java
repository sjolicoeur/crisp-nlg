package crisp.evaluation;

import java.lang.ProcessBuilder;
import java.lang.Process;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileReader;

import crisp.converter.ProbCRISPConverter;
import crisp.converter.FastCRISPConverter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.CostPddlOutputCodec;

import crisp.evaluation.lamaplanparser.LamaPlanParser;

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

import javax.swing.JFrame;
//import org.jgraph.JGraph;


public class LamaPlannerInterface implements PlannerInterface {
    
    public static final String PYTHON_BIN = "/usr/bin/python";
    public static final String LAMA_PREFIX = "/home/CE/dbauer/LAMA/";
    public static final String LAMA_TRANSLATOR = "translate/translate.py";
    public static final String LAMA_PREPROCESSOR = "preprocess/preprocess-mac";
    public static final String LAMA_SEARCH = "search/release-search-mac";
    
    public static final String TEMPDOMAIN_FILE = "tmpdomain.lisp";
    public static final String TEMPPROBLEM_FILE = "tmpproblem.lisp";
    public static final String TEMPRESULT_FILE = "tmpresult";

    public static final String LAMA_STRATEGIES = "fF";

    private long preprocessingTime;
    private long searchTime;
    
    LamaPlannerInterface() {
        preprocessingTime = 0;
        searchTime = 0;
    }

    
    private void pipeFileToProcess(Process p, File f) throws IOException {
        byte[] buf = new byte[1024];
        
        OutputStream out = p.getOutputStream();
        InputStream in = new FileInputStream(f);
        
        int len = 0;        
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);    
        }        
        in.close();
        out.close();                
    }
    
    
    public List<Term> runPlanner(Domain domain, Problem problem) throws Exception {
        // This does look a bit like LAMA.sh. Calling the individual commands from here makes it easier to measure time
        
        long start;
        long end;
        
        new CostPddlOutputCodec().writeToDisk(domain, problem, new FileWriter(new File(TEMPDOMAIN_FILE)),
                                                           new FileWriter(new File(TEMPPROBLEM_FILE)));
                                                                   
          
        // Run the LAMA translator
        ProcessBuilder translate_pb = new ProcessBuilder(PYTHON_BIN, LAMA_PREFIX+LAMA_TRANSLATOR, TEMPDOMAIN_FILE, TEMPPROBLEM_FILE);
        Process translator = translate_pb.start();        
        translator.waitFor();
        if (translator.exitValue() != 0) {
            throw new RuntimeException("LAMA translator "+PYTHON_BIN+" "+LAMA_PREFIX+LAMA_TRANSLATOR + " terminated badly.");
        }
        
        // Run the LAMA preprocessor
        start = System.currentTimeMillis();
        ProcessBuilder preproc_pb = new ProcessBuilder(LAMA_PREFIX+LAMA_PREPROCESSOR);                         
        Process preprocessor = preproc_pb.start();
        pipeFileToProcess(preprocessor, new File("output.sas"));
        preprocessor.waitFor();        
        end = System.currentTimeMillis();        
        this.preprocessingTime = end-start;
        if (preprocessor.exitValue() != 0) {
            throw new RuntimeException("Couldn't run LAMA preprocessor "+PYTHON_BIN+" "+LAMA_PREFIX+LAMA_PREPROCESSOR);
        }        
        
        // Run search
        start = System.currentTimeMillis();
        ProcessBuilder search_pb = new ProcessBuilder(LAMA_PREFIX+LAMA_SEARCH,LAMA_STRATEGIES,TEMPRESULT_FILE);
        Process search = search_pb.start();
        pipeFileToProcess(search, new File("output"));
        search.waitFor();
        end = System.currentTimeMillis();
        this.searchTime = end-start;                
        if (search.exitValue() != 0) {
            throw new RuntimeException("Couldn't run LAMA search"+LAMA_PREFIX+LAMA_SEARCH);
        }                
        
        FileReader resultFileReader = new FileReader(new File(TEMPRESULT_FILE+".1"));
        try{
            LamaPlanParser parser = new LamaPlanParser(resultFileReader);
            return parser.plan();
        } catch(Exception e) {
            System.err.println("Exception while parsing planner input.");
            return null;
        }
                                                                                           
    }
        
    public long getPreprocessingTime() {
        return preprocessingTime;
    }
    
    public long getSearchTime() {
        return searchTime;
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
		//FastCRISPConverter.convert(grammar, problemfile, domain, problem);
        new FastCRISPConverter().convert(grammar, problemfile, domain, problem);

		long end = System.currentTimeMillis();

		System.out.println("Total runtime for problem generation: " + (end-start) + "ms");

        //System.out.println("Domain: " + domain );
		//System.out.println("Problem: " + problem);
            
        System.out.println("Running planner ... ");
        PlannerInterface planner = new LamaPlannerInterface();
        List<Term> plan = planner.runPlanner(domain,problem);
        System.out.println(plan);
        DerivationTreeBuilder derivationTreeBuilder = new CrispDerivationTreeBuilder(grammar);
        DerivationTree derivTree = derivationTreeBuilder.buildDerivationTreeFromPlan(plan, domain);
        System.out.println(derivTree);        
        DerivedTree derivedTree = derivTree.computeDerivedTree(grammar);
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
        v.draw(grammar.getTree("t26"), g);        
               
        f.add(g);
        f.pack();
	    f.setVisible(true);	               
	    v.computeLayout(g);       
	    f.pack();    
      */  
      
    }
    
    
}