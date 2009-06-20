package crisp.converter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.CostPddlOutputCodec;

import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.codec.PCrispXmlInputCodec;

import de.saar.chorus.term.Term; 

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


public class CRISPtoCostPDDL {
        
    /**
     * Print a usage message
     */
    public static void usage(){
        System.out.println("Usage: java crisp.converter.CRISPtoPPDL [CRISP grammar] [CRISP problem]"); 
    }
    
	/**
	 * Main program.  When running the converter from the command line, pass
	 * the name of the CRISP problem file as the first argument.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
        
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

		System.out.println("Total runtime: " + (end-start) + "ms");

        System.out.println("Domain: " + domain );
		System.out.println("Problem: " + problem);

		new CostPddlOutputCodec().writeToDisk(domain, problem, new FileWriter(new File(args[2])), new FileWriter(new File(args[3])));
	}



}