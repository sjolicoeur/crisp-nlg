package crisp.problemgenerator;

import java.io.*;

public class ProblemGenerator {
    BufferedWriter br;
    PrintWriter out;
    int senNum, valence, disNum;
    public static void main(String[] args) throws Exception {
//        CrispGrammar grammar = new CrispGrammar();
//        CrispXmlInputCodec codec = new CrispXmlInputCodec();
        //TODO: generate the grammar at the same time.

        if (args.length != 3) {
            System.err.println("Wrong number of arguments.");
            usage();
            System.exit(1);
        }
        new ProblemGenerator(Integer.valueOf(args[0]), Integer.valueOf(args[1]), Integer.valueOf(args[2])).generate();

    }
    public static void usage() {
        System.out.println("ProblemGenerator [number of sentences] [valence] [number of distractors] ");
    }

    public ProblemGenerator(int senNum, int val, int disNum) throws IOException {
        FileWriter fr=new FileWriter("xtag-problem-" + senNum + "_" + val + "_" + disNum + ".xml");
        br = new BufferedWriter(fr);
		out = new PrintWriter(br);
        this.senNum = senNum;
        this.valence = val;
        this.disNum = disNum;
    }

    public void generate(){
        printHeader();
        for (int sentence = 1; sentence <= senNum; sentence++) {

            for (int argument = 1; argument <= valence; argument++){
                printWorld("the-1(a" +sentence + "_" + argument + ")");
                printCommGoal("the-1(a" +sentence + "_" + argument + ")");
                printWorld("businessman-" + sentence + argument + "(a" +sentence + "_" + argument + ")");
                printCommGoal("businessman-" + sentence + argument + "(a" +sentence + "_" + argument + ")");
                for (int distractor = 1; distractor <= disNum; distractor++){
                    printWorld("rich-1" + distractor + "(a" +sentence + "_" + argument + ")");
                    printWorld("businessman-" + sentence + argument + "(a" +sentence + "_" + argument + "_dist" +
                            distractor + ")");
                    printWorld("the-1(a" +sentence + "_" + argument + "_dist" + distractor + ")");
                    for (int i = 1; i <= disNum; i++)
                        if (i != distractor) {
                            printWorld("rich-1" + i + "(a" +sentence + "_" + argument + "_dist" + distractor + ")");
                        }
                }
            }
            if (valence == 1) {
                printWorld("sneeze-2(e" + sentence + ",a" + sentence + "_1)");
                printCommGoal("sneeze-2(e" + sentence + ",a" + sentence + "_1)");
            }
            else if (valence == 2) {
                printWorld("admire-3(e" + sentence + ",a" + sentence + "_1" + ",a" + sentence + "_2)");
                printCommGoal("admire-3(e" + sentence + ",a" + sentence + "_1" + ",a" + sentence + "_2)");
            }
            else if (valence == 3) {
                printWorld("give-4(e" + sentence + ",a" + sentence + "_1" + ",a" + sentence + "_2" + ",a"
                        + sentence + "_3)");
                printCommGoal("give-4(e" + sentence + ",a" + sentence + "_1" + ",a" + sentence + "_2" + ",a"
                        + sentence + "_4)");
            }
        }
        printFooter();
        out.flush();
    }
    private void printWorld(String str){
        out.write("<world>" + str +"</world>");
    }
    private void printCommGoal(String str){
        out.write("<commgoal>" + str + "</commgoal>");
    }
    private void printHeader(){
        out.write("<?xml version='1.0' encoding='UTF-8'?><crispproblem name='xtag-" +
                senNum + "-" + valence+ "-" + disNum + "' grammar='' cat='S' index='e1' plansize='5'>");
        //TODO: find out the relation between the input arguments and the plansize
    }
    private void printFooter(){
        out.write("</crispproblem>");
    }
}