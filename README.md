# PWM

Library to work with PWMs (Position-Weight-Matrices)
Also contains console applications to manipulate PWMs.

Some of the features are:
* merging PWMs
* computing odds matrix
* insertion of the sequence to minimize the discruption motif

# Insertions of sequences into Position-Weights-Matrices (PWMs)


One of the approaches to search for the safe place is applying scoring matrices to PWMs of our motifs, so we can find the place where our insert will affect smallest amount of valuable nucleotides. 
In this library we implemented the algorithm similar to motifBS developed by Computational Genomics Research Group of University of California, Berkeley ( http://compbio.berkeley.edu/people/ed/motifBS.html ) with some improvements:
 * to make parsing and viewing easier I put nucleotide positions as rows
 * support of gaps is added. There I assumed that having a gap means that with some probability this position is not really important, so I added gap probabilities to other nucleotides probabilities. I also provided a multiplicator parameter to increase/decrease gaps important

As we use PWM, which are more sensitive than any consensus strings, I decided to put the insertion into PWM matrix. 
In other words I represent insertion as one-hot Vectors. 
For example if the first nucleotide of the sequence to be inserted is T, it will be represented as a PWM of (0,0,0,1) where 1 for T and 0 for all others nucleotides. If we will make a second round of insertions we probably should substitute 1 to the number of sequences from which the PWM was created.

In console applications the naming convention for insertions is:

sequence_x<number of insertions>_at_<list of insertion positions>_name.tsv
