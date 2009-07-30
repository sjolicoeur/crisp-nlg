load "common-twocolumn.plt"

set terminal pdf dashed font "Helvetica,26" size 30cm,20cm

set output "give-button-unord.pdf"

set xrange [ 1: ]
set xlabel "buttons (b)"
set ylabel "time (s)"
set xtics 5 
set mxtics 1
set log y


plot "give-button-unord.csv" \
        using 1:7  title "FF (grounding)"      with l ls 11, \
     "" using 1:9  title "FF (total)"          with l ls 1, \
     "" using 1:39 title "SGPLAN 6 (parsing)"  with l ls 12, \
     "" using 1:41 title "SGPLAN 6 (total)"    with l ls 2
