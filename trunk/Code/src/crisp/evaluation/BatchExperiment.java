package crisp.evaluation;

import crisp.planningproblem.Problem;
import crisp.planningproblem.Domain;

import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.codec.PCrispXmlInputCodec;

import de.saar.chorus.term.Term;

import crisp.converter.ProblemConverter;
import crisp.converter.ProbCRISPConverter;

import java.util.Set;
import java.util.Queue;
import java.util.LinkedList;

import java.io.StringReader;
import java.io.StringWriter;    
import java.io.File;

/**
 * Provides basic functionality for large scale experiments with the generation system. 
 */
public class BatchExperiment {         

    private static final Integer PROBLEM_SIZE = 40;
    private static final String GRAMMAR_NAME = "dummy";

    
    PlannerInterface planner; 
    Grammar grammar; 
    ProblemConverter converter; 
    DatabaseInterface database;
    String resultTable;
    Queue<Integer> batch;
    
    public BatchExperiment(PlannerInterface planner, Grammar grammar, ProblemConverter converter,
        DatabaseInterface database, String resultTable) {
        this.planner = planner; 
        this.grammar = grammar;
        this.converter = converter;
        this.database = database;
        this.resultTable = resultTable;
        this.batch = new LinkedList<Integer>();       
    }
    
    public void addToBatch(int sentenceID){
        batch.offer(sentenceID);
    }
    
    public void runExperiment(){
        while (! batch.isEmpty()) {                        
                processNextSentence();            
        }
    }
    
    public void processNextSentence(){
        processSentence(batch.poll());
    }
    
    private void processSentence(int sentenceID) {
        System.out.println("Processing sentence #"+sentenceID);
        try{
            Set<Term> semantics = database.getSentenceSemantics(sentenceID);
            String rootIndex = database.getRootIndex(sentenceID);
        
            String xmlProblem = createXMLProblem(semantics, rootIndex, "problem-"+sentenceID, GRAMMAR_NAME, PROBLEM_SIZE);            
            
            Domain domain = new Domain();
            Problem problem = new Problem();
            System.out.print("  converting...");
            converter.convert(grammar, new StringReader(xmlProblem), domain, problem);
            System.out.println("done.");
            System.out.println("Starting planner... ");
            planner.runPlanner(domain, problem);
            // Write derivation, surface and time to result table
        } catch (Exception e) {
            System.out.println("FAIL!");
            System.err.println("Couldn't process sentence #"+sentenceID);
            System.err.println(e);
            // Write error tag to database            
        }
    }
          
    
    private String createXMLProblem(Set<Term> semantics, String rootIndex, String name, String grammar, Integer problemsize) {
        StringWriter writer = new StringWriter();
        
        writer.write("<crispproblem name=\"");
        writer.write(name);
        writer.write("\" grammar=\"");
        writer.write(grammar);
        writer.write("\" cat=\"S\" index=\"");
        writer.write(rootIndex);
        writer.write("\" plansize=\"");
        writer.write(problemsize.toString());
        writer.write("\">\n");

        // Everything in the KB becomes communicative goal
        for (Term t : semantics) {
            writer.write("<world>");
            writer.write(t.toString());
            writer.write("</world>\n");
            writer.write("<commgoal>");
            writer.write(t.toString());
            writer.write("</commgoal>\n");                        
        }
        
        writer.write("</crispproblem>\n");
        return writer.toString();        
    }
    
    
    public static void main(String[] args) throws Exception{
        
        System.out.print("Parsing gramar...");
        PCrispXmlInputCodec codec = new PCrispXmlInputCodec();
		ProbabilisticGrammar<Term> grammar = new ProbabilisticGrammar<Term>();	
		codec.parse(new File(args[0]), grammar);
        System.out.println("done");
                
        System.out.print("Initializing experiment 1...");       
        BatchExperiment exp1 = new BatchExperiment(new LamaPlannerInterface(), 
                                                   grammar,                                                   
                                                   new ProbCRISPConverter(),
                                                   new MySQLInterface("jdbc:mysql://forbin/penguin" ,"penguin_rw","xohD9xei"),
                                                   "pcrisp_results1"
                                                   );
        exp1.addToBatch(1);        
        exp1.addToBatch(42);
        System.out.println("Done.");
        System.out.println("Running experiment...");
        exp1.runExperiment();
    }
    
}
