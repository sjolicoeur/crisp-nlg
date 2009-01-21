
TEXFILES = introduction.tex bisim.tex gre.tex discussion.tex related.tex conclusion.tex example-outputs.tex

main.ps: main.dvi
	dvips -P pdf -t letter -o main.ps main.dvi


main.dvi: main.tex $(FILES) main.bbl Makefile
	latex main.tex
	-grep -s "LaTeX Warning: Citation" main.log && bibtex main
	-grep -s "LaTeX Warning: Citation" main.log && latex main	
	-grep -s "LaTeX Warning: Citation" main.log && latex main
	-grep -s "LaTeX Warning: Label(s) may have changed." main.log && latex main
	-grep -s "LaTeX Warning: Label(s) may have changed." main.log && latex main
	-grep -s "LaTeX Warning: Label(s) may have changed." main.log && latex main


pdf: main.ps
	ps2pdf -dPDFSETTINGS=/printer \
          -dCompatibilityLevel=1.3 \
          -dMaxSubsetPct=100 \
          -dSubsetFonts=true \
          -dEmbedAllFonts=true \
          -sPAPERSIZE=letter \
	main.ps main.pdf