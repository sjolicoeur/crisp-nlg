package dlgre;

class BitSetSet[E](domainsize:Int, mapper:E => Int, reverse:Int => E) extends java.util.AbstractSet[E] {
  	private val b = new java.util.BitSet(domainsize);
        private val tmp = new java.util.BitSet(domainsize);
          
        // expensive
	override def iterator() : java.util.Iterator[E] = {
          val ret = new java.util.HashSet[E]
          
          Iterator.range(0,domainsize).foreach { x =>
            if( b.get(x) ) {
              ret.add(reverse(x));
            }
          }
          
          ret.iterator()       
        }
        
        override def size() : Int = {
          b.cardinality()
        }
        
        override def add(x:E) = {
          val index = mapper(x);
          val ret = b.get(index);
          
          b.set(mapper(x));
          
          ret
        }
        
        override def contains(x:Any) = {
          if( x.isInstanceOf[E] ) {
            b.get(mapper(x.asInstanceOf[E]));  
          } else {
            false
          }
        }
          
        
        def addAll(c:scala.Collection[E]) = {
          c.foreach { x => add(x) }
        }
        
        def getDomainsize : Int = domainsize
        def getMapper = mapper
        def getReverseMapper = reverse
        
        def addAll(other:BitSetSet[E]) = {
          if( domainsize != other.getDomainsize || mapper != other.getMapper || reverse != other.getReverseMapper ) {
            throw new UnsupportedOperationException("incompatible bitsets");
          }
          
          b.or(other.b);
        }
        
        def intersect(other:BitSetSet[E]) = {
          val ret = new BitSetSet[E](domainsize, mapper, reverse)
          
          ret.b.or(b);
          ret.b.and(other.b);
          
          ret;
        }
        
        // ATTN: not thread-safe
        def isSubsetOf(other:BitSetSet[E]) = {
          tmp.clear();
          tmp.or(other.b);
          tmp.flip(0,domainsize+1);
          tmp.and(b);
          
          tmp.cardinality() == 0
        }
        
        def intersects(other:BitSetSet[E]) = {
          b.intersects(other.b)
        }
        
        def asScalaCollection = {
          new scala.collection.mutable.JavaSetAdaptor[E](this)
        }
        
        override def clear() = b.clear();
            
        override def equals(that: Any): boolean =
          that.isInstanceOf[BitSetSet[E]] && {
            val other = that.asInstanceOf[BitSetSet[E]];
            
            domainsize == other.getDomainsize && mapper == other.getMapper &&
              reverse == other.getReverseMapper && b == other.b
        }
        
        override def hashCode = b.hashCode
}
