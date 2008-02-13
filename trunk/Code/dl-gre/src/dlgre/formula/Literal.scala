package dlgre.formula;

case class Literal(p:String, polarity:Boolean) extends Formula {
  	override def isSatisfied(u:String, graph:Graph) = {
		graph.hasPredicate(u,p) == polarity;            
        }
  
	override def prettyprint = {
   	   if( polarity ) {
       		p	              
           } else {
                "~" + p
           }
        }
        
        override def flatten = {
          this
        }
}
