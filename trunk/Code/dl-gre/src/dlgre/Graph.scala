package dlgre;

import scala.collection.mutable._;

class Graph() {
  	class Edge(u:String, e:String, v:String) {
     	  val src = u;
          val label = e;
          val tgt = v;
          
          override def toString = { src + " --[" + label + "]--> " + tgt }
          }
          
          
	private val edges = new HashMap[String, HashMap[String, ArrayBuffer[String]]];
        private val nodes = new HashMap[String, HashSet[String]];
        
        private val roles = new HashSet[String]
        private val predicates = new HashSet[String]
        
        def addNode(u : String) = {
          if( !nodes.contains(u) ) {
            nodes += u -> new HashSet;
          }
        }
        
        def addEdge(u : String, v : String, r : String) = {
          edges.getOrElseUpdate(u, new HashMap).getOrElseUpdate(r, new ArrayBuffer) += v;
          
          addNode(u);
          addNode(v);
          
          roles += r;
        }
        
        def addPredicate(u : String, p : String) = {
          addNode(u);
          
          nodes.get(u).get += p;
          predicates += p;
        }
        
        
        def getAllRoles : Set[String] = roles;
        def getAllPredicates : Set[String] = predicates;
        
        
        def getAllNodes : List[String] = {
          nodes.keys.toList
        }
        
        
        def getPredicates(u:String) : List[String] = {
          nodes.getOrElse(u, new HashSet).toList
        }
        
        def hasPredicate(u:String, pred:String) : Boolean = {
          nodes.getOrElse(u, new HashSet).contains(pred)
        }
        
        
        def getInEdges(u : String) : List[Edge] = {
          (for( val src <- edges; val pair <- src._2; val tgt <- pair._2 if tgt == u )
            yield new Edge(src._1, pair._1, tgt)).toList
        }
        
        def getInEdges(u : String, label : String) : List[Edge] = {
          (for( val src <- edges; val pair <- src._2; val tgt <- pair._2 if tgt == u && pair._1 == label)
            yield new Edge(src._1, pair._1, tgt)).toList
        }
        
        def getOutEdges(u : String) : List[Edge] = {
          (for( val pair <- edges.getOrElse(u, new HashMap); val tgt <- pair._2 if tgt == u )
            yield new Edge(u, pair._1, tgt)).toList
        }
        
        def getOutEdges(u : String, label : String) : List[Edge] = {
          (for( val tgt <- edges.getOrElse(u, new HashMap).getOrElse(label, new ArrayBuffer) )
            yield new Edge(u, label, tgt)).toList
        }
        
        def hasEdge(src:String, role:String, tgt:String) : Boolean = {
          edges.getOrElse(src, new HashMap).getOrElse(role, new ArrayBuffer).contains(tgt);
        }
        
        
        // TODO: Why does for/yield return Iterable rather than List?
        override def toString() = {
          "Nodes:\n" + 
            join( (for( val pair <- nodes ) yield (pair._1 + ": " + join(pair._2, " "))), "\n") +
          "\n\nEdges:\n" +
            join( for (val src <- edges; val pair <- src._2; val tgt <- pair._2) 
                    yield src._1 + " --[" + pair._1 + "]--> " + tgt,
                  "\n")
        }
        
        def join(list:Iterable[String], sep:String) : String = join(list.toList, sep)
        
        def join(list:List[String], sep:String) : String = list.tail.foldLeft(list.head)((x,y) => x + sep + y)
}
