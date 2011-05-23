load "common-twocolumn.plt"

set terminal pdf dashed font "Helvetica,26" size 30cm,20cm

set output "give2-extracells.pdf"

set xlabel "extra grid width (w)"
set ylabel "time (s)"
set xrange [ 1 : 40 ]
set yrange [ 0 : 35 ]
set mxtics 5
set mytics 5
set xtics 5
set ytics 5

plot "give2-extracells.csv" \
        using 4:9   title "FF"         with l ls 1, \
     "" using 4:14  title "Metric-FF"  with l ls 2, \
     "" using 4:19  title "FF-ha"      with l ls 4, \
     "" using 4:30  title "SGPLAN 6"   with l ls 3, \
     "" using 4:39  title "LAMA"       with l ls 5, \
     "" using 4:40  title "C3"         with l ls 6
