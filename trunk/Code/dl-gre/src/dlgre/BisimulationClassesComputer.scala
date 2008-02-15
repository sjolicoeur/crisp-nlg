package dlgre;

import scala.collection.mutable.Queue;
import scala.collection.mutable.Set;

import dlgre.formula._;

object BisimulationClassesComputer {
   // Computes the bisimulation classes of a graph.  The method returns a list of Subset objects
   // representing the classes.
   def compute(graph:Graph) = {
     val roles = graph.getAllRoles;
     
     // the current classes
     val queue = new Queue[Option[Subset]]
     
     // initialize with a single class that contains everything
     queue += Some(new Subset(Top(), graph, new dlgre.formula.Simplifier(graph)));
     
     // split over all (positive) literals up to saturation
     splitOverLiterals(queue, graph.getAllPredicates);
     
     
     // now repeatedly split over roles to distinguished subsets
     var oldQueue : List[Subset] = Nil;
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
   def splitOverLiterals(queue: Queue[Option[Subset]], predicates: Collection[String]) = {
     predicates.foreach { p =>
       forallQueue(queue, { (subset, queue) =>
       	subset.splitOverLiteral(p, true) match {
           case Some((sub1,sub2)) => { 
             queue += Some(sub1);
             queue += Some(sub2);
           }
           
           case None => queue += Some(subset);
        }});
     }
   }
   
   // Splits classes that have the same role pointing into different previously
   // existing classes (1 step). 
   def splitOverRoles(queue : Queue[Option[Subset]], roles : Set[String]) = {
     val elements = extractQueue(queue);
     val localQueue = new Queue[Option[Subset]];
     
     queue.clear;
     
     elements.foreach { el =>
        localQueue.clear;
        localQueue += Some(el);
        
        for( val role <- roles; val sub <- elements ) {
          	//println("[" + role + "/" + sub + "] ");
               forallQueue(localQueue, { (subset, q) =>
               subset.splitOverRole1(role, sub) match {
                 case Some((s1,s2)) => {
                  // println("  - split " + subset + " over " + role + " into " + s1 + " and " + s2);
                   q += Some(s1);
                   q += Some(s2);
                 }
                 
                 case None => q += Some(subset);
               }});
             }
        
        
        queue ++= localQueue;
     }
   }

   
   
   
   
   
   
   // Calls a function for every element in a queue.  In order to ensure that the queue
   // is only traversed once, a None element is appended to the queue before the iteration.
   // This element is removed afterwards.  Crucially, the function gets passed the entire queue
   // as an argument and is allowed to append new elements to the queue.
   def forallQueue(q : Queue[Option[Subset]], proc : (Subset,Queue[Option[Subset]]) => Unit) = {
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
   def extractQueue(queue : Queue[Option[Subset]]) = {
     (for( val x <- queue.elements if x.isInstanceOf[Some[Subset]] ) 
       yield (x match { 
         case Some(subset) => subset; 
         case None => throw new Exception("Extracting from queue with empty elements!") 
       })).toList
   }
   
}
