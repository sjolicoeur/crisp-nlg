package dlgre.formula;

import scala.collection.mutable._;

abstract class Formula {
  	val memoize = new HashMap[Graph,Set[String]];
          
  	def prettyprint : String;

        def isSatisfied(u:String, graph:Graph) : Boolean;

        def flatten : Formula;
        
        
        
        
        // TODO this is strictly speaking wrong -- the graph may change!
        def extension(graph:Graph) : Set[String] = {
          if( ! memoize.contains(graph) ) {
             val ext = new HashSet[String];
             
             graph.getAllNodes.foreach { u =>  if(isSatisfied(u,graph)) { ext += u; }}
             memoize += graph -> ext
          }
          
          memoize.get(graph).get
        }
        
        def conjoin(l : List[Formula]) : Formula = {
          if( l.isEmpty ) {
            Top()
          } else {
            Conjunction(l)
          }
        }
}
