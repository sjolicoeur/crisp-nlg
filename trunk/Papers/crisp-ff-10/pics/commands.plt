set datafile separator ","


set xtics 1
set xrange [1:5]
set xlabel "n"
set ylabel "time (s)"

set line style 1 lt 4 lw 2 lc rgb "red"
set line style 2 lt 2 lw 2 lc rgb "blue"
set line style 3 lt 1 lw 2 lc rgb "green"
set line style 4 lt 3 lw 2 lc rgb "magenta"

plot "./xtag-k2-dist0.txt"  using 1:5 title "Old FF" with l ls 1, "" using 1:6 title "New preprocessor" with l ls 2, "" using 1:7 title "New FF" with l ls 3, "" using 1:8 title "LAMA" with l ls 4

plot "./xtag-k2-dist2.txt"  using 1:2 title "EHC" with l ls 1, "" using 1:3 title "BFS" with l ls 2, "" using 1:4 title "BFS+H" with l ls 3, "" using 1:5 title "LAMA" with l ls 4
