package crisp.converter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.CostPddlOutputCodec;
//import crisp.planningproblem.codec.PddlOutputCodec;

import crisp.planningproblem.codec.PddlOutputCodec;
import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.codec.PCrispXmlInputCodec;

import de.saar.chorus.term.Term; 

import de.saar.penguin.tag.grammar.LinearInterpolationProbabilisticGrammar;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;


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

        if (args.length < 1) {
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
        LinearInterpolationProbabilisticGrammar<Term> grammar = new LinearInterpolationProbabilisticGrammar<Term>(0.9, 1, 1000);
        codec.parse(new File(args[0]), grammar);
        grammar.initBackoff();

        File problemfile = new File(args[1]);

        System.out.println("Generating planning problem...");
        new ProbCRISPConverter().convert(grammar, problemfile, domain, problem);
        //new FancyBackoffProbCRISPConverter().convert_backoff(grammar, new FileReader(problemfile), domain, problem);
        


        long end = System.currentTimeMillis();

        System.out.println("Total runtime: " + (end - start) + "ms");

        System.out.println("Domain: " + domain);
        System.out.println("Problem: " + problem);

        new PddlOutputCodec().writeToDisk(domain, problem, new PrintWriter(new FileWriter(new File(args[2]))),
                new PrintWriter(new FileWriter(new File(args[3]))));
    }



}