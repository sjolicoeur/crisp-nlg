package dlgre.formula;

case class Top extends Formula {
	override def isSatisfied(u:String, graph:Graph) = {
   		true       
        }
        
        override def prettyprint = {
          "T"
        }
        
        override def flatten = {
          this
        }
  
}
