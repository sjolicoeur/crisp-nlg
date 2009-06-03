load "common-twocolumn.plt"

set terminal pdf dashed font "Helvetica,26" size 30cm,20cm

set output "give-minimal-grounding.pdf"

set xlabel "grid width (N)"
set ylabel "time (s)"
set xrange [ 0 : 40 ]
set yrange [ 0 : 40 ]
set mxtics 5
set mytics 5
set xtics 5
set ytics 5

plot "give-minimal.csv" \
        using 2:7   title "FF (grounding)"      with l ls 11, \
     "" using 2:9   title "FF (total)"          with l ls 1, \
     "" using 2:28  title "SGPLAN 6 (parsing)"  with l ls 13, \
     "" using 2:30  title "SGPLAN 6 (total)"    with l ls 3, \
     "" using 2:37  title "LAMA (grounding)"    with l ls 15, \
     "" using 2:39  title "LAMA (total)"        with l ls 5
