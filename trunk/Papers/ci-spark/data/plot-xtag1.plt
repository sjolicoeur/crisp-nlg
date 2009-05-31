#set terminal postscript colour enhanced
set datafile separator ","
set key left top
set xlabel "n"
set ylabel "time (s)"

#set logscale y

set xrange [ 1 : 10 ]
#set yrange [ 0.01 : 800 ]
#set mxtics 5
set mytics 5
set xtics 1
#set ytics 5
set line style 1 lt 1 lw 3 lc rgb "blue"
set line style 2 lt 1 lw 3 lc rgb "red"
set line style 3 lt 1 lw 3 lc rgb "green"
set line style 4 lt 1 lw 3 lc rgb "orange"
set line style 5 lt 1 lw 3 lc rgb "violet"
set line style 6 lt 1 lw 3 lc rgb "brown"
set line style 7 lt 1 lw 3 lc rgb "black"
set line style 8 lt 1 lw 1 lc rgb "grey"
set grid ytics ls 8

plot "xtag-k1.csv" \
        using 2:3   title "Metric-FF (search)"           with l ls 1, \
     "" using 2:4  title "Metric-FF (total)"    with l ls 2, \
     "" using 2:5  title "FF (search)"      with l ls 3, \
     "" using 2:6  title "FF (total)"  with l ls 4
