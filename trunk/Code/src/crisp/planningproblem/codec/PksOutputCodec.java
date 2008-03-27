package crisp.planningproblem.codec;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import crisp.planningproblem.Action;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.effect.Effect;
import crisp.planningproblem.goal.Goal;
import de.saar.basic.StringTools;
import de.saar.chorus.term.Term;

public class PksOutputCodec extends OutputCodec {
    private static class PksReplacingWriter extends Writer {
        private final Writer original;

        protected PksReplacingWriter(Writer original) {
            super();
            this.original = original;
        }

        @Override
        public void close() throws IOException {
            original.close();
        }

        @Override
        public void flush() throws IOException {
            original.flush();
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            for( int i = off; i < off+len; i++ ) {
                if( cbuf[i] == '-' ) {
                    cbuf[i] = '_';
                }
            }

            original.write(cbuf, off, len);
        }

    }

    @Override
    public void writeToDisk(Domain domain, Problem problem, String filenamePrefix, String problemname) throws IOException {
        System.err.println("Writing PKS file ...");
        PrintWriter dw = new PrintWriter(new PksReplacingWriter(new FileWriter(filenamePrefix + problemname + ".pks")));

        dw.println("<?xml version='1.0'?>\n");
        dw.println("<pks>");

        printDomain(domain, dw);
        printProblem(problem, dw);

        dw.println("</pks>");

        dw.flush();
        dw.close();

        System.err.println("Done.");
    }

    private void printProblem(Problem problem, PrintWriter writer) {
        writer.println("  <problem name='" + problem.getName() + "' domain='" + problem.getDomain().getName() + "'>");

        writer.println("    <init>");
        for( Term t : problem.getInitialState() ) {
            writer.println("      add(Kf, " + t.toString() + ");");
        }
        writer.println();
        for( String indiv : problem.getDomain().getUniverse().keySet() ) {
            writer.println("      add(Kf, " + problem.getDomain().getUniverse().get(indiv) + "(" + indiv + "));");
        }
        writer.println("    </init>\n");

        writer.println("    <goal>");
        writer.print("      ");
        printAsPks(problem.getGoal(), writer);
        writer.println("\n    </goal>");
        writer.println("  </problem>");
    }

    private void printDomain(Domain domain, PrintWriter writer) {
        int i;

        writer.println("  <domain name='" + domain.getName() + "'>");

        writer.println("    <symbols>");
        writer.println("      <predicates>");
        for( Predicate pred : domain.getPredicates() ) {
            writer.println("        " + pred.getLabel() + "/" + pred.getVariables().size() + ",");
        }

        i = 0;
        for( String type : domain.getTypeHierarchy().getTypes() ) {
            if( !type.equals("object")) {
                writer.print("        " + type + "/1");
                if( i++ < domain.getTypeHierarchy().getTypes().size() - 2) { // ignore "object"
                    writer.print(",");
                }
                writer.println();
            }
        }

        writer.println("      </predicates>");

        writer.println("      <constants>");
        writer.println("        " + StringTools.join(domain.getUniverse().keySet(), ", "));
        writer.println("      </constants>");
        writer.println("    </symbols>");

        writer.println("    <actions>");
        for( Action a : domain.getActions() ) {
            writeAsPks(a, writer);
            writer.println();
        }
        writer.println("    </actions>");
        writer.println("  </domain>");
    }

    private void writeAsPks(Action action, PrintWriter writer) {
        int i;

        writer.println("      <action name='" + action.getPredicate().getLabel() + "'>");
        writer.println("        <params>"
                + StringTools.join(action.getPredicate().getVariables().getItems(), ",")
                + "</params>");

        writer.println("        <preconds>");
        writer.print("          ");
        printAsPks(action.getPrecondition(), writer);
        writer.println("\n        </preconds>");

        writer.println("        <effects>");
        writer.print("          ");
        printAsPks(action.getEffect(), writer);
        writer.println();
        writer.println("        </effects>");

        writer.println("      </action>");
    }

    private void printAsPks(Effect effect, PrintWriter writer) {
        if( effect instanceof crisp.planningproblem.effect.Conjunction ) {
            crisp.planningproblem.effect.Conjunction conj = (crisp.planningproblem.effect.Conjunction) effect;

            for( Effect conjunct : conj.getConjuncts() ) {
                printAsPks(conjunct, writer);
                writer.print(" ");
            }
        } else if( effect instanceof crisp.planningproblem.effect.Conditional ) {
            crisp.planningproblem.effect.Conditional cond = (crisp.planningproblem.effect.Conditional) effect;

            writer.print("(");
            printAsPks(cond.getCondition(), writer);
            writer.print(") => (");
            printAsPks(cond.getEffect(), writer);
            writer.print(")");
        } else if( effect instanceof crisp.planningproblem.effect.Universal ) {
            crisp.planningproblem.effect.Universal univ = (crisp.planningproblem.effect.Universal) effect;

            for( String var : univ.getVariables().getItems() ) {
                writer.print("forallK(" + var + ") K(" + univ.getVariables().getType(var) + "(" + var + ")) => (");
            }
            printAsPks(univ.getScope(), writer);
            for( String var : univ.getVariables().getItems() ) {
                writer.print(")");
            }
        } else if( effect instanceof crisp.planningproblem.effect.Literal ) {
            crisp.planningproblem.effect.Literal lit = (crisp.planningproblem.effect.Literal) effect;

            if( lit.getPolarity() ) {
                writer.print("add(Kf, " + lit.getAtom().toString() + ");");
            } else {
                writer.print("del(Kf, "+ lit.getAtom().toString() + ");");
            }
        }
    }

    private void printAsPks(Goal goal, PrintWriter writer) {
        if( goal instanceof crisp.planningproblem.goal.Conjunction ) {
            crisp.planningproblem.goal.Conjunction conj = (crisp.planningproblem.goal.Conjunction) goal;
            boolean first = true;

            for( Goal conjunct : conj.getConjuncts() ) {
                if( first ) {
                    first = false;
                } else {
                    writer.print(" ^ ");
                }
                printAsPks(conjunct, writer);
            }
        } else if( goal instanceof crisp.planningproblem.goal.Universal ) {
            crisp.planningproblem.goal.Universal univ = (crisp.planningproblem.goal.Universal) goal;

            for( String var : univ.getVariables().getItems() ) {
                writer.print("forallK(" + var + ") K(" + univ.getVariables().getType(var) + "(" + var + ")) => (");
            }
            printAsPks(univ.getScope(), writer);
            for( String var : univ.getVariables().getItems() ) {
                writer.print(")");
            }
        } else if( goal instanceof crisp.planningproblem.goal.Negation ) {
            crisp.planningproblem.goal.Negation neg = (crisp.planningproblem.goal.Negation) goal;
            writer.print("!");
            printAsPks(neg.getSubformula(), writer);
        } else if( goal instanceof crisp.planningproblem.goal.Literal ) {
            crisp.planningproblem.goal.Literal lit = (crisp.planningproblem.goal.Literal) goal;

            if( !lit.getPolarity() ) {
                writer.print("!");
            }
            writer.print("K(" + lit.getAtom().toString() + ")");
        }
    }

    /*
     *  <action name="NP-mary">
        <params>?u,?x</params>
        <preconds>
          K(open(?u,np,?x)) ^
      K(skb1(mary, ?x))
        </preconds>
        <effects>
          del(Kf, open(?u,np,?x));
        </effects>
      </action>
     */

}
