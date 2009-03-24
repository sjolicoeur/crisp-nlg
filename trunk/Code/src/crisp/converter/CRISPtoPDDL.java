package crisp.converter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PddlOutputCodec;


public class CRISPtoPDDL {
        
    /**
     * Print a usage message
     */
    public static void usage(){
        System.out.println("Usage: java crisp.converter.CRISPtoPPDL [crisp problem XML file]"); 
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
        
        
		Domain domain = new Domain();
		Problem problem = new Problem();

		long start = System.currentTimeMillis();
		FastCRISPConverter.convert(args[0], domain, problem);
		long end = System.currentTimeMillis();

		System.out.println("Total runtime: " + (end-start) + "ms");

        System.out.println("Domain: " + domain );
		System.out.println("Problem: " + problem);

		new PddlOutputCodec().writeToDisk(domain, problem, "");
	}



}