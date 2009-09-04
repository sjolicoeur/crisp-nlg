package crisp.converter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PksOutputCodec;

import de.saar.penguin.tag.codec.CrispXmlInputCodec;
import de.saar.penguin.tag.grammar.Grammar;
import de.saar.chorus.term.Term;

import java.io.FileReader;
import java.io.File;

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

                CrispXmlInputCodec codec = new CrispXmlInputCodec();
		Grammar<Term> grammar = new Grammar<Term>();
		codec.parse(new FileReader(new File(args[0])), grammar);

                File problemFile =  new File(args[1]);

		new FastCRISPConverter().convert(grammar, problemFile, domain, problem);
		long end = System.currentTimeMillis();

		System.err.println("Total runtime: " + (end-start) + "ms");

		System.out.println("Domain: " + domain);
		System.out.println("Problem: " + problem);

		new PksOutputCodec().writeToDisk(domain, problem, "");
	}

}