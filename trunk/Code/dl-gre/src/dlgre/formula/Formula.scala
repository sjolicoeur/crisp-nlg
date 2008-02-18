package dlgre.formula;

import scala.collection.mutable._;

abstract class Formula {
  	val memoize = new HashMap[Graph,Set[String]];
          
  	def prettyprint : String;

          /*
          def prettyprint : String = {
   	  this match {
               case Conjunction(l) => join(for(sub <- l if ! sub.isInstanceOf[Top]) yield sub.prettyprint, " & ");
               case Literal(x,true) => x;
               case Literal(x,false) => "~" + x;
               case Existential(role,sub) => "Ex-" + role + ".(" + sub.prettyprint + ")";
               case Negation(sub) => "~" + sub.prettyprint;
          }
        }
        */
        
        def isSatisfied(u:String, graph:Graph) : Boolean;
        
        // TODO this is strictly speaking wrong -- the graph may change!
        def extension(graph:Graph) : Set[String] = {
          //if( ! memoize.contains(graph) ) {
             val ext = new HashSet[String];
             
             graph.getAllNodes.foreach { u =>  if(isSatisfied(u,graph)) { ext += u; }}
             // println(u + " satisfies " + this + ": " + isSatisfied(u,graph));
            
             //println("  - extension: " + ext);
             ext
             /*
             memoize += graph -> ext
          }
          
          memoize.get(graph).get
          */
        }
        
        def flatten : Formula;
        
        def conjoin(l : List[Formula]) : Formula = {
          if( l.isEmpty ) {
            Top()
          } else {
            Conjunction(l)
          }
        }
}
