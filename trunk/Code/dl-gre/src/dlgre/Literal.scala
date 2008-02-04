package dlgre;

case class Literal(from:Subset, p:String, polarity:Boolean, isTrue:Boolean) extends Property {
	override def filter(g:Graph) : List[String] = {
          if( isTrue ) {
            for( val x <- from.getIndividuals if g.hasPredicate(x, p) == polarity )
              yield x;
          } else {
            for( val x <- from.getIndividuals if g.hasPredicate(x, p) != polarity )
              yield x;
          }
        }
}
