load "common.plt"

set output "modifiers-all.pdf"

set xlabel "size (m,n)"

set xtics  rotate by 90 scale 1,1 \
     ("(1,1)" 1, "(2,1)" 2, "" 3 1, "(3,1)" 4, "" 5 1, "" 6 1, "(4,1)" 7, \
      "" 8 1, "" 9 1, "" 10 1, "(5,1)" 11, "" 12 1, "" 13 1, "" 14 1, "" 15 1, \
      "(6,1)" 16, "" 17 1, "" 18 1, "" 19 1, "" 20 1, "" 21 1, "(7,1)" 22,  \
      "" 23 1, "" 24 1, "" 25 1, "" 26 1, "" 27 1, "" 28 1, "(8,1)" 29, \
      "" 30 1, "" 31 1, "" 32 1, "" 33 1, "" 34 1, "" 35 1, "" 36 1, "(9,1)" 37, \
      "" 38 1, "" 39 1, "" 40 1, "" 41 1, "" 42 1, "" 43 1, "" 44 1, "" 45 1, "(10,1)" 46, \
      "" 47 1, "" 48 1, "" 49 1, "" 50 1, "" 51 1, "" 52 1, "" 53 1, "" 54 1)


set grid xtics ls 8

set logscale y

plot "modifiers-all.csv" \
        using 1:4   title "FF (search)"           with l ls 11, \
     "" using 1:5  title "FF (total)"    with l ls 1, \
     "" using 1:6  title "Metric-FF (search)"      with l ls 12, \
     "" using 1:7  title "Metric-FF (total)"  with l ls 2, \
     "" using 1:8  title "SGPLAN 6 (search)"      with l ls 13, \
     "" using 1:9  title "SGPLAN 6 (total)"  with l ls 3, \
     "" using 1:10 title "GraphPlan (Java)" with l ls 4, \
     "" using 1:11 title "SPUD (reconstruction)" with l ls 5

