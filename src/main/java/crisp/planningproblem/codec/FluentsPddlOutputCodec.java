/**
 * 
 */
package crisp.planningproblem.codec;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import crisp.planningproblem.Action;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.TypeHierarchy;
import crisp.planningproblem.formula.Conditional;
import crisp.planningproblem.formula.Conjunction;
import crisp.planningproblem.formula.Formula;
import crisp.planningproblem.formula.Literal;
import crisp.planningproblem.formula.Negation;
import crisp.planningproblem.formula.Universal;
import de.saar.basic.StringTools;
import de.saar.chorus.term.Term;

/**
 * @author garoufi
 *
 */
public class FluentsPddlOutputCodec extends PddlOutputCodec {
    
    @Override
    public void writeProblem(Problem problem, PrintWriter pw) {
        String name = problem.getName();

        pw.println("(define (problem " + name + ")");
        pw.println("   (:domain " + problem.getDomain().getName() + ")");

        // Write objects
        Map<String, String> objects = problem.getUniverse();
        if (!objects.isEmpty()) {
            pw.println("    (:objects");
            for (String object : objects.keySet()) {
                pw.println("        " + object + " - " + objects.get(object));
            }            
            pw.println("    )");
        }

        // Write initial state
        pw.println("   (:init");
        for (Term term : problem.getInitialState()) {
            pw.println("      " + term.toLispString());
        }
        
        // Include initial value of total cost
        pw.println("      (= (total-cost) 0)");	
        pw.println("   )\n");


        // Write goal condition
        pw.println("   (:goal " + toPddlString(problem.getGoal()) + ")\n");
        
        // Write optimization metric
        pw.println("   (:metric minimize (total-cost))");
        pw.println(")");
        pw.flush(); //otherwise output is incomplete 
        pw.close();
    }


    @Override
    public void writeDomain(Domain domain, PrintWriter dw) {
        dw.println("(define (domain " + domain.getName() + ")");
        dw.println("        (:requirements " + StringTools.join(domain.getRequirements(), " ") + " :fluents )");
        dw.print("        (:types");
        for( String type : domain.getTypeHierarchy().getTypes() ) {
            if( !type.equals(TypeHierarchy.TOP) ) {
                dw.print("  " + type);
                if( ! domain.getTypeHierarchy().getDirectSupertype(type).equals(TypeHierarchy.TOP) ) {
                    dw.print(" - " + domain.getTypeHierarchy().getDirectSupertype(type));
                }
            }
        }
        dw.println(")");

        dw.println("       (:constants");
        for( String constant : domain.getUniverse().keySet()) {
            dw.println("         " + constant + " - " + domain.getUniverse().get(constant));
        }
        dw.println("       )");

        dw.println("       (:predicates");

        for (String pred : domain.getPredicates()) {
            if (!pred.equals("**equals**")) {
                dw.println("         (" + pred + " " + makeLispString(domain.getSignature(pred)) + ")");
            }
        }
        dw.println("        )");

        dw.println("        (:functions (total-cost))\n");

        for( Action a : domain.getActions() ) {
            dw.println("\n" + toPddlString(a));
        }

        dw.println(")");
        dw.flush(); //otherwise output is incomplete
        dw.close();
    }


    protected String toPddlString(Action action) {
        StringBuffer buf = new StringBuffer();
        String prefix = "      ";

        buf.append("   (:action " + action.getPredicate().getLabel() + "\n");
        buf.append(prefix + ":parameters (" + makeLispString(action.getPredicate().getSubterms(), action.getParameterTypes()) + ")\n");
        buf.append(prefix + ":precondition " + toPddlString(action.getPrecondition()) + "\n");
        buf.append(prefix + ":effect " + toPddlStringGoal(action.getEffect()) + "\n");

            buf.append("(increase (total-cost) ");
            Double duration = action.getCost();
            String durationString = String.format("%f",duration);

            buf.append(durationString);
            buf.append(")");


        buf.append(")\n       )\n");

        return buf.toString();
    }

    private String toPddlStringGoal(Formula goal) {
        if (goal instanceof Conjunction) {
            Conjunction conj = (Conjunction) goal;
            StringBuffer buf = new StringBuffer("(and");

            for (Formula conjunct : conj.getConjuncts()) {
                buf.append(" " + toPddlString(conjunct));
            }


            return buf.toString();
        } else if (goal instanceof Universal) {
            Universal univ = (Universal) goal;
            return "(forall (" + makeLispString(univ.getVariables(), univ.getVariableTypes()) + ") " + toPddlString(univ.getScope()) + ")";
        } else if (goal instanceof Conditional) {
            Conditional cond = (Conditional) goal;
            return "(when " + toPddlString(cond.getCondition()) + " " + toPddlString(cond.getEffect()) + ")";

        } else if (goal instanceof Negation) {
            Negation neg = (Negation) goal;
            return "(not " + toPddlString(neg.getSubformula()) + ")";
        } else if (goal instanceof Literal) {
            Literal lit = (Literal) goal;
            return (lit.getPolarity() ? "" : "(not ") + lit.getAtom().toLispString().replace("**equals**", "=") + (lit.getPolarity() ? "" : ")");
        } else {
            return null;
        }
    }


    private String toPddlString(Formula goal) {
        if (goal instanceof Conjunction) {
            Conjunction conj = (Conjunction) goal;
            StringBuffer buf = new StringBuffer("(and");

            for (Formula conjunct : conj.getConjuncts()) {
                buf.append(" " + toPddlString(conjunct));
            }

            buf.append(")");

            return buf.toString();
        } else if (goal instanceof Universal) {
            Universal univ = (Universal) goal;
            return "(forall (" + makeLispString(univ.getVariables(), univ.getVariableTypes()) + ") " + toPddlString(univ.getScope()) + ")";
        } else if (goal instanceof Conditional) {
            Conditional cond = (Conditional) goal;
            return "(when " + toPddlString(cond.getCondition()) + " " + toPddlString(cond.getEffect()) + ")";

        } else if (goal instanceof Negation) {
            Negation neg = (Negation) goal;
            return "(not " + toPddlString(neg.getSubformula()) + ")";
        } else if (goal instanceof Literal) {
            Literal lit = (Literal) goal;
            return (lit.getPolarity() ? "" : "(not ") + lit.getAtom().toLispString().replace("**equals**", "=") + (lit.getPolarity() ? "" : ")");
        } else {
            return null;
        }
    }

    private String makeLispString(List<Term> subterms, List<String> parameterTypes) {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < subterms.size(); i++) {
            buf.append(subterms.get(i) + " - " + parameterTypes.get(i) + "  ");
        }

        return buf.toString();
    }


    private String makeLispString(List<String> argumentTypes) {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < argumentTypes.size(); i++) {
            buf.append("?x" + (i + 1) + " - " + argumentTypes.get(i) + "  ");
        }

        return buf.toString();

    }



}