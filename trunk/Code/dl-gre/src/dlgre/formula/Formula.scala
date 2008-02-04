package dlgre.formula;

abstract class Formula {
	def prettyprint : String = {
   	  this match {
               case Conjunction(x,Top()) => x.prettyprint;
               case Conjunction(x,y) => x.prettyprint + " & " + y.prettyprint;
               case Literal(x,true) => x;
               case Literal(x,false) => "~" + x;
               case Existential(role,sub) => "Ex-" + role + ".(" + sub.prettyprint + ")";
          }
        }
}
