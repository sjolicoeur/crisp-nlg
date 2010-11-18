package crisp.problemgenerator;

import de.saar.penguin.tag.codec.CrispXmlInputCodec;
import de.saar.penguin.tag.codec.ParserException;
import de.saar.penguin.tag.grammar.CrispGrammar;
import de.saar.penguin.tag.grammar.CrispLexiconEntry;

import java.io.*;

public class ProblemGenerator {
    PrintWriter pout;
    PrintWriter gout;
    int numberOfSentences, arity, numberOfDistractors;
    String fileName, fne;
    CrispGrammar grammar;
    
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Wrong number of arguments.");
            usage();
            System.exit(1);
        }
        new ProblemGenerator(Integer.valueOf(args[0]), Integer.valueOf(args[1]), Integer.valueOf(args[2]), args[3])
                .generate();
    }

    public static void usage() {
        System.out.println("ProblemGenerator [number of sentences] [valence] [number of distractors] [file name]");
    }

    public ProblemGenerator(int numOfSentences, int valenceNum, int numDistractors, String fileNamePrefix)
            throws IOException, ParserException {
        this.numberOfSentences = numOfSentences;
        this.arity = valenceNum;
        this.numberOfDistractors = numDistractors;
        this.fileName = fileNamePrefix;
        fne = numberOfSentences + "-" + arity + "-" + numberOfDistractors + ".xml";

        FileWriter fr=new FileWriter(fileName + "-problem-" + fne);
        BufferedWriter br = new BufferedWriter(fr);
        pout = new PrintWriter(br);
        grammar = new CrispGrammar();
        CrispXmlInputCodec codec = new CrispXmlInputCodec();
        codec.parse(new FileReader(new File("src/main/resources/base-grammar.xml")) , grammar);
        FileWriter gfr = new FileWriter(fileName + "-grammar-" + fne);
        BufferedWriter gbr = new BufferedWriter(gfr);
        gout = new PrintWriter(gbr);
        FileInputStream fstream = new FileInputStream("src/main/resources/base-grammar.xml");
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader brr = new BufferedReader(new InputStreamReader(in));
        String line;

        while (!(line = brr.readLine()).equals("</crisp-grammar>"))
            gout.write(line+"\n");
        gout.flush();
        in.close();
    }

    public void generate() throws IOException {
        printHeader();
        int current = 1;
        for (int sentence = 1; sentence <= numberOfSentences; sentence++, current++) {

            for (int argument = 1; argument <= arity; argument++, current++) {
                printWorld("the-1(a" +sentence + "_" + argument + ")");
                printCommGoal("the-1(a" +sentence + "_" + argument + ")");
                printWorld("businessman-1" + current + "(a" +sentence + "_" + argument + ")");
                if (current > 1)
                    addLexicalEntry("businessman", current);
                printCommGoal("businessman-1" + current + "(a" +sentence + "_" + argument + ")");
                for (int distractor = 1; distractor <= numberOfDistractors; distractor++) {
                    printWorld("rich-1" + distractor + "(a" +sentence + "_" + argument + ")");
                    printWorld("businessman-1" + current + "(a" +sentence + "_" + argument + "_dist" +
                            distractor + ")");
                    if (current == 1 && distractor > 1)
                        addLexicalEntry("rich", distractor);
                    printWorld("the-1(a" +sentence + "_" + argument + "_dist" + distractor + ")");
                    for (int i = 1; i <= numberOfDistractors; i++)
                        if (i != distractor) {
                            printWorld("rich-1" + i + "(a" +sentence + "_" + argument + "_dist" + distractor + ")");
                        }
                }
            }
            if (arity == 1) {
                printWorld("sneeze-2(e" + sentence + ",a" + sentence + "_1)");
                printCommGoal("sneeze-2(e" + sentence + ",a" + sentence + "_1)");
            }
            else if (arity == 2) {
                printWorld("admire-3(e" + sentence + ",a" + sentence + "_1" + ",a" + sentence + "_2)");
                printCommGoal("admire-3(e" + sentence + ",a" + sentence + "_1" + ",a" + sentence + "_2)");
            }
            else if (arity == 3) {
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
        pout.write("<world>" + str +"</world>\n");
    }

    private void printCommGoal(String str) {
        pout.write("<commgoal>" + str + "</commgoal>\n");
    }

    private void printHeader() {
        pout.write("<crispproblem name='" + fileName + "-" + numberOfSentences + "-" + arity+ "-" + numberOfDistractors
                + "' grammar='" + fileName + "-grammar-"+ numberOfSentences + "-" + arity+ "-" + numberOfDistractors  +".xml'" +
                " cat='S' index='e1' syntaxnodes='" + numberOfSentences*(arity+2) + "' referents='0'>\n");
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

            semantics = lex.semantics.get(0).toString().replace("1(" , num+"(");
            gout.write("<semcontent>" + semantics + "</semcontent>\n</tree>\n");
        }
        gout.write("</entry>\n");
    }
}