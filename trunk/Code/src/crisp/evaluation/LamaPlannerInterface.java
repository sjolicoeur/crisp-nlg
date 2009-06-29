package crisp.evaluation;

import java.lang.ProcessBuilder;
import java.lang.Process;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileInputStream;

import crisp.converter.ProbCRISPConverter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.CostPddlOutputCodec;

import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.codec.PCrispXmlInputCodec;
import de.saar.chorus.term.Term; 


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
    
    
    public void runPlanner(Domain domain, Problem problem) throws Exception {
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
        ProbCRISPConverter.convert(grammar, problemfile, domain, problem);

		long end = System.currentTimeMillis();

		System.out.println("Total runtime for problem generation: " + (end-start) + "ms");

        System.out.println("Domain: " + domain );
		System.out.println("Problem: " + problem);

        PlannerInterface planner = new LamaPlannerInterface();
        planner.runPlanner(domain,problem);
        
    }
    
    
}
