# UNDER CONSTRUCTION !

# NestedIn
NestedIn is a standalone command line tool for phylogenetic tree scanning for pattern of horizontal gene transfer. It is written in Java and can be run in any computational platform with Java installation. Simply download the jar file which is ready to use. There is no other softwares or packages that need to be installed.

## Test if your computer has Java installed

In command prompt / terminal, type:
```
java -version
```
If java is installed, a feedback would be like below:
```
java version "11.0.2" 2019-01-15 LTS
Java(TM) SE Runtime Environment 18.9 (build 11.0.2+9-LTS)
Java HotSpot(TM) 64-Bit Server VM 18.9 (build 11.0.2+9-LTS, mixed mode)
If no version is reported, you likely have no java.
```
If no verson is reported, then you probably do not have java installed yet. Java can be downloaded from https://www.java.com/en/

## Quick start:

Download the NestedIn.jar to, for example, your desktop "~/Desktop". And download the direcotry containing tree files \(with \*.tre or \*.tree suffix) 'mybush', to your desktop "~/Desktop". Now you want to find all trees having dog nested with bacterial sequences and at least one of the interier nodes supporting dog-bacteria monophyly have high enough bootstraps (that's say 85),  so type in terminal console:
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mybush -don Bacteria -cut 85
```
If more than donors are involved, e.g., bacteria and archaea, join the donors with comma ',':
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mybush -don Bacteria,Archaea -cut 85
```

## What can you get from NestedIn

NestedIn searches for nested position of queries among donor sequences. A 'nested position' is defined as two or more nodes that support query-donor monophyletic groups. In large-scale phylogenomic analysis, it is practically impossible to root all trees in a biological sense. When sorting unrooted trees, a node uniting exclusively query and donor sequences might not  suggests a monophyletic query-donor group that reflects HGT. This hypothesis could be rejected easily when having the tree re-rooted (Fig. 1A). However, when having two (n=2) or more nodes (n>2) supporting query-donor monophyly, the HGT hypothesis becomes more robust (Fig. 1B). That being said, NestedIn allows tree sorting for simple query-donor monophyletic relationship with argument (-mon).

As outputs, NestedIn provides a list of tree names that meet uerser's search criteria and directory containing the corresponidng tree files. If requested (argument '-igs'), it outputs, for each tree, the information of all the query-donor monophyletic clades inculded in the tree, including node support, donor sequences in the clade, and sequences of optional taxa that are allowed in the monophyletic clades.  


## More usages:

### Input
NestedIn takes a directory containing newick tree files as input. For NestedIn to recoginze query sequence names (e.g., Bacteria.MX1376543), the tree files should be named after the query sequences (i.e., Bacteria.MX1376543.contre(e) followed by Bacteria.MX1376543.tre(e)). If neither '.contre' nor '.tre' is found in the tree file name, the program chops the name at the first '.' and takes the shortened name as query sequence. In the newick tree, the query sequences (e.g., Bacteria.MX1376543) have to be exactly the same as in the tree file names. Otherwise, the program will not be able to identify query-donor monophyletic groups (see example data for details).

### -opt
If optional taxa, e.g., Stramenopiles, are allowed to present in the dog-bacterial monophyletic group, then type:
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mybush -don Bacteria -cut 85 -opt Stramenopiles
```
for more than one optional taxa, e.g., Stramenopiles + Rhizaria, type:
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mybush -don Bacteria -cut 85 -opt Stramenopiles,Rhizaria
```

### -ign
To ignore a taxon, e.g., cyanobacteria, while screening the tree files
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mybush -don Bacteria -cut 85 -ign Cyanobacteria
```
To ignore two or more taxa, e.g., Cyanobacteia and Proteobacteria, type:
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mybush -don Bacteria -cut 85 -ign Cyanobacteria,Proteobacteria
```

### -igs
To request details of interior nodes that support user-specified monolyletic clades, type
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mybush -don Bacteria -cut 85 -igs
```
This inforation will be generated, for each tree meeting criteria, in output directory with one line for each node. Three columns represent node suport, donor sequences joined with comma ',', and sequences from optional taxa join with ',' (if '-pot' argument).   
