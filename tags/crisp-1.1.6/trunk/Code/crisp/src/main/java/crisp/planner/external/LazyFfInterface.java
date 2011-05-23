package crisp.planner.external;


import crisp.converter.FastCRISPConverter;
import java.io.File;
import java.io.FileReader;


import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.OutputCodec;


import crisp.pddl.PddlParser;
import crisp.planner.lazyff.GoalStateCondition;
import crisp.planner.lazyff.HelpfulActionFinder;
import crisp.planner.lazyff.RelaxedGraphplanEvaluator;
import crisp.planner.lazyff.BestFirstSearch;
import crisp.planner.lazyff.Search;
import crisp.planner.lazyff.State;
import crisp.planningproblem.Action;
import crisp.planner.reachability.ReachabilityAnalyzer;
import crisp.planner.reachability.GroundPlanningProblem;
import crisp.planningproblem.codec.PddlOutputCodec;
import crisp.result.CrispDerivationTreeBuilder;
import crisp.result.DerivationTreeBuilder;

import de.saar.penguin.tag.codec.PCrispXmlInputCodec;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;

import de.saar.chorus.term.Term;

import de.saar.penguin.tag.grammar.Grammar;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import java.util.List;
import java.util.Timer;

//import javax.swing.JFrame;
//import org.jgraph.JGraph;


public class LazyFfInterface implements PlannerInterface {


    private class SearchRunnable implements Runnable {

        State initState;
        State result;
        GoalStateCondition condition;
        Search search;

        public SearchRunnable(Search search, State initState, GoalStateCondition condition) {
            this.initState = initState;
            this.condition = condition;
            this.search = search;
        }
      
        public void run() {
            result = search.search(initState, condition);
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("Thread interrupted.");
            }
            System.out.println("Ending thread");

        }

        public State getResult(){
            return result;
        }
    }

    
    private long preprocessingTime;
    private long searchTime;
    private long totalTime;
    private boolean timedOut;

    public LazyFfInterface() {

        preprocessingTime = 0;
        searchTime = 0;
        timedOut = false;
    }

    public boolean isTimedOut(){
        return timedOut;
    }

    /**
     * Run the planner with default timeout.
     * @param domain The planning domain/operators for the problem
     * @param problem The planning problem
     * @return a list of ground (compound) Terms describing instantiated plan actions
     */
    public List<Term> runPlanner(Domain domain, Problem problem) throws Exception {
        return runPlanner(domain, problem, 0);
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
        //OutputCodec outputCodec2 = new CostPddlOutputCodec();

        StringWriter domainWriter = new StringWriter();
        StringWriter problemWriter = new StringWriter();
        outputCodec.writeToDisk(domain, problem, new PrintWriter(domainWriter),
                                                 new PrintWriter(problemWriter));


        System.out.println(domain.getActions().size() + " operators in domain.");
        //outputCodec2.writeToDisk(domain, problem, new PrintWriter(new FileWriter(new File("tmpdomain.lisp"))),
        //                                         new PrintWriter(new FileWriter(new File("tmpproblem.lisp"))));

        outputCodec = null;

        // Run the planner
        System.out.println("Reachability analysis...");
        start = System.currentTimeMillis();

        StringReader domainReader = new StringReader(domainWriter.toString());
        StringReader problemReader = new StringReader(problemWriter.toString());
        PddlParser.parse(domainReader, domain, problemReader, problem);
        

        ReachabilityAnalyzer analyzer = new ReachabilityAnalyzer(domain);
        GroundPlanningProblem gpp = analyzer.analyzeReachability(problem);

        end = System.currentTimeMillis();

        preprocessingTime = end-start;        
        System.out.println(preprocessingTime + "ms.");
        System.out.println("Search...");

        RelaxedGraphplanEvaluator eval = new RelaxedGraphplanEvaluator(gpp);
        Search search = new BestFirstSearch(gpp, new HelpfulActionFinder(eval), eval);

        //StateEvaluator eval = new SimpleCostEvaluator(gpp);
        //Search search = new GreedySearch(gpp, new ActionFinder(), eval);        
        start = System.currentTimeMillis();
        this.timedOut = false;
        State result;
        SearchRunnable sr = new SearchRunnable(search, new State(gpp), new GoalStateCondition(gpp));
        Thread searchThread = new Thread(sr);                    

        System.out.println("will time out in "+timeout+"ms.");
        Timer timer = new Timer();
        timer.schedule(new InterruptScheduler(Thread.currentThread()), timeout);  
        try{
            searchThread.start();
            searchThread.join();            
            result = sr.getResult();
            searchThread.interrupt();
        } catch (InterruptedException e) {
            timer.cancel();
            System.out.println("Timed out.");
            searchThread.interrupt();
            this.timedOut = true;
            return null;
        } finally {
            timer.cancel();
            searchThread.interrupt();
            end = System.currentTimeMillis();
            searchTime = end-start;
            System.out.println(search + "ms.");
            totalTime = searchTime + preprocessingTime;
            System.out.println("Total: " + totalTime + "ms.");
        }
        timer.cancel();
        

        
        
        

        List<Term> resultAsTerm = new ArrayList<Term>();

        if (result==null) {
            return null;
        }
        for (Action a : result.getPlanToHere()) {            
            resultAsTerm.add(a.getPredicate());
        }

        return resultAsTerm;
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
	Grammar<Term> grammar = new Grammar<Term>();
	codec.parse(new FileReader(new File(args[0])), grammar);


        File problemfile = new File(args[1]);

        System.out.println("Generating planning problem...");
        new FastCRISPConverter().convert(grammar, new FileReader(problemfile), domain, problem);

        long end = System.currentTimeMillis();

	System.out.println("Total runtime for problem generation: " + (end-start) + "ms");


        System.out.println("Running planner ... ");
        LazyFfInterface planner = new LazyFfInterface();
        List<Term> plan = planner.runPlanner(domain,problem, 600000);

        System.out.println(planner.getTotalTime());
        System.out.println(planner.getPreprocessingTime());
        System.out.println(planner.getSearchTime());
        if (plan == null) {
            System.err.println("No plan found!");
            System.exit(0);
         }
        for (Term instance : plan ){
            System.out.println(instance);
        }
        DerivationTreeBuilder derivationTreeBuilder = new CrispDerivationTreeBuilder(grammar);
        DerivationTree derivTree = derivationTreeBuilder.buildDerivationTreeFromPlan(plan, domain);
        System.out.println(derivTree);
        DerivedTree derivedTree = derivTree.computeDerivedTree(grammar);
        System.out.println(derivedTree);
        System.out.println(derivedTree.yield());

    }


}
