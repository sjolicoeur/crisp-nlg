package crisp.converter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PksOutputCodec;

public class CRISPtoPKS {

	/**
	 * Main program.  When running the converter from the command line, pass
	 * the name of the CRISP problem file as the first argument.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Domain domain = new Domain();
		Problem problem = new Problem();

		long start = System.currentTimeMillis();
		CRISPConverter.convert(args[0], domain, problem);
		long end = System.currentTimeMillis();

		System.err.println("Total runtime: " + (end-start) + "ms");

		System.out.println("Domain: " + domain);
		System.out.println("Problem: " + problem);

		new PksOutputCodec().writeToDisk(domain, problem, "");
	}

}