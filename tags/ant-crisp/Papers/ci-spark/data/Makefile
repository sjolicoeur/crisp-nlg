
all: xtag-k1.pdf xtag-k2.pdf modifiers-all.pdf give-minimal.pdf give2-minimal.pdf give-extracells.pdf give2-extracells.pdf give-button-ord.pdf give-button-unord.pdf give-minimal-grounding.pdf give2-minimal-grounding.pdf

%.pdf: plot-%.plt %.csv common.plt common-twocolumn.plt
	gnuplot $<

