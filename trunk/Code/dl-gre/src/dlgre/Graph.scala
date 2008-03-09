package dlgre;

import scala.collection.mutable._;
import util.StringUtils.join;

class Graph[E]() {
  	class Edge(u:E, e:String, v:E) {
     	  val src = u;
          val label = e;
          val tgt = v;
          
          override def toString = { src + " --[" + label + "]--> " + tgt }
          }
          
          
	private val edges = new HashMap[E, HashMap[String, HashSet[E]]];
        private val nodes = new HashMap[E, HashSet[String]];
        
        private val roles = new HashSet[String]
        private val predicates = new HashSet[String]
        
        private val nodesToIndices = new HashMap[E,Int]
        private val nodeList = new java.util.ArrayList[E]
        
        private val getNodeIndex = {nodename:E => nodesToIndices(nodename)}
        private val getNodeNameForIndex = {index:Int => nodeList.get(index)}
        
        
        def addNode(u : E) = {
          if( !nodes.contains(u) ) {
            nodes += u -> new HashSet;
          }
        }
        
        def containsNode(u:E) = nodes.contains(u);
        
        def removeNode(u:E) = {
          nodes -= u;
          edges -= u; // remove all outgoing edges
          edges.foreach { entry => entry._2.foreach { other => other._2 -= u }}; 
        }
        
        def addEdge(u : E, v : E, r : String) = {
          edges.getOrElseUpdate(u, new HashMap).getOrElseUpdate(r, new HashSet[E]) += v;
          
          addNode(u);
          addNode(v);
          
          roles += r;
        }
        
        def addPredicate(u : E, p : String) = {
          addNode(u);
          
          nodes.get(u).get += p;
          predicates += p;
        }
        
        
        def getAllRoles : Set[String] = roles;
        def getAllPredicates : Set[String] = predicates;
        
        
        def getAllNodes : List[E] =  nodes.keys.toList
        def getAllNodesAsSet = nodes.keySet
        def getAllNodesIterator = nodes.keys
        
        def getPredicates(u:E) : List[String] = {
          nodes.getOrElse(u, new HashSet).toList
        }
        
        def hasPredicate(u:E, pred:String) : Boolean = {
          nodes.getOrElse(u, new HashSet).contains(pred)
        }
        
        
        def getInEdges(u : E) : List[Edge] = {
          (for( val src <- edges; val pair <- src._2; val tgt <- pair._2 if tgt == u )
            yield new Edge(src._1, pair._1, tgt)).toList
        }
        
        def getInEdges(u : E, label : String) : List[Edge] = {
          (for( val src <- edges; val pair <- src._2; val tgt <- pair._2 if tgt == u && pair._1 == label)
            yield new Edge(src._1, pair._1, tgt)).toList
        }
        
        def getOutEdges(u : E) : List[Edge] = {
          (for( val pair <- edges.getOrElse(u, new HashMap); val tgt <- pair._2  ) //if tgt == u
            yield new Edge(u, pair._1, tgt)).toList
        }
        
        def getOutEdges(u : E, label : String) : List[Edge] = {
          (for( val tgt <- edges.getOrElse(u, new HashMap).getOrElse(label, new ArrayBuffer) )
            yield new Edge(u, label, tgt)).toList
        }
        
        def getAllEdges : List[Edge] = {
          (for( val src <- edges; val pair <- src._2; val tgt <- pair._2 )
            yield new Edge(src._1, pair._1, tgt)).toList
        }
        
        def hasEdge(src:E, role:String, tgt:E) : Boolean = {
          edges.getOrElse(src, new HashMap).getOrElse(role, new HashSet[E]).contains(tgt);
        }
        
        private def foreachOutEdge(u:E, proc:(E => Unit)) = {
          edges.get(u) match {
            case Some(edge) => edge.values.foreach { tgts => tgts.foreach { v => proc(v) } };
            case _ => ;
          }
        }
        
        
        /****** search *****/
          
        def foreachDFS(start: E, proc : E => Unit, cancel: E => Boolean) = {
          val visited = new HashSet[E];
          
          _foreachDFS(start, proc, cancel, visited);
          
          /*
          println(this);
          println("Search covered " + visited.size + " nodes");
          */
        }
        
        private def _foreachDFS(u:E, proc:E => Unit, cancel:E => Boolean, visited:Set[E]) : Unit = {
          if( !visited.contains(u) ) {
            visited += u;
            proc(u);
            
            if( !cancel(u) ) {
              foreachOutEdge(u, v => _foreachDFS(v, proc, cancel, visited));
            }
          }
        }
        
        // OPT: store root nodes directly
        def getRoots = nodes.keys.filter { u => getInEdges(u).size == 0 }
        
        
        
        def foreachDFSr(start:E, proc : E => Unit, cancel: E => Boolean) = {
          val visited = new HashSet[E];
          
          _foreachDFSr(start, proc, cancel, visited);
        }
        
        private def _foreachDFSr(u:E, proc:E => Unit, cancel:E => Boolean, visited:Set[E]) : Unit = {
          if( !visited.contains(u) ) {
            visited += u;
            proc(u);
            
            if( !cancel(u) ) {
              getInEdges(u).foreach { edge =>
              _foreachDFSr(edge.src, proc, cancel, visited);
              }
            }
          }
        }
        
        // OPT: store leaf nodes directly
        def getLeaves = nodes.keys.filter { u => getOutEdges(u).size == 0 }
        
        /*
        def _dfs[T](u:E, found:E => T, combine:(T,T) => T, cancel:E => Boolean, visited:Set[E]) : T = {
          
        }
        
        def dfs[T](u:E, found:E => T, combine:(T,T) => T, cancel:E => Boolean) : T = {
          _dfs(u, found, combine, cancel, new HashSet[E]);
        }
        */

        val and = (a:Boolean,b:Boolean) => a && b;
        
        def _isReachable(u:E, tgt:E, visited:Set[E]) : Boolean = {
          if( !visited.contains(u) ) {
            visited += u;
            
            if( u == tgt ) {
              true
            } else {
              getOutEdges(u).exists { edge =>
              	_isReachable(edge.tgt, tgt, visited);
              }
            }
          } else {
            false
          }
        }
        
        def isReachable(src:E, tgt:E) = _isReachable(src, tgt, new HashSet[E]);
        
        
        /****** support for bitsets ******/
          
        private def recomputeNodeList = {
          nodeList.clear();
          nodesToIndices.clear();
          
          nodes.keySet.toList.foreach { x =>
            nodeList.add(x);
          }
          
          Iterator.range(0,nodeList.size).foreach { i =>
                  nodesToIndices += nodeList.get(i) -> i;
          }
        }
        
        def getNodeSet = {
          if( nodesToIndices.isEmpty ) {
            recomputeNodeList;
          }
          
          new BitSetSet[E](nodeList.size, getNodeIndex, getNodeNameForIndex);
        }
        
        def getNodeSet(a:Collection[E]) : BitSetSet[E] = {
          val ret = getNodeSet;
          ret.addAll(a);
          ret;
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
}
