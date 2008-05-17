use Time::HiRes qw( time );

$sum_numSplits = 0;
$numSolved = 0;

$sum_preparetime = 0;
$sum_plannertime = 0;

$iterations = 2;

for( $i = 0; $i < $iterations; $i++ ) {
  $t1 = time();

  $out = `java -cp bin:../crisp-nlg/bin:../DlGre/packages/scala-library.jar crisp.rfh.FfConverter ../crisp-nlg/grammars/rabbit-from-hat/problem-rfh2.xml`;
  ($splits) = ($out =~ (/Found (\d+) splits/ms));
  ($preparetime) = ($out =~ (/Time: (\d+)/ms));

  $t2 = time();

  $ffout = `~/Documents/proj/crisp/FF-v2.3/ff -o rfh2-domain.lisp -f rfh2-problem.lisp`;

  $t3 = time();
  $solved = ($ffout =~ /found legal plan/ms);
  ($plannertime) = ($ffout =~ /(\d+\.\d+) seconds total time/ms);

  $sum_numSplits += $splits;
  $numSolved++ if( $solved );

  $sum_preparetime += $preparetime;
  $sum_plannertime += 1000*$plannertime;
}


print "Average preparation time: ", ($sum_preparetime/$iterations), "ms\n";
print "Average planner time: ", ($sum_plannertime/$iterations), "ms\n";

print "Average number of splits: ", ($sum_numSplits/$iterations), "\n";
print "Solved: $numSolved of $iterations\n";
 
