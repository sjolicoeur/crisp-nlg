#set terminal postscript colour enhanced
set datafile separator ","
set key left top
set xlabel "size (x)"
set ylabel "time (s)"
set xrange [ 1 : 40 ]
set yrange [ 0 : 60 ]
set mxtics 5
set mytics 5
set xtics 5
set ytics 5
set line style 1 lt 1 lw 3 lc rgb "blue"
set line style 2 lt 1 lw 3 lc rgb "red"
set line style 3 lt 1 lw 3 lc rgb "green"
set line style 4 lt 1 lw 3 lc rgb "orange"
set line style 5 lt 1 lw 3 lc rgb "violet"
set line style 6 lt 1 lw 3 lc rgb "brown"
set line style 7 lt 1 lw 3 lc rgb "black"
set line style 8 lt 1 lw 1 lc rgb "grey"
set grid ytics ls 8

plot "RESULTS-give-minimal.csv" \
        using 2:9   title "FF"           with l ls 1, \
     "" using 2:14  title "Metric-FF"    with l ls 2, \
     "" using 2:19  title "FF (ha)"      with l ls 3, \
     "" using 2:27  title "SGPlan 5.22"  with l ls 4, \
     "" using 2:30  title "SGPlan 6"     with l ls 5, \
     "" using 2:39  title "Lama"         with l ls 6, \
     "" using 2:40  title "C3"           with l ls 7
