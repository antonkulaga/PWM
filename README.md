# PWM

Library to work with PWMs (Position-Weight-Matrices)
Also contains console applications to manipulate PWMs.

Some of the features are:
* merging PWMs
* computing odds matrix
* insertion of the sequence to minimize the discruption motif

Library
=======

Reading files
-------------

PWM can be created from tab and column separated files of the following format:
```tsv
-	3.0	3.0	0.0	0.0	0.0	0.0	0.0	0.0	368.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	683.0	0.0	0.0	0.0	5.0	368.0	99.0	2.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0	398.0	0.0	0.0	2.0	0.0	0.0	0.0	0.0	2.0	0.0	0.0	0.0	0.0	0.0	368.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	2.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	2.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	681.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	2.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	315.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	7.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	368.0	0.0	0.0	1.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	2.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	368.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	1.0	0.0	1.0	1.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	1.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	2.0	2.0	0.0	0.0	0.0	0.0	0.0	0.0	66.0	
A	693.0	4.0	0.0	4.0	2.0	0.0	6.0	696.0	293.0	2.0	2.0	6.0	2.0	694.0	51.0	694.0	93.0	0.0	4.0	12.0	3.0	694.0	2.0	128.0	35.0	0.0	2.0	1.0	388.0	3.0	586.0	2.0	4.0	12.0	0.0	0.0	1.0	2.0	10.0	2.0	0.0	2.0	0.0	694.0	695.0	666.0	7.0	6.0	0.0	0.0	273.0	0.0	1.0	0.0	6.0	0.0	4.0	3.0	511.0	2.0	298.0	0.0	406.0	454.0	2.0	3.0	8.0	0.0	688.0	2.0	694.0	27.0	688.0	2.0	686.0	688.0	4.0	692.0	680.0	0.0	0.0	0.0	0.0	0.0	690.0	2.0	53.0	694.0	682.0	6.0	0.0	0.0	0.0	2.0	2.0	0.0	0.0	0.0	0.0	0.0	3.0	0.0	0.0	0.0	0.0	243.0	2.0	657.0	0.0	0.0	0.0	690.0	696.0	2.0	0.0	0.0	694.0	0.0	694.0	0.0	692.0	0.0	4.0	0.0	2.0	690.0	694.0	2.0	0.0	310.0	4.0	4.0	14.0	0.0	0.0	2.0	692.0	0.0	694.0	2.0	696.0	2.0	694.0	4.0	4.0	684.0	0.0	677.0	0.0	0.0	2.0	0.0	642.0	690.0	696.0	4.0	683.0	2.0	6.0	0.0	4.0	140.0	0.0	2.0	0.0	2.0	0.0	0.0	19.0	696.0	693.0	3.0	0.0	0.0	3.0	1.0	690.0	690.0	133.0	0.0	4.0	0.0	688.0	2.0	660.0	0.0	2.0	0.0	0.0	690.0	375.0	2.0	2.0	10.0	4.0	0.0	0.0	0.0	326.0	677.0	57.0	2.0	0.0	0.0	688.0	696.0	69.0	13.0	4.0	2.0	690.0	0.0	692.0	696.0	563.0	693.0	4.0	0.0	684.0	690.0	692.0	0.0	686.0	4.0	0.0	4.0	0.0	0.0	18.0	6.0	326.0	0.0	330.0	552.0	692.0	637.0	691.0	2.0	9.0	688.0	2.0	696.0	0.0	694.0	3.0	694.0	690.0	0.0	0.0	696.0	0.0	1.0	2.0	0.0	2.0	694.0	4.0	694.0	694.0	687.0	13.0	0.0	2.0	0.0	2.0	0.0	8.0	2.0	2.0	1.0	691.0	4.0	0.0	0.0	2.0	0.0	4.0	0.0	10.0	2.0	0.0	0.0	692.0	654.0	2.0	8.0	2.0	649.0	4.0	691.0	4.0	688.0	2.0	0.0	0.0	2.0	691.0	685.0	2.0	8.0	2.0	0.0	8.0	0.0	0.0	2.0	0.0	2.0	2.0	680.0	0.0	652.0	0.0	688.0	6.0	4.0	645.0	0.0	0.0	0.0	616.0	0.0	0.0	696.0	694.0	692.0	0.0	696.0	2.0	4.0	0.0	2.0	3.0	0.0	2.0	0.0	15.0	25.0	630.0	
C	0.0	16.0	0.0	688.0	0.0	2.0	580.0	0.0	35.0	4.0	0.0	0.0	4.0	0.0	4.0	2.0	0.0	30.0	300.0	0.0	4.0	0.0	690.0	529.0	6.0	578.0	209.0	4.0	2.0	2.0	0.0	1.0	0.0	675.0	670.0	0.0	2.0	686.0	0.0	0.0	0.0	2.0	2.0	0.0	0.0	0.0	675.0	2.0	0.0	0.0	2.0	0.0	2.0	9.0	293.0	0.0	5.0	685.0	166.0	4.0	4.0	12.0	0.0	230.0	0.0	2.0	677.0	0.0	0.0	4.0	0.0	667.0	4.0	0.0	0.0	0.0	2.0	0.0	0.0	0.0	0.0	694.0	386.0	692.0	2.0	6.0	0.0	2.0	2.0	686.0	2.0	0.0	692.0	692.0	10.0	14.0	5.0	0.0	2.0	0.0	0.0	0.0	4.0	0.0	2.0	2.0	380.0	0.0	0.0	0.0	694.0	6.0	0.0	692.0	0.0	690.0	0.0	691.0	0.0	13.0	0.0	5.0	2.0	0.0	7.0	4.0	2.0	656.0	0.0	0.0	6.0	668.0	666.0	376.0	0.0	4.0	0.0	317.0	0.0	661.0	0.0	4.0	0.0	0.0	672.0	2.0	2.0	0.0	304.0	2.0	0.0	0.0	0.0	0.0	0.0	686.0	9.0	684.0	4.0	688.0	20.0	0.0	2.0	0.0	0.0	6.0	4.0	14.0	368.0	0.0	0.0	2.0	0.0	0.0	2.0	683.0	0.0	1.0	4.0	2.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	4.0	694.0	0.0	0.0	8.0	680.0	0.0	686.0	0.0	0.0	0.0	2.0	0.0	4.0	0.0	15.0	696.0	8.0	0.0	104.0	0.0	1.0	375.0	2.0	1.0	0.0	0.0	0.0	0.0	0.0	2.0	0.0	2.0	2.0	8.0	2.0	2.0	694.0	6.0	12.0	676.0	182.0	0.0	0.0	2.0	356.0	6.0	4.0	4.0	5.0	637.0	0.0	0.0	10.0	0.0	687.0	0.0	2.0	0.0	0.0	14.0	399.0	0.0	0.0	0.0	692.0	357.0	678.0	0.0	24.0	0.0	0.0	2.0	677.0	0.0	283.0	682.0	0.0	26.0	18.0	2.0	26.0	0.0	2.0	0.0	2.0	0.0	0.0	2.0	0.0	661.0	0.0	0.0	0.0	694.0	0.0	2.0	694.0	0.0	691.0	0.0	686.0	2.0	2.0	0.0	0.0	2.0	2.0	24.0	2.0	11.0	694.0	609.0	0.0	0.0	0.0	686.0	4.0	2.0	14.0	2.0	694.0	0.0	2.0	0.0	50.0	2.0	0.0	686.0	10.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	2.0	0.0	696.0	0.0	567.0	2.0	676.0	0.0	3.0	4.0	0.0	2.0	14.0	2.0	0.0	
G	0.0	613.0	0.0	4.0	0.0	692.0	2.0	0.0	0.0	686.0	0.0	688.0	686.0	2.0	0.0	0.0	2.0	2.0	7.0	0.0	687.0	0.0	2.0	6.0	0.0	3.0	2.0	0.0	2.0	689.0	2.0	691.0	690.0	0.0	1.0	0.0	0.0	2.0	678.0	0.0	0.0	686.0	692.0	2.0	0.0	30.0	2.0	681.0	692.0	684.0	2.0	0.0	0.0	0.0	1.0	0.0	4.0	0.0	2.0	0.0	391.0	0.0	0.0	10.0	49.0	689.0	6.0	0.0	6.0	684.0	0.0	2.0	2.0	694.0	2.0	6.0	669.0	0.0	6.0	0.0	0.0	0.0	0.0	0.0	2.0	688.0	5.0	0.0	0.0	2.0	0.0	6.0	0.0	2.0	8.0	0.0	689.0	0.0	694.0	0.0	0.0	696.0	0.0	696.0	0.0	436.0	0.0	6.0	0.0	2.0	0.0	0.0	0.0	2.0	0.0	6.0	0.0	3.0	2.0	681.0	2.0	683.0	2.0	2.0	687.0	0.0	0.0	15.0	1.0	0.0	0.0	20.0	14.0	0.0	0.0	0.0	4.0	379.0	2.0	33.0	0.0	690.0	2.0	692.0	3.0	10.0	692.0	15.0	4.0	0.0	2.0	696.0	4.0	6.0	0.0	2.0	2.0	4.0	0.0	8.0	0.0	4.0	2.0	0.0	4.0	688.0	5.0	682.0	288.0	0.0	2.0	0.0	0.0	234.0	691.0	2.0	0.0	3.0	553.0	0.0	692.0	558.0	2.0	692.0	2.0	0.0	0.0	0.0	0.0	0.0	6.0	684.0	10.0	644.0	2.0	19.0	4.0	0.0	360.0	7.0	633.0	657.0	0.0	0.0	0.0	0.0	0.0	671.0	689.0	0.0	0.0	691.0	4.0	0.0	0.0	1.0	690.0	675.0	10.0	2.0	2.0	0.0	4.0	0.0	2.0	4.0	0.0	20.0	474.0	0.0	0.0	11.0	2.0	137.0	0.0	55.0	0.0	55.0	2.0	8.0	680.0	0.0	9.0	2.0	689.0	2.0	4.0	0.0	295.0	0.0	0.0	0.0	2.0	0.0	8.0	0.0	285.0	2.0	2.0	3.0	4.0	0.0	405.0	0.0	355.0	18.0	0.0	691.0	3.0	693.0	1.0	0.0	694.0	2.0	688.0	0.0	337.0	2.0	686.0	0.0	2.0	2.0	2.0	2.0	0.0	0.0	0.0	0.0	2.0	0.0	685.0	6.0	694.0	0.0	0.0	4.0	0.0	0.0	0.0	4.0	0.0	0.0	2.0	6.0	0.0	292.0	0.0	0.0	0.0	14.0	0.0	0.0	646.0	0.0	687.0	4.0	18.0	694.0	4.0	0.0	36.0	696.0	685.0	0.0	0.0	0.0	0.0	0.0	127.0	2.0	18.0	2.0	690.0	0.0	0.0	8.0	665.0	0.0	0.0	
T	0.0	60.0	696.0	0.0	694.0	2.0	108.0	0.0	0.0	4.0	694.0	2.0	4.0	0.0	641.0	0.0	601.0	664.0	385.0	1.0	2.0	2.0	2.0	28.0	287.0	16.0	481.0	691.0	304.0	2.0	108.0	2.0	2.0	9.0	25.0	696.0	693.0	6.0	8.0	694.0	696.0	6.0	2.0	0.0	1.0	0.0	12.0	6.0	4.0	12.0	21.0	696.0	693.0	685.0	396.0	696.0	683.0	8.0	15.0	690.0	3.0	684.0	290.0	2.0	277.0	2.0	5.0	696.0	2.0	6.0	2.0	0.0	2.0	0.0	8.0	2.0	21.0	4.0	8.0	696.0	696.0	2.0	310.0	4.0	2.0	0.0	638.0	0.0	12.0	2.0	694.0	690.0	2.0	0.0	676.0	682.0	2.0	696.0	0.0	696.0	693.0	0.0	692.0	0.0	694.0	15.0	314.0	33.0	696.0	694.0	2.0	0.0	0.0	0.0	696.0	0.0	2.0	2.0	0.0	2.0	2.0	8.0	688.0	694.0	0.0	2.0	0.0	23.0	14.0	386.0	686.0	4.0	2.0	320.0	696.0	690.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	17.0	0.0	2.0	4.0	388.0	694.0	692.0	0.0	50.0	0.0	0.0	4.0	2.0	6.0	686.0	0.0	672.0	552.0	692.0	694.0	692.0	0.0	687.0	0.0	21.0	0.0	1.0	691.0	696.0	462.0	0.0	10.0	4.0	2.0	6.0	694.0	0.0	138.0	6.0	2.0	34.0	696.0	694.0	692.0	2.0	6.0	0.0	2.0	4.0	42.0	4.0	677.0	692.0	696.0	8.0	12.0	2.0	37.0	681.0	0.0	0.0	0.0	523.0	5.0	2.0	319.0	4.0	4.0	0.0	0.0	133.0	2.0	2.0	19.0	2.0	2.0	0.0	688.0	4.0	690.0	0.0	682.0	684.0	0.0	22.0	690.0	2.0	683.0	8.0	0.0	0.0	0.0	0.0	2.0	685.0	0.0	4.0	0.0	0.0	0.0	0.0	0.0	2.0	682.0	2.0	0.0	696.0	695.0	0.0	339.0	8.0	2.0	15.0	0.0	0.0	4.0	2.0	696.0	6.0	14.0	339.0	652.0	670.0	1.0	665.0	2.0	2.0	692.0	0.0	694.0	6.0	693.0	355.0	33.0	0.0	694.0	694.0	0.0	2.0	38.0	0.0	688.0	2.0	46.0	4.0	2.0	4.0	2.0	0.0	694.0	694.0	666.0	3.0	0.0	0.0	75.0	694.0	696.0	686.0	4.0	692.0	399.0	682.0	692.0	0.0	2.0	694.0	44.0	0.0	6.0	3.0	2.0	23.0	2.0	692.0	696.0	44.0	0.0	11.0	0.0	0.0	4.0	0.0	0.0	0.0	688.0	0.0	690.0	0.0	692.0	694.0	686.0	2.0	669.0	0.0	
```
Where first columns contains row names (usually gaps and nucleotides but can be anything) and all other columns are their frequences at different positions (starting from zero)


