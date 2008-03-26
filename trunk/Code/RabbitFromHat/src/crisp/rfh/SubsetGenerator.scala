package crisp.rfh;

import scala.collection.mutable._;

object SubsetGenerator {
   def foreachSubset[E](set:Iterable[E], proc:Set[E] => Unit) = {
     var setcounter = 0;
     val asArray = new ArrayBuffer[E];
     
     // fix order of elements in array
     set.foreach { x => asArray += x; setcounter = setcounter+1 };
     
     val size = setcounter;
     var counter = BigInt(0);
     val last = BigInt(2).pow(size);
     

     while( counter < last ) {
       val subset = new HashSet[E];
       
       // construct subset
       (0 until size).foreach { bit =>
         if( counter.testBit(bit) ) {
           subset += asArray(bit);
         }
       }
       
       proc(subset);
       counter = counter + 1;
     }
   }
}
