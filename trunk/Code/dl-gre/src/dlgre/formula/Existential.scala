package dlgre.formula;

case class Existential(role:String, sub:Formula) extends Formula {
  	override def isSatisfied(u:String, graph:Graph) = {
		graph.getAllNodes.exists { v => graph.hasEdge(u,role,v) && sub.isSatisfied(v,graph) };            
        }
          
          
	override def prettyprint = {
          "Ex-" + role + ".(" + sub.prettyprint + ")";       
        }
        
        override def flatten = {
          Existential(role, sub.flatten)
        }
}
