package crisp.converter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PddlOutputCodec;

import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.codec.CrispXmlInputCodec;

import de.saar.chorus.term.Term; 

import java.io.File;
import java.io.FileReader;


public class CRISPtoPDDL {
        
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
        
        CrispXmlInputCodec codec = new CrispXmlInputCodec();
		Grammar<Term> grammar = new Grammar<Term>();	
		codec.parse(new FileReader(new File(args[0])), grammar);         
 
        File problemfile = new File(args[1]);
        
		new FastCRISPConverter().convert(grammar, problemfile, domain, problem);
		long end = System.currentTimeMillis();

		System.out.println("Total runtime: " + (end-start) + "ms");

        System.out.println("Domain: " + domain );
		System.out.println("Problem: " + problem);

		new PddlOutputCodec().writeToDisk(domain, problem, "");
	}



}