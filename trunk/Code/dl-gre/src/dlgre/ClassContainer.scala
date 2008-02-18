package dlgre;

import scala.collection.mutable.HashSet;
import scala.collection.mutable.Set;

import dlgre.formula._;

class ClassContainer(graph:Graph) {
	val classes : Set[Formula] = new HashSet[Formula]; // TODO hashcode for subset
	val simplifier = new Simplifier(graph);
        
        
        classes += new Top();

        
        
        def getClasses = {
   		classes.toList       
        }
        
        def isAllSingletons = {
          getClasses.forall { cl => cl.extension(graph).size == 1 }
        }
        
        def add(subset:Formula) = {
          if( classes.exists { cl => cl.extension(graph) == subset.extension(graph) } ) {
            false
          } else {
            if( isNontrivial(subset) && isInformative(subset) ) {
              val knownClasses = classes.toList;
              
              knownClasses.foreach { other =>
                if( other.extension(graph).size > 1 ) {
            	  val inter = new Conjunction(List(subset,other));
                    
            	  if( isNontrivial(inter) && isInformative(inter)  ) {
            	    classes +=  inter;
                  }
                }
              }
            
              removeUninformativeSubsets();
            
              true
            } else {
              false
            }
          }
          
        }
        
        private def isNontrivial(fmla:Formula) = {
          ! fmla.extension(graph).isEmpty
        }
        
        private def isInformative(subset:Formula) = {
          val individuals = subset.extension(graph);
          val unionOverSubsets = new HashSet[String];
          
          classes.foreach { cl =>
          	val ext = cl.extension(graph);
                  
                if( ext subsetOf individuals ) {
                  unionOverSubsets ++= ext;
                }
          }
          
          individuals != unionOverSubsets
          

          /*
          val allSubsets = (for(s <- classes if subsetToSet(s) subsetOf individuals) yield subsetToSet(s)).toList;
          
         // println("   - informativity of " + subset);
          ! isCompletelyCovered(individuals.toList, allSubsets)
          */
        }
        
        private def removeUninformativeSubsets() = {
          val elements = classes.toList;
          
          elements.foreach { sub =>
                classes -= sub;
                
                if( isInformative(sub) ) {
                  classes += sub;
                }
          }
        }
}
