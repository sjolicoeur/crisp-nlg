package dlgre.formula;

abstract class Formula {
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
        
        def flatten : Formula;
        
        def conjoin(l : List[Formula]) : Formula = {
          if( l.isEmpty ) {
            Top()
          } else {
            Conjunction(l)
          }
        }
}
