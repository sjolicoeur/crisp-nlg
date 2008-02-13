package dlgre.formula;

case class Negation(sub:Formula) extends Formula {
  	override def isSatisfied(u:String, graph:Graph) = {
          ! sub.isSatisfied(u,graph)  
        }
          
	override def prettyprint = {
   		"~" + sub.prettyprint       
        }
        
        override def flatten = {
          Negation(sub.flatten)
        }
}
