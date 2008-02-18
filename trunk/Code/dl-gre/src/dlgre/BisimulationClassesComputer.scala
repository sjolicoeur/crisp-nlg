package dlgre;

import scala.collection.mutable.Queue;
import scala.collection.mutable.Set;

import dlgre.formula._;

class BisimulationClassesComputer(graph:Graph) {
   // Computes the bisimulation classes of a graph.  The method returns a list of Subset objects
   // representing the classes.
   def compute = {
     val roles = graph.getAllRoles;
     
     // the current classes
     val queue = new Queue[Option[Formula]]
     
     // initialize with a single class that contains everything
     queue += Some(new dlgre.formula.Top());
     
     // split over all (positive) literals up to saturation
     splitOverLiterals(queue, graph.getAllPredicates);
     
     
     // now repeatedly split over roles to distinguished subsets
     var oldQueue : List[Formula] = Nil;
     var newQueue = extractQueue(queue);
     
     //println("\n\nBefore role splitting: " + newQueue);
     
     do {
       oldQueue = newQueue;
       splitOverRoles(queue, roles);
       newQueue = extractQueue(queue);
       
       //println("Queue is now: " + newQueue);
     } while( oldQueue != newQueue );

     newQueue;
   }
   
   // Splits classes that satisfy different positive literals.
   private def splitOverLiterals(queue: Queue[Option[Formula]], predicates: Collection[String]) = {
     predicates.foreach { p =>
       forallQueue(queue, { (formula, queue) =>
       	splitOverLiteral(formula, p, true) match {
           case Some((sub1,sub2)) => { 
             queue += Some(sub1);
             queue += Some(sub2);
           }
           
           case None => queue += Some(formula);
        }});
     }
   }
   
   private def splitOverLiteral(f:Formula, pred:String, polarity:Boolean) = {
     val ext = f.extension(graph);
     
     if( ext.size <= 1 ) {
       None
     } else {
       val sub1 = new dlgre.formula.Conjunction(List(f, new dlgre.formula.Literal(pred, polarity)));
       val sub2 = new dlgre.formula.Conjunction(List(f, new dlgre.formula.Literal(pred, !polarity)));
       
       if( ext != sub1.extension(graph)  &&  ext != sub2.extension(graph) ) {
         Some((sub1,sub2))
       } else {
         None
       }
     }
   }
   
   // Splits classes that have the same role pointing into different previously
   // existing classes (1 step). 
   def splitOverRoles(queue : Queue[Option[Formula]], roles : Set[String]) = {
     val elements = extractQueue(queue);
     val localQueue = new Queue[Option[Formula]];
     
     queue.clear;
     
     elements.foreach { el =>
        localQueue.clear;
        localQueue += Some(el);
        
        for( val role <- roles; val sub <- elements ) {
          	//println("[" + role + "/" + sub + "] ");
               forallQueue(localQueue, { (formula, q) =>
               splitOverRole1(formula, role, sub) match {
                 case Some((s1,s2)) => {
                  // println("  - split " + subset + " over " + role + " into " + s1 + " and " + s2);
                   q += Some(s1);
                   q += Some(s2);
                 }
                 
                 case None => q += Some(formula);
               }});
             }
        
        
        queue ++= localQueue;
     }
   }

   private def splitOverRole1(formula:Formula, role:String, roleTo:Formula) = {
     val ext = formula.extension(graph);
     
     if( ext.size > 1 &&
       ext.exists { x => roleTo.extension(graph).exists { y => graph.hasEdge(x,role,y)} } &&
       ext.exists { x => roleTo.extension(graph).forall { y => !graph.hasEdge(x,role,y)} } 
     ) {
       Some((new dlgre.formula.Conjunction(List(formula, new dlgre.formula.Existential(role, roleTo))),
             new dlgre.formula.Conjunction(List(formula, new dlgre.formula.Negation(new dlgre.formula.Existential(role, roleTo)))))
           )
     } else {
       None
     }
   }
   
   
   
   
   
   // Calls a function for every element in a queue.  In order to ensure that the queue
   // is only traversed once, a None element is appended to the queue before the iteration.
   // This element is removed afterwards.  Crucially, the function gets passed the entire queue
   // as an argument and is allowed to append new elements to the queue.
   def forallQueue(q : Queue[Option[Formula]], proc : (Formula,Queue[Option[Formula]]) => Unit) = {
     var finished = false;
     q += None;
     
     while(!finished) {
       val el = q.dequeue
       
       el match {
            case None => finished = true;
            case Some(subset) => proc(subset,q);
       }
     }
   }
   
   // Extracts the list of values from a queue that only contains Some elements.
   // The method throws an exception if the queue contains None elements.
   def extractQueue(queue : Queue[Option[Formula]]) = {
     (for( val x <- queue.elements if x.isInstanceOf[Some[Formula]] ) 
       yield (x match { 
         case Some(subset) => subset; 
         case None => throw new Exception("Extracting from queue with empty elements!") 
       })).toList
   }
   
}
