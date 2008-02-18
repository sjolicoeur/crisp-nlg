package dlgre;

import scala.collection.immutable.HashSet;

import dlgre.formula._;

class ClassContainer(graph:Graph) {
	var classes : Set[Formula] = new HashSet[Formula]; // TODO hashcode for subset
	val simplifier = new Simplifier(graph);
        
        
        classes += new Top();
        
        
        
        def getClasses = {
   		classes.toList       
        }
        
        def isAllSingletons = {
          getClasses.forall { cl => cl.extension(graph).size == 1 }
        }
        
        def add(subset:Formula) = {
          //println("add: " + subset.prettyprint);
          
          if( classes.exists { cl => cl.extension(graph) == subset.extension(graph) } ) {
            //println(" - already known");
            false
          } else {
            if( isNontrivial(subset) && isInformative(subset) ) {
              val knownClasses = classes.toList;
              
              knownClasses.foreach { other =>
                if( other.extension(graph).size > 1 ) {
            	  val inter = new Conjunction(List(subset,other));
                    
            	  if( isNontrivial(inter) && isInformative(inter)  ) {
            	    //println(" - really add: " + inter.prettyprint); 
            	    classes = classes + inter;
            	  } else {
                  /*      
                 if( !isNontrivial(inter) ) {
                   println(" - (" + inter.prettyprint + " is empty)");
                 }
                 
                 if( !isInformative(inter) ) {
                   println(" - (" + inter.prettyprint + " is not informative)");
                 }
                 */
                 }
                } else {
                  //println(" - (" + other.prettyprint + " is singleton)");
                }
              }
            
              removeUninformativeSubsets();
            
              //println(" - classes are now: " + classes.map { cl => cl.prettyprint} );
            
              true
            } else {
              //println(" - no ");
            
              false
            }
          }
          
        }
        
        private def isNontrivial(fmla:Formula) = {
          ! fmla.extension(graph).isEmpty
        }
        
        // TODO - argh inefficient!
        private def isInformative(subset:Formula) = {
          val individuals = subsetToSet(subset);
          val allSubsets = (for(s <- classes if subsetToSet(s) subsetOf individuals) yield subsetToSet(s)).toList;
          
         // println("   - informativity of " + subset);
          ! isCompletelyCovered(individuals.toList, allSubsets)
        }
        
        private def isCompletelyCovered(individuals:List[String], allSubsets:List[Set[String]]) : Boolean = {
          //println("    - icc " + individuals + " with " + allSubsets);
          individuals match {
            case Nil => true;
            case head::tail => allSubsets.exists { sub =>
            			if( sub.contains(head) ) {
                                      isCompletelyCovered(tail.remove {x => sub.contains(x)}, allSubsets)
                                } else {
                                  false
                                }
                      }
          }
        }
        
        
        private def subsetToSet(subset:Formula) : Set[String] = {
          new HashSet[String] ++ subset.extension(graph)
        }
        
        private def removeUninformativeSubsets() = {
          val elements = classes.toList;
          
          elements.foreach { sub =>
          	val backup = classes;
                classes = classes - sub;
                
                if( isInformative(sub) ) {
                  classes = backup;
                }
          }
        }
}
