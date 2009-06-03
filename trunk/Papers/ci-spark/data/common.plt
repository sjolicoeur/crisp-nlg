set terminal pdf dashed font "Helvetica,14" size 30cm,20cm

set datafile separator ","
set key left top

# this is for FF
set line style 1 lt 1 lw 12 lc rgb "blue"
set line style 11 lt 1 lw 5 lc rgb "light-blue"

# for metric-FF
set line style 2 lt 2 lw 12 lc rgb "red"
set line style 12 lt 2 lw 5 lc rgb "light-red"

# for SGPLAN
set line style 3 lt 3 lw 12 lc rgb "green"
set line style 13 lt 3 lw 6 lc rgb "light-green"

# further line styles
set line style 4 lt 4 lw 12 lc rgb "orange"
set line style 5 lt 5 lw 12 lc rgb "violet"
set line style 6 lt 6 lw 12 lc rgb "brown"
set line style 7 lt 7 lw 12 lc rgb "black"

# tic lines
set line style 8 lt 8 lw 1 lc rgb "grey"


set grid ytics ls 8

set ylabel "time (s)"

set mytics 5
set xtics 1
