package crisp.problemgenerator;

import crisp.converter.CurrentNextCrispConverter;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PddlOutputCodec;
import de.saar.penguin.tag.codec.CrispXmlInputCodec;
import de.saar.penguin.tag.codec.ParserException;
import de.saar.penguin.tag.grammar.CrispGrammar;
import de.saar.penguin.tag.grammar.CrispLexiconEntry;
import de.saar.penguin.tag.grammar.SituatedCrispXmlInputCodec;

import java.io.*;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class ProblemGenerator {

    PrintWriter pout;
    PrintWriter gout;
    int numberOfSentences, arity, numberOfDistractors;
    String fileName, fne;
    CrispGrammar grammar;

    public static void main(String[] args) throws Exception {
        boolean generatePddl = false;

        if (args.length < 4) {
            System.err.println("Wrong number of arguments.");
            usage();
            System.exit(1);
        }

        int i = 0;

        if (args[i].equals("--pddl")) {
            generatePddl = true;
            i++;
        }

        ProblemGenerator generator = new ProblemGenerator(Integer.valueOf(args[i]), Integer.valueOf(args[i + 1]), Integer.valueOf(args[i + 2]), args[i + 3]);
        if (generatePddl) {
            generator.generatePddlProblem();
        } else {
            generator.generateCrispProblem();
        }
    }

    public static void usage() {
        System.out.println("ProblemGenerator [options] <number of sentences> <verb arity> <number of distractors> <output filename prefix>");
        System.out.println("Options:");
        System.out.println("   --pddl     generate PDDL files instead of a generation problem instance");
    }

    public ProblemGenerator(int numOfSentences, int valenceNum, int numDistractors, String fileNamePrefix) throws IOException, ParserException {
        this.numberOfSentences = numOfSentences;
        this.arity = valenceNum;
        this.numberOfDistractors = numDistractors;
        this.fileName = fileNamePrefix;
        fne = numberOfSentences + "-" + arity + "-" + numberOfDistractors + ".xml";

    }

    private static Reader getBaseGrammarReader() {
        return new InputStreamReader(ProblemGenerator.class.getResourceAsStream("/base-grammar.xml"));
    }

    public void generateCrispProblem() throws IOException, ParserException {
        gout = new PrintWriter(new FileWriter(fileName + "-grammar-" + fne));
        pout = new PrintWriter(new FileWriter(fileName + "-problem-" + fne));
        generateProblem();
    }

    public void generatePddlProblem() throws IOException, ParserException, ParserConfigurationException, SAXException {
        StringWriter crispGrammarWriter = new StringWriter();
        StringWriter crispProblemWriter = new StringWriter();

        gout = new PrintWriter(crispGrammarWriter);
        pout = new PrintWriter(crispProblemWriter);
        generateProblem();

        CurrentNextCrispConverter converter = new CurrentNextCrispConverter();
        SituatedCrispXmlInputCodec codec = new SituatedCrispXmlInputCodec();
        CrispGrammar fullGrammar = new CrispGrammar();
        Domain domain = new Domain();
        Problem problem = new Problem();

        codec.parse(new StringReader(crispGrammarWriter.toString()), fullGrammar);
        converter.convert(fullGrammar, new StringReader(crispProblemWriter.toString()), domain, problem);

        PrintWriter domainWriter = new PrintWriter(new FileWriter(fileName + "-domain-" + fne + ".lisp"));
        PrintWriter problemWriter = new PrintWriter(new FileWriter(fileName + "-problem-" + fne + ".lisp"));
        new PddlOutputCodec().writeToDisk(domain, problem, domainWriter, problemWriter);

    }

    private void generateProblem() throws IOException, ParserException {
        grammar = new CrispGrammar();

        CrispXmlInputCodec codec = new CrispXmlInputCodec();
        codec.parse(getBaseGrammarReader(), grammar);


        BufferedReader brr = new BufferedReader(getBaseGrammarReader());
        String line;

        while (!(line = brr.readLine()).equals("</crisp-grammar>")) {
            gout.println(line);
        }

        gout.flush();

        printHeader();
        int current = 1;
        for (int sentence = 1; sentence <= numberOfSentences; sentence++, current++) {

            for (int argument = 1; argument <= arity; argument++, current++) {
                printWorld("the-1(a" + sentence + "_" + argument + ")");
                printCommGoal("the-1(a" + sentence + "_" + argument + ")");
                printWorld("businessman-1" + current + "(a" + sentence + "_" + argument + ")");
                if (current > 1) {
                    addLexicalEntry("businessman", current);
                }
                printCommGoal("businessman-1" + current + "(a" + sentence + "_" + argument + ")");
                for (int distractor = 1; distractor <= numberOfDistractors; distractor++) {
                    printWorld("rich-1" + distractor + "(a" + sentence + "_" + argument + ")");
                    printWorld("businessman-1" + current + "(a" + sentence + "_" + argument + "_dist"
                            + distractor + ")");
                    if (current == 1 && distractor > 1) {
                        addLexicalEntry("rich", distractor);
                    }
                    printWorld("the-1(a" + sentence + "_" + argument + "_dist" + distractor + ")");
                    for (int i = 1; i <= numberOfDistractors; i++) {
                        if (i != distractor) {
                            printWorld("rich-1" + i + "(a" + sentence + "_" + argument + "_dist" + distractor + ")");
                        }
                    }
                }
            }
            if (arity == 1) {
                printWorld("sneeze-2(e" + sentence + ",a" + sentence + "_1)");
                printCommGoal("sneeze-2(e" + sentence + ",a" + sentence + "_1)");
            } else if (arity == 2) {
                printWorld("admire-3(e" + sentence + ",a" + sentence + "_1" + ",a" + sentence + "_2)");
                printCommGoal("admire-3(e" + sentence + ",a" + sentence + "_1" + ",a" + sentence + "_2)");
            } else if (arity == 3) {
                printWorld("give-4(e" + sentence + ",a" + sentence + "_1" + ",a" + sentence + "_2" + ",a"
                        + sentence + "_3)");
                printCommGoal("give-4(e" + sentence + ",a" + sentence + "_1" + ",a" + sentence + "_2" + ",a"
                        + sentence + "_4)");
            }
        }
        printFooter();
        pout.flush();
        gout.flush();
    }

    private void printWorld(String str) {
        pout.write("<world>" + str + "</world>\n");
    }

    private void printCommGoal(String str) {
        pout.write("<commgoal>" + str + "</commgoal>\n");
    }

    private void printHeader() {
        pout.write("<crispproblem name='" + fileName + "-" + numberOfSentences + "-" + arity + "-" + numberOfDistractors
                + "' grammar='" + fileName + "-grammar-" + numberOfSentences + "-" + arity + "-" + numberOfDistractors + ".xml'"
                + " cat='S' index='e1' syntaxnodes='" + numberOfSentences * (arity + 2) + "' referents='0'>\n");
    }

    private void printFooter() {
        pout.write("</crispproblem>");
        gout.write("</crisp-grammar>");
    }

    private void addLexicalEntry(String word, int num) throws IOException {
        gout.write("<entry word='" + word + num + "'>\n");
        String semantics;
        for (CrispLexiconEntry lex : grammar.getCrispLexiconEntries(word + "1")) {
            gout.write("<tree refid='" + lex.tree + "'>\n");

            semantics = lex.semantics.get(0).toString().replace("1(", num + "(");
            gout.write("<semcontent>" + semantics + "</semcontent>\n</tree>\n");
        }
        gout.write("</entry>\n");
    }
}
