# UNDER CONSTRUCTION !

# NestedIn
NestedIn is a standalone command line tool for phylogenetic tree scanning for pattern of horizontal gene transfer. It is a Java implementation of the HGT scanning tool that were used in research papers [Qiu et al, 2013](https://www.cell.com/current-biology/fulltext/S0960-9822(13)01052-X) and [Qiu et al, 2015](https://onlinelibrary.wiley.com/doi/abs/10.1111/jpy.12294). This tool runs in any computater platform (Wondows, Mac, linux) with a Java installation. Simply download the jar file and that is it. There is no other softwares or packages that need to be installed.

## 1. Test if your computer has Java installed
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
If no verson is reported, then you probably do not have java installed yet. Go to the java download page  https://www.oracle.com/technetwork/java/javase/downloads/index.html and download the latest version of JDK.

## 2. Quick start:
Download the NestedIn.jar to, for example, your desktop "\~/Desktop". And download the Example_data.tar.zip to your desktop "\~/Desktop". Unzip the Example_data directory that contains a example input directory 'mytrees' and example outputs 'exampleOutput.trees' and 'exampleOutput.txt'. Now you want to find all trees having the query nested within proteobacterial sequences and at least one of the interier nodes supporting query-bacteria monophyly have high enough bootstraps (that's say 90), so type in terminal console:
```
cd ~/Desktop/Example_data
java -jar ~/Desktop/NestedIn.jar -dir mytrees -don Proteoacteria -cut 90
```
You will get screen ouput:
```
direcotry:     mytrees
donor(s):      Proteobacteria
cut-off:       90.0
take 1 seconds.------------------------->1.000
8 trees meet user criteria.
```
Compare the newly generated output directory and text file with the example ouputs. Hopefully they are of the same.

## 3. Why use NestedIn and what do you get from it?
NestedIn searches for nested position of queries among donor sequences. A 'nested position' is a special type of query-donor monophyletic relations. It is defined as query-donor monophyletic relations supported by two or more interior nodes. For example, in fig. 1, query sequence 'Porphyridium_cruentum_contig_3789' and five proteobacterial (donor) sequences form a monophyletic group suggesting possible HGT between the two taxa. In Fig. 2, 'Porphyridium_cruentum_contig_3789' is nested among proteobacterial sequences with at least two highly supported nodes (black dots) supporting query-donor monoohyly. This nested position provides a more convincing evidence of HGT and indicates the direction of HGT (e.g., proteobacteria-to-rhodophyta in this case). NestedIn allows tree screening for simple query-donor monophyletic relations (e.g., as in Fig.1) with argument `-asn 1`.
![Rhodophyta-Porphyridium_cruentum_4panelsMask makeup](https://user-images.githubusercontent.com/41085300/60680875-10a56c80-9e5b-11e9-8bca-e03e64bf3300.png)
Figures 1-4 are hypothetical trees created for solely illustrating purpose. The acutally trees can be downloaded from [The Porphyridium purpureum Genome Project](http://cyanophora.rutgers.edu/porphyridium/).

There are quite a few phylogenetic tree sorting tools that are availabe at the moment. NestedIn is the only tool that targets 'nested position' and addresses the number of interior nodes that support query-donor monophyly. As outputs, NestedIn provides a list of tree names that meet uerser's search criteria and a directory containing the corresponidng tree files. If requested (argument `-igp`), it outputs, for each tree, the information of all the query-donor monophyletic clades inculded in the tree, including node support, donor sequences in the clade, and sequences of optional taxa that are allowed in the monophyletic clades.  


## 4. More usage information:

### 4.1. Input
NestedIn takes a directory containing newick tree files as input, e.g., `((A,B),(C,(D,E)));`. For NestedIn to figure out query sequence names (e.g., Bacteria.MX1376543), the tree files should be named after the query sequences (i.e., Bacteria.MX1376543.contre(e) or Bacteria.MX1376543.tre(e)). If neither '.contre(e)' nor '.tre(e)' is found in the tree file name, the program chops the name at the first dot '.' and takes the shortened name as query sequence. In the newick tree, the query sequences (e.g., Bacteria.MX1376543) have to be exactly the same as in the tree file names. Otherwise, the program will not be able to identify query-donor monophyletic groups (see provided example data for example).


### 4.2. Mandatory inputs

#### 4.2.1 -dir
To specify the input direcoty that contains all trees to be screened.
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir
```

#### 4.2.2 -don
To specify the HGT donor taxa. For example:
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria
```
This parameter is case sensitive. If HGTs came from more than one donor taxa, e.g., Bacteria and Archaea, join the taxa with dot ',', type:
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria,Cyanobacteria
```


### 4.3. Optional inputs

#### 4.3.1 -out
To specify tag that suffix **out**put directory and output file.
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria -out mytest
```
This command produces an directory 'mytest.trees' containning all qualifying tree files and an output file 'mytest.txt' reporting all qualifying tree file names, number of supporting nodes.

#### 4.3.2 -cut
To specify **cut**off to define strongly supported nodes (default = 0).
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria -cut 85
```
This command sets cutoff at 85. All interior nodes supporting query-donor monophyly with support values no less than 85 is considered strong supporting nodes.

#### 4.3.3 -opt
If **opt**ional taxa, e.g., Cyanidioschyzon in Fig. 3, are allowed to present in the query-bacterial monophyletic group, then type:
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria -opt Cyanidioschyzon
```
for more than one optional taxa, e.g., Cyanidioschyzon and Galderia, type:
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria -opt Cyanidioschyzon,Galderia
```
This option allows users to search for more ancient HGTs that were shared between query taxon and its closely related taxa. The sequences of optional taxa will be recorded and exported when argument `-igp` is on.

#### 4.3.4 -ign
To **ign**ore a taxon, e.g., Xenopus in Fig. 4, while screening the tree files, type:
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria -ign Xenopus
```
This option allows users to ignore sequences from some taxa which they think might be problematic (e.g., contamination). The sequences of ignored taxa will be skipped while tree processing and will not be recorded by the program.

#### 4.3.5 -igp
To request details of query-donor monophyletic ingroup, type:
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria -igp
```
This information will be generated, for each tree meeting criteria, in output directory with one line for each node. Three columns represent node suport, donor sequences joined with comma ',', and sequences from optional taxa join with ',' (with '-opt' argument).   

#### 4.3.6 -asn
To specify minimal number of **a**ll **s**upporing **n**odes (regarless of supporting value) that supports query-donor monophyly. The default value for this parameter is 2 (i.e., defining a nested position).
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria -asn 1
```
This command scans for trees with one or more interior nodes supporting query-donor monophyly (turing off nested position requirement).
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria -asn 3
```
This command scans for trees with three or more interior nodes supporting query-donor monophyly (enforced nested position requirement).

#### 4.3.7 -ssn
To specify minimal number of **s**trongly **s**upported **n**odes (supporting value > cutoff) that supports query-donor monophyly. The default value for this parameter is 1.
```
java -jar ~/Desktop/NestedIn.jar -dir ~/Desktop/mydir -don Proteobacteria -ssn 2
```
This command scans for trees with two or more nodes (with supporing values ≥ 85) supporting query-donor monophyly (enforced nested position requirement).
