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

import crisp.converter.ProbCRISPConverter;
import crisp.converter.FastCRISPConverter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.CostPddlOutputCodec;
import crisp.planningproblem.codec.OutputCodec;

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
import java.util.Timer;

//import javax.swing.JFrame;
//import org.jgraph.JGraph;


public class LamaPlannerInterface implements PlannerInterface {
    
    public static final String PYTHON_BIN = "/usr/bin/python";
    public static final String LAMA_PREFIX = "/home/CE/dbauer/LAMA/";
    public static final String LAMA_SCRIPT = "/home/CE/dbauer/LAMA/lama.sh";
    public static final String LAMA_TRANSLATOR = "translate/translate.py";
    public static final String LAMA_PREPROCESSOR = "preprocess/preprocess-mac";
    public static final String LAMA_SEARCH = "search/release-search-mac";
    
    public static final String TEMPDOMAIN_FILE = "/local/dbauer/tmpdomain.lisp";
    public static final String TEMPPROBLEM_FILE = "/local/dbauer/tmpproblem.lisp";
    public static final String TEMPRESULT_FILE = "/local/dbauer/tmpresult";

    public static final String LAMA_STRATEGIES = "fF";

    public static final long DEFAULT_TIMEOUT = 60000;
    
    private long preprocessingTime;
    private long searchTime;
    private long totalTime;

    LamaPlannerInterface() {
        preprocessingTime = 0;
        searchTime = 0;
    }

    
    private void pipeFileToProcess(Process p, File f) throws IOException {
        byte[] buf = new byte[1024];
        
        OutputStream out = new BufferedOutputStream(p.getOutputStream());
        InputStream in = new FileInputStream(f);
                
        int len = 0;        
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);    
        }        
        in.close();
        out.close();                
    }
    
    public List<Term> runPlanner(Domain domain, Problem problem) throws Exception {
        return runPlanner(domain, problem, DEFAULT_TIMEOUT);
    }
    
    public List<Term> runPlanner(Domain domain, Problem problem, long timeout) throws Exception {
        // This does look a bit like LAMA.sh. Calling the individual commands from here makes it easier to measure time
        
        long start;
        long end;
    
        
        OutputCodec outputCodec = new CostPddlOutputCodec();
        outputCodec.writeToDisk(domain, problem, new FileWriter(new File(TEMPDOMAIN_FILE)),
                                                 new FileWriter(new File(TEMPPROBLEM_FILE)));
                                                                   
        
        outputCodec = null;  
        // Run the LAMA translator
        /*
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
        */
        
        // Run search
        start = System.currentTimeMillis();
        Process lamaproc = Runtime.getRuntime().exec("bash -e "+LAMA_SCRIPT+" "+TEMPDOMAIN_FILE+" "+TEMPPROBLEM_FILE+" "+TEMPRESULT_FILE);      
        
        BufferedReader errstream = new BufferedReader(new InputStreamReader(lamaproc.getErrorStream()));
        
        Timer timer = new Timer();
        timer.schedule(new InterruptScheduler(Thread.currentThread()), timeout);        
        try{
            lamaproc.waitFor();
        } catch (InterruptedException e) {
            System.err.println("Planner timed out after "+timeout+" ms.");
            return null;      
        } finally {
            timer.cancel();
            lamaproc.destroy();
        }
        
        end = System.currentTimeMillis();
        this.totalTime = end-start;                
        if (lamaproc.exitValue() != 0) {
            throw new RuntimeException("LAMA in "+LAMA_SCRIPT+ " exited inappropriately.");
        }              
        lamaproc.destroy();
        lamaproc = null;  
        
                
        extractPlanningTime(errstream);
        
        FileReader resultFileReader = new FileReader(new File(TEMPRESULT_FILE+".1"));        
        
        
        try{
            LamaPlanParser parser = new LamaPlanParser(resultFileReader);
            return parser.plan();
        } catch(Exception e) {
            System.err.println("Exception while parsing planner output.");
            return null;
        } finally {
            resultFileReader = null;
        }
                                                                                           
    }
        
    private long parseTimeSpan(String timespan){
        String[] minutesSeconds = timespan.split("m");
        long time = new Integer(minutesSeconds[0])*60000;
        // Cut trailing "s"
        String secondStr = minutesSeconds[1].substring(0,minutesSeconds[1].length()-1);
        Float millisecondFloat = new Float(secondStr)*1000;
        time = time + millisecondFloat.longValue();
        return time;
    }
    
    private void extractPlanningTime(BufferedReader plannerErrorStream) throws IOException{
        String line;
        int counter = 0;       
        long instantiateTime = 0;
        long preprocTime = 0;
        while ((line = plannerErrorStream.readLine()) != null) {
            if (line.startsWith("real")) {
                switch (counter) {
                    case 0:
                        instantiateTime = parseTimeSpan(line.split("\\s")[1]);
                        break;
                    case 1:
                        this.preprocessingTime = instantiateTime + parseTimeSpan(line.split("\\s")[1]);
                        break;
                    case 2:
                        this.searchTime = parseTimeSpan(line.split("\\s")[1]);
                        return;
                }
                counter++;
            }
        }
        
    }
    
    public long getPreprocessingTime() {
        return preprocessingTime;
    }
    
    public long getTotalTime() {
        return preprocessingTime + searchTime;
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
		//new FastCRISPConverter().convert(grammar, problemfile, domain, problem);
        new ProbCRISPConverter().convert(grammar, problemfile, domain, problem);

		long end = System.currentTimeMillis();

		System.out.println("Total runtime for problem generation: " + (end-start) + "ms");

        //System.out.println("Domain: " + domain );
		//System.out.println("Problem: " + problem);
            
        System.out.println("Running planner ... ");
        PlannerInterface planner = new LamaPlannerInterface();
        List<Term> plan = planner.runPlanner(domain,problem, 600000);
        System.out.println(planner.getTotalTime());
        System.out.println(planner.getPreprocessingTime());
        System.out.println(planner.getSearchTime());
        System.out.println(plan);
        DerivationTreeBuilder derivationTreeBuilder = new PCrispDerivationTreeBuilder(grammar);
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
