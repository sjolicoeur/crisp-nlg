set terminal pdf dashed font "Helvetica,24" size 30cm,20cm

set datafile separator ","
set key left top

set line style 1 lt 1 lw 10 lc rgb "blue"
set line style 11 lt 1 lw 3 lc rgb "light-blue"

set line style 2 lt 2 lw 10 lc rgb "red"
set line style 12 lt 2 lw 3 lc rgb "light-red"

set line style 3 lt 1 lw 3 lc rgb "green"
set line style 4 lt 1 lw 3 lc rgb "orange"
set line style 5 lt 1 lw 3 lc rgb "violet"
set line style 6 lt 1 lw 3 lc rgb "brown"
set line style 7 lt 1 lw 3 lc rgb "black"
set line style 8 lt 1 lw 1 lc rgb "grey"


set grid ytics ls 8

set ylabel "time (s)"

set mytics 5
set xtics 1
