load "common-twocolumn.plt"

set output "xtag-k1.pdf"

set xlabel "size (n)"

plot "xtag-k1.csv" \
        using 2:5  title "FF (search)"      with l ls 11, \
     "" using 2:6  title "FF (total)"  with l ls 1, \
     "" using 2:3   title "Metric-FF (search)"           with l ls 12, \
     "" using 2:4  title "Metric-FF (total)"    with l ls 2, \
     "" using 2:7  title "SPUD reconstruction"  with l ls 5 \
