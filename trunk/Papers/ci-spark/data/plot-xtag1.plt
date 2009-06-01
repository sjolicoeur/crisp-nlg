#set terminal postscript colour enhanced

load "common.plt"

set xlabel "n"
set ylabel "time (s)"

#set logscale y

set xrange [ 1 : 10 ]
#set yrange [ 0.01 : 800 ]
#set mxtics 5
set mytics 5
set xtics 1
#set ytics 5


plot "xtag-k1.csv" \
        using 2:3   title "Metric-FF (search)"           with l ls 1, \
     "" using 2:4  title "Metric-FF (total)"    with l ls 2, \
     "" using 2:5  title "FF (search)"      with l ls 3, \
     "" using 2:6  title "FF (total)"  with l ls 4
