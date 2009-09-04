package crisp.evaluation;


import crisp.converter.BackoffModelProbCRISPConverter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import crisp.converter.TreeModelProbCRISPConverter;
import crisp.converter.ProbCRISPConverter;
import crisp.converter.FastCRISPConverter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.CostPddlOutputCodec;
import crisp.planningproblem.codec.OutputCodec;
import crisp.planningproblem.codec.PddlOutputCodec;

import crisp.evaluation.lamaplanparser.LamaPlanParser;

import crisp.result.PCrispDerivationTreeBuilder;
import crisp.result.CrispDerivationTreeBuilder;
import crisp.result.DerivationTreeBuilder;

import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.codec.PCrispXmlInputCodec;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;

import de.saar.chorus.term.Term; 

import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

//import javax.swing.JFrame;
//import org.jgraph.JGraph;


public class LamaPlannerInterface implements PlannerInterface {

    public static final String LAMA_PROPERTIES_FILE = "lama.properties";
    
    public static final String DEF_LAMA_SCRIPT = "lama.sh";
    public static final String DEF_TEMPDOMAIN_FILE = "tmpdomain.lisp";
    public static final String DEF_TEMPPROBLEM_FILE = "tmpproblem.lisp";
    public static final String DEF_TEMPRESULT_FILE = "tmpresult";
    public static final String LAMA_STRATEGIES = "fF";    
    public static final long DEFAULT_TIMEOUT = 600000;
    
    private long preprocessingTime;
    private long searchTime;
    private long totalTime;

    private String lamaScript;
    private String tempDomainFile;
    private String tempProblemFile;
    private String tempResultFile;
    private String lamaStrategies;
    private String username;
    private long defaultTimeout;
    

    LamaPlannerInterface() {


        // Read properties file.
        Properties props = new Properties();

        username = System.getProperty("user.name");

        try {
            props.load(new FileInputStream(LAMA_PROPERTIES_FILE));            
            lamaScript = props.getProperty("lamaScript");
            tempDomainFile = props.getProperty("tempDomainFile");
            tempProblemFile = props.getProperty("tempProblemFile");
            tempResultFile = props.getProperty("tempResultFile");
            lamaStrategies = props.getProperty("lamaStrategies");            
            defaultTimeout = new Integer(props.getProperty("defaultTimeout"));
        } catch (IOException e) {
            System.err.print("Couldn't read LAMA properties file "+LAMA_PROPERTIES_FILE +". Using default configuration.");
            lamaScript = DEF_LAMA_SCRIPT;
            tempDomainFile = DEF_TEMPDOMAIN_FILE;
            tempProblemFile = DEF_TEMPPROBLEM_FILE;
            tempResultFile = DEF_TEMPRESULT_FILE;
            lamaStrategies = LAMA_STRATEGIES;
            defaultTimeout = DEFAULT_TIMEOUT;
        }

        preprocessingTime = 0;
        searchTime = 0;
    }

    /* Call PS to collect all process IDs of possible LAMA sub-processes
     * WARNING: this will kill all LAMA processes running on the machine  
     * and other processes with the same name!
     */
    private void killLamaChildProcesses() throws IOException, InterruptedException{
        Process ps = Runtime.getRuntime().exec("ps -f -u"+username);
        ps.waitFor();
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(ps.getInputStream()));
    
        String line;
        
        Pattern pattern = Pattern.compile("translate\\.py|release-search|preprocess");
        while ((line = inputStream.readLine()) != null) {
            String[] lineParts = line.split(" +");
            Matcher matcher = pattern.matcher(line); 
            
            // check if this is one of the LAMA binaries and if it is owned 
            // by this user
            if (lineParts[0].equals(username) && matcher.find()) {
                String pid = lineParts[1]; // Assume that tabular values in the 
                                                  // ps output are seperated by 
                                                  // whitespaces and that the second 
                                                  // value is the PID
                System.err.println("Killing process " +pid);
                Process kill = Runtime.getRuntime().exec("kill "+pid);
                kill.waitFor();
            }
        }
    }

    /** 
     * Run the planner with default timeout.
     * @param domain The planning domain/operators for the problem
     * @param problem The planning problem
     * @return a list of ground (compound) Terms describing instantiated plan actions
     */
    public List<Term> runPlanner(Domain domain, Problem problem) throws Exception {
        return runPlanner(domain, problem, defaultTimeout);
    }
    
    /** 
     * Run the planner with default timeout.
     * @param domain The planning domain/operators for the problem
     * @param problem The planning problem
     * @return a list of ground (compound) Terms describing instantiated plan actions
     */
    public List<Term> runPlanner(Domain domain, Problem problem, long timeout) throws Exception {
        // This does look a bit like LAMA.sh. Calling the individual commands from here makes it easier to measure time
        
        long start;
        long end;
    
        OutputCodec outputCodec = new PddlOutputCodec();
        outputCodec.writeToDisk(domain, problem, new FileWriter(new File(tempDomainFile)),
                                                 new FileWriter(new File(tempProblemFile)));
        
        outputCodec = null;  
        
        // Run the planner 
        start = System.currentTimeMillis();
        
        Timer timer = new Timer();
        timer.schedule(new InterruptScheduler(Thread.currentThread()), timeout);        
        Process lamaproc = Runtime.getRuntime().exec("bash -e "+lamaScript+" "+tempDomainFile+" "+tempProblemFile+" "+tempResultFile);
        BufferedReader errstream = new BufferedReader(new InputStreamReader(lamaproc.getErrorStream()));
        try{            
            lamaproc.waitFor();
            end = System.currentTimeMillis();
            this.totalTime = end-start;                
            if (lamaproc.exitValue() != 0) {
                throw new RuntimeException("LAMA in "+lamaScript+ " exited inappropriately. Probably no solution found.");
            }                              
            extractPlanningTime(errstream);

        } catch (InterruptedException e) {
            System.err.println("Planner timed out after "+timeout+" ms.");
            return null;      
        } finally {
            timer.cancel();
            lamaproc.destroy();
            killLamaChildProcesses(); // kill remaining LAMA processes
        }
        
        
        
        FileReader resultFileReader = new FileReader(new File(tempResultFile+".1"));
        
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
        new BackoffModelProbCRISPConverter().convert(grammar, problemfile, domain, problem);

        long end = System.currentTimeMillis();

	System.out.println("Total runtime for problem generation: " + (end-start) + "ms");

            
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
        
    }


}
