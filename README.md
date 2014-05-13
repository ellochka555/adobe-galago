adobe-galago
============

Independent Study to Analyze Adobe Product Usage


Here are some things you need to do to set this up:

1) Install Galago and Heritrix

2) Make a ~/.galago.conf file and add
{
	"tmpdir" : /path/to/some/file
	"appclasspath" : ["edu.umass.cs.ciir.galagoadobe.galago"]
}

3) run the shell script './galago_bckupp' which includes the jar file from this project in the path directory.

4) you should be able to see adobe functions in galago!


Some important files are:

	AdobeCrawl.txt which is a seed list for heritrix. 

	crawler-beans.xml which is the job configuration for heritrix.

All jar files must be put into the Heritrix library file to be able to use the trec file writer for crawling.

