/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package crisp.planningproblem.codec;

import crisp.planningproblem.Action;
import crisp.planningproblem.Domain;
import crisp.planningproblem.DurativeAction;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.TypeHierarchy;
import de.saar.basic.StringTools;
import java.io.PrintWriter;

/**
 *
 * @author dbauer
 */
public class TempPddlOutputCodec extends PddlOutputCodec{

     @Override
    public void writeDomain(Domain domain, PrintWriter dw) {
        dw.println("(define (domain " + domain.getName() + ")");
        dw.println("        (:requirements " + StringTools.join(domain.getRequirements(), " ") + " :durative-actions )");
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
        for( Predicate pred : domain.getPredicates() ) {
            if( !pred.getLabel().equals("**equals**")) {
                dw.println("         (" + pred.getLabel() + " " + pred.getVariables().toLispString() + ")");
            }
        }
        dw.println("        )");
        
        for( Action a : domain.getActions() ) {
            dw.println("\n" + toPddlString(a));
        }

        dw.println(")");
        dw.flush(); //otherwise output is incomplete
    }

    @Override
    protected String toPddlString(Action action) {
        StringBuffer buf = new StringBuffer();
        String prefix = "      ";

        boolean isDurativeAction = (action instanceof DurativeAction);

        if (isDurativeAction) {
            buf.append("   (:durative-action ");            
        } else {
            buf.append("   (:action ");
        }
        buf.append(action.getPredicate().getLabel() + "\n");
        buf.append(prefix + ":parameters (" + action.getPredicate().getVariables().toLispString() + ")\n");
        buf.append(prefix + ":precondition " + toPddlString(action.getPrecondition()) + "\n");
        buf.append(prefix + ":effect " + toPddlString(action.getEffect()) + "\n");
        if (isDurativeAction){
            buf.append(prefix + ":duration ");
            Double duration = ((DurativeAction) action).getDuration();
            String durationString = String.format("%f", duration);
            buf.append(durationString);            
        }

        buf.append(") \n");

        return buf.toString();
    }

}
