set terminal pdf dashed font "Helvetica,14" size 30cm,20cm

set datafile separator ","
set key left top

# this is for FF
set line style 1 lt 1 lw 12 lc rgb "#0000DC"
set line style 11 lt 1 lw 6 lc rgb "#4A4AEE"

# for metric-FF
set line style 2 lt 2 lw 12 lc rgb "#C00000"
set line style 12 lt 2 lw 6 lc rgb "#D03636"

# for SGPLAN
set line style 3 lt 3 lw 12 lc rgb "#009C00"
set line style 13 lt 3 lw 7 lc rgb "#3AB63A"

# Graphplan / FF(ha)
set line style 4 lt 4 lw 10 lc rgb "#A44C10" # Graphplan

# SPUD / Lama
set line style 5 lt 5 lw 12 lc rgb "#864C98" 
set line style 15 lt 5 lw 6 lc rgb "#864C98"

# C3
set line style 6 lt 6 lw 12 lc rgb "brown"

set line style 7 lt 7 lw 12 lc rgb "black"


# tic lines
set line style 8 lt 3 lw 1 lc rgb "grey"


set grid ytics ls 8

set ylabel "time (s)"

set mytics 5
set xtics 1
