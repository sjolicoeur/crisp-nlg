package dlgre.formula;

import dlgre.Graph;

import scala.collection.mutable._;

class Simplifier(graph:Graph) {
	private val _extension = new HashMap[Formula,Set[String]];
        
        private def extension(fmla:Formula) : Set[String] = {
          if( ! _extension.contains(fmla) ) {
            val e = new HashSet[String];
            
            // println("[recompute " + fmla + "]");
            
            graph.getAllNodes.foreach { u =>
              if( fmla.isSatisfied(u,graph) ) {
                e += u;
              }
            }
            
            _extension += fmla -> e;
          }
          
          _extension.get(fmla).get;
        }
        
        private def extension(l : List[Formula]) : Set[String] = {
          val ext = new HashSet[String];
          
          ext ++= graph.getAllNodes;
          l.foreach { f => ext intersect extension(f) }
          
          ext
        }
        
        
        
        def simplify(fmla : Formula) = {
           simplifyConjunctions(fmla.flatten);  
        }
        
        private def simplifyConjunctions(fmla : Formula) : Formula = {
           fmla match {
             case Conjunction(l) => fmla.conjoin(replaceNegativeConjunctions(removeEntailedNegatives(l map simplifyConjunctions, Nil)))
             case Existential(r,sub) => Existential(r,simplifyConjunctions(sub))
             case Literal(x,y) => fmla
             case Negation(sub) => Negation(simplifyConjunctions(sub))
             case Top() => fmla
           }
        }
        
        private def removeEntailedNegatives(l : List[Formula], accu : List[Formula]) : List[Formula] = {
          if( l.isEmpty ) {
            accu
          } else {
            val fmla = l.head;
            
            fmla match {
              case Literal(p,false) => if( isEntailed(fmla, l.tail ::: accu) ) {
                			removeEntailedNegatives(l.tail, accu);
                                       } else {
                                        removeEntailedNegatives(l.tail, fmla::accu)
                                       }
              case Negation(p) => if( isEntailed(fmla, l.tail ::: accu) ) {
                                    removeEntailedNegatives(l.tail, accu);
                                  } else {
                                    removeEntailedNegatives(l.tail, fmla::accu)
                                  }
              case _ => removeEntailedNegatives(l.tail, fmla::accu)

            }
          }
        }
        
      private def isEntailed(fmla : Formula, others : List[Formula] ) : Boolean = {
        extension(others) subsetOf extension(fmla)
      }
      
      // TODO: This only captures the very special case of a conjunction such that the
      // conjunction of all negative literals in it is exactly coextensive with a single
      // positive atom.
      private def replaceNegativeConjunctions(l : List[Formula]) : List[Formula] = {
        val negativeLiterals = l filter { f => f match { case Literal(p,false) => true; case _ => false }}
        val rest = l remove { f => f match { case Literal(p,false) => true; case _ => false }}
        
        val ext = extension(negativeLiterals)
        val alternatives = (for( p <- graph.getAllPredicates if ext == extension(Literal(p,true)) ) yield Literal(p,true)).toList
        
        if( !negativeLiterals.isEmpty && !alternatives.isEmpty ) {
          alternatives.head :: rest
        } else {
          l
        }
      }
        
}