Insertion algorithm
-------------------

One of the approaches to search for the safe place is applying scoring matrices to PWMs of our motifs, so we can find the place where our insert will affect smallest amount of valuable nucleotides. 
In this library we implemented the algorithm similar to motifBS developed by Computational Genomics Research Group of University of California, Berkeley ( http://compbio.berkeley.edu/people/ed/motifBS.html ) with some improvements:
 * to make parsing and viewing easier I put nucleotide positions as rows
 * support of gaps is added. There I assumed that having a gap means that with some probability this position is not really important, so I added gap probabilities to other nucleotides probabilities. I also provided a multiplicator parameter to increase/decrease gaps important

As we use PWM, which are more sensitive than any consensus strings, I decided to put the insertion into PWM matrix. 
In other words I represent insertion as one-hot Vectors. 
For example if the first nucleotide of the sequence to be inserted is T, it will be represented as a PWM of (0,0,0,1) where 1 for T and 0 for all others nucleotides. If we will make a second round of insertions we probably should substitute 1 to the number of sequences from which the PWM was created.

Console application
===================

Console application is created to manipulate PWMs

```bash
 Usage:
     PWM list
     PWM merge
     PWM insert_at
     PWM insert
 PWM application
 Options and flags:
     --help
         Display this help text.
 Subcommands:
     list
         Lists known files
     merge
         merges all PWMs inside the folder into one
     insert_at
         insert sequence manually into some positions
     insert
         inserts sequence into PWM into the best place
```

list subcommand
---------------
Lists PWM inside the folder and gives their stats and (if --verbose) values
```bash
 Usage: PWM list [--verbose] [--delimiter <string>] <file or folder to read from>
 Lists known files
 Options and flags:
     --help
         Display this help text.
     --verbose, -v
         show values of the found PWMs
     --delimiter <string>, -d <string>
         delimiter to be used when parsing PWMs
```
concat subcommand
-----------------
Usage: PWM concat [--delimiter <string>] <first file> <second file> <output file>
concat two PWMs
Options and flags:
    --help
        Display this help text.
    --delimiter <string>, -d <string>
        delimiter to be used when parsing PWMs

*
Multiplication subcommand
-----------------------
Repeats PWM several times
```
Usage: PWM * [--delimiter <string>] <first file> [<copies>] <output file>
Multiple PWM several times
Options and flags:
    --help
        Display this help text.
    --delimiter <string>, -d <string>
        delimiter to be used when parsing PWMs
```


merge subcommand
---------------
Merges PWM files in the folder together, to generalized PWM
```bash
Usage: PWM merge [--verbose] [--delimiter <string>] <file or folder to read from> <output file>
 --help
     Display this help text.
 --verbose, -v
     show values of the found PWMs
 --delimiter <string>, -d <string>
     delimiter to be used when parsing PWMs
```
insert_at subcommand
--------------------
Insertion of the sequence at manually chosen positions inside PWM
```bash
Usage: PWM insert_at --position <integer> [--position <integer>]... [--value <floating-point>] [--verbose] [--delimiter <string>] <sequence> <file or folder to read from> <output file>
insert sequence manually into some positions
Options and flags:
    --help
        Display this help text.
    --position <integer>, -p <integer>
        positions to which insert the sequence
    --value <floating-point>, -v <floating-point>, -a <floating-point>, -l <floating-point>
        default value for inserted nucleotides in PWM
    --verbose, -v
        show values of the found PWMs
    --delimiter <string>, -d <string>
        delimiter to be used when parsing PWMs
```
To use this command just select tsv/csv with PWM and provide sequence and position for the insertion.
Insertion will be done in a format of one-hot vectors multiplied by *value* option, if bases like N, W, Y and so on will present it will insert corresponding probabilities 
*value" option is needed to increase importance of the inserted sequence to avoid its potential disruption in any further changes of the PWM.

insert subcommand
-----------------
Finding and inserting into best positions in the PWM, trying to minimize PWM disruption.
```bash
Usage: PWM insert [--num <integer>] [--distance <integer>] [--value <floating-point>] [--miss <floating-point>] [--gapmult <floating-point>] [--verbose] [--delimiter <string>] [--begin <integer>] [--end <integer>] <sequence> <file or folder to read from> <output folder or file>
inserts sequence into PWM into the best place
Options and flags:
    --help
        Display this help text.
    --num <integer>, -n <integer>
        number of insertions
    --distance <integer>, -d <integer>, -i <integer>, -s <integer>, -t <integer>
        minimum distance between two insertions if insertion number > 1
    --value <floating-point>, -v <floating-point>, -a <floating-point>, -l <floating-point>
        default value for inserted nucleotides in PWM
    --miss <floating-point>, -m <floating-point>
        miss score (negative value required)
    --gapmult <floating-point>, -g <floating-point>
        how much we care about gaps when inserting
    --verbose, -v
        show values of the found PWMs
    --delimiter <string>, -d <string>
        delimiter to be used when parsing PWMs
    --begin <integer>, -b <integer>
        from which nucleotide to start insertions
    --end <integer>, -e <integer>
        until which nucleotide to stop insertion
```
This command can be applied either to a file or a folder.
If it is applied to a file it reads its PWM, makes insertions and saves as an output file.
If it is applied to a folder it traverses the folder for tsv/csv-es, reads PWM from each of the files, 
makes an insertion to each of the file and saves the files with the following naming convention:
```bash
sequence_x<number of insertions>_at_<list of insertion positions>_<filename>.tsv
``` 

Insertions will be done in a format of one-hot vectors multiplied by *value* option. 
*value* option is needed to increase importance of the inserted sequence to avoid its potential disruption in any further changes of the PWM.
Parameter *num* defines the number of insertions into the file, parameter *distance* - minimal distance between insertions.
Parameters *miss* and *gapmult* are used to tune the algorithm. 
*miss* is a negative number that means the penalty for the mismatch
*gapmult* increases the importance of gaps, and thus - ability to insert any base into the position. 
*begin* and *end* are important if you want to select an insertion place inside some range within PWM

insert_pwm subcommand
---------------------
same as insert command but uses PWM instead of the sequence.
```
Usage: PWM insert_pwm [--num <integer>] [--distance <integer>] [--value <floating-point>] [--miss <floating-point>] [--gapmult <floating-point>] [--verbose] [--delimiter <string>] [--begin <integer>] [--end <integer>] <PWM file or folder to read from> <file or folder to read from> <output folder or file>
inserts PWM into larger PWM into the best place
Options and flags:
    --help
        Display this help text.
    --num <integer>, -n <integer>
        number of insertions
    --distance <integer>, -d <integer>, -i <integer>, -s <integer>, -t <integer>
        minimum distance between two insertions if insertion number > 1
    --value <floating-point>, -v <floating-point>, -a <floating-point>, -l <floating-point>
        default value for inserted nucleotides in PWM
    --miss <floating-point>, -m <floating-point>
        miss score (negative value required)
    --gapmult <floating-point>, -g <floating-point>
        how much we care about gaps when inserting
    --verbose, -v
        show values of the found PWMs
    --delimiter <string>, -d <string>
        delimiter to be used when parsing PWMs
    --begin <integer>, -b <integer>
        from which nucleotide to start insertions
    --end <integer>, -e <integer>
        until which nucleotide to stop insertion
```

generate subcommand
-------------------
```
Usage: PWM generate [--delimiter <string>] [--verbose] [--tries <integer>] [--max_repeats <integer>] [--avoid <string>]... [--gc_min <floating-point>] [--gc_max <floating-point>] [--instances <integer>] [--enzyme <string>] [--sticky_left <string>] [--sticky_right <string>] [--win_gc_size1 <integer>] [--win_gc_min1 <floating-point>] [--win_gc_max1 <floating-point>] [--win_gc_size2 <integer>] [--win_gc_min2 <floating-point>] [--win_gc_max2 <floating-point>] [--sticky_diff <integer>] [--sticky_gc <integer>] <file or folder to read from> <output file>
Generates from PWM
Options and flags:
    --help
        Display this help text.
    --delimiter <string>, -d <string>
        delimiter to be used when parsing PWMs
    --verbose, -v
        show values of the found PWMs
    --tries <integer>, -t <integer>
        Maximum number of attempts to generate a good sequence
    --max_repeats <integer>, -r <integer>
        Maximum repeat length
    --avoid <string>, -a <string>
        Avoid enzymes
    --gc_min <floating-point>
        minimum GC content
    --gc_max <floating-point>
        maximum GC content
    --instances <integer>, -i <integer>
        Maximum number of instances per template
    --enzyme <string>, -e <string>
        Golden gate enzyme for cloning, if nothing is chosen no GoldenGate sites are added
    --sticky_left <string>
        Flank assembled sequence from the left with sticky side
    --sticky_right <string>
        Flank assembled sequence from the right with sticky side
    --win_gc_size1 <integer>
        GC window1 size, if < 1 then window is not used
    --win_gc_min1 <floating-point>
        window1 minimum GC content
    --win_gc_max1 <floating-point>
        window1 maximum GC content
    --win_gc_size2 <integer>
        GC window2 size, if < 1 then window is not used
    --win_gc_min2 <floating-point>
        window2 minimum GC content
    --win_gc_max2 <floating-point>
        window2 maximum GC content
    --sticky_diff <integer>
        minimal difference between sticky sides
    --sticky_gc <integer>
        minimal numbers of G || C nucleotides in the sticky end
```
Generates sequences PWMs based on synthesis parameters and restriction sides to avoid, also allows to add GoldenGate sites to ease cloning.

generate_analytical
--------------------
Generates from PWM with information about repeats
```
Usage: PWM generate_analytical [--delimiter <string>] [--verbose] [--tries <integer>] [--max_repeats <integer>] [--avoid <string>]... [--gc_min <floating-point>] [--gc_max <floating-point>] [--win_gc_size1 <integer>] [--win_gc_min1 <floating-point>] [--win_gc_max1 <floating-point>] [--win_gc_size2 <integer>] [--win_gc_min2 <floating-point>] [--win_gc_max2 <floating-point>] [--sticky_diff <integer>] [--sticky_gc <integer>] [--sticky_tries <integer>] <file or folder to read from> <output file>
Generates PWM with information about repeats of length --max_repeats which is saved with analytics suffix. It can also optionally list of repeats of length --max_repeats more frequent thatn log_repeats value
Options and flags:
    --help
        Display this help text.
    --delimiter <string>, -d <string>
        delimiter to be used when parsing PWMs
    --verbose, -v
        show values of the found PWMs
    --tries <integer>, -t <integer>
        Maximum number of attempts to generate a good sequence
    --max_repeats <integer>, -r <integer>
        Maximum repeat length
    --log_repeats <integer>
        if more than 0 then it writes repeats which is more frequent than N to the log file with their coordinates
    --avoid <string>, -a <string>
        Avoid enzymes
    --gc_min <floating-point>
        minimum GC content
    --gc_max <floating-point>
        maximum GC content
    --win_gc_size1 <integer>
        GC window1 size, if < 1 then window is not used
    --win_gc_min1 <floating-point>
        window1 minimum GC content
    --win_gc_max1 <floating-point>
        window1 maximum GC content
    --win_gc_size2 <integer>
        GC window2 size, if < 1 then window is not used
    --win_gc_min2 <floating-point>
        window2 minimum GC content
    --win_gc_max2 <floating-point>
        window2 maximum GC content
    --sticky_diff <integer>
        minimal difference between sticky sides
    --sticky_gc <integer>
        minimal numbers of G || C nucleotides in the sticky end
    --sticky_tries <integer>
        Maximum number of attempts to select golden-gate edges if all other parameters match```

Building from source
====================

To build from source install sbt (https://www.scala-sbt.org/ ) and run it
```
sbt
```
after that in sbt console, do:
```
run
```
to compile and run
```
test
```
to run project unit-tests
```
universal:packageBin
```
to package binaries. It will create a zip with the program inside *target/universal* folder.
Inside the zip archive there will be *bin* folder with scripts (bash script for Linux and bat script for Windows) to run the application.
to build a docker container
```bash
sbt docker:publishLocal
```
to publish a docker container with the application. Then you can start the container:
```bash
docker run -v /path/to/my/data:/data quay.io/comp-bio-aging/pwm:0.0.16
```

Adding PWM library to your dependencies
========================================

to depend on PWM library and use PWM classes, add the following:
```
resolvers += sbt.Resolver.bintrayRepo("comp-bio-aging", "main")
libraryDependencies += "group.aging-research" %%% "PWM" % "0.0.16"
```
to your sbt configuration.
The you should be able to use PWM classes like:
```scala
import pwms._
val pwm = LoaderPWM.loadFile(File("/path/to/my/file"))
```