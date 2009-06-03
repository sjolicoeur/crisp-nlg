load "common-twocolumn.plt"

set terminal pdf dashed font "Helvetica,26" size 30cm,20cm

set output "give2-minimal-grounding.pdf"

set xlabel "size (b)"
set ylabel "time (s)"
set xrange [ 0 : 40 ]
set yrange [ 0 : 40 ]
set mxtics 5
set mytics 5
set xtics 5
set ytics 5

plot "give2-minimal.csv" \
        using 2:7   title "FF (grounding)"      with l ls 11, \
     "" using 2:9   title "FF (total)"          with l ls 1, \
     "" using 2:28  title "SGPLAN 6 (parsing)"  with l ls 12, \
     "" using 2:30  title "SGPLAN 6 (total)"    with l ls 2, \
     "" using 2:37  title "Lama (grounding)"    with l ls 13, \
     "" using 2:39  title "Lama (total)"        with l ls 3
