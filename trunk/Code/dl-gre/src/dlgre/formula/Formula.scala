package dlgre.formula;

import scala.collection.mutable._;

abstract class Formula {
  	//val memoize = new HashMap[Graph[String],Set[String]];
          
  	def prettyprint : String;

        def isSatisfied(u:String, graph:Graph[String]) : Boolean;

        def flatten : Formula;
        
        
        
        
        def extension(graph:Graph[String]) : Set[String] = {
          //println("** CALL to extension");
          
          val ext = new HashSet[String];
          graph.getAllNodes.foreach { u =>  if(isSatisfied(u,graph)) { ext += u; }}
          ext
          /*
          
          if( ! memoize.contains(graph) ) {
             val ext = new HashSet[String];
             
             graph.getAllNodes.foreach { u =>  if(isSatisfied(u,graph)) { ext += u; }}
             memoize += graph -> ext
          } else {
            println("*** (memoized)");

          }
          
          memoize.get(graph).get
          */
        }
        
        def conjoin(l : List[Formula]) : Formula = {
          if( l.isEmpty ) {
            Top()
          } else {
            Conjunction(l)
          }
        }
}
