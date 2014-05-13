adobe-galago
============

Independent Study to Analyze Adobe Product Usage


My version does now support these new tag names after a quick last minute patch, so this
might not be as all encompassing as I'd expect. 
To effectively use the crawler:
install Heritrix, replace the \url{dist/src/main/bin/} file and ensure that jsoup-1.7.3.jar,
balie-1.8.jar, weka.jar, and galago-adobe-1.0-SNAPSHOT.jar 
from the GitHub repository are in Heritrix's \url{lib/} folder. Once this is complete, download the
\url{crawler-beans.cxml} file for the job and use that as Heritrix's target configuration.

This will write a new directory called \url{trec/} in that instance of the job folder. We can
then use Galago's build functionality to create an index on this new \url{trec} folder.
There is a sample dataset that I have collected, but this allows anyone to create
a new dataset based on new data from Adobe.




Need Java 7
Clone git
install galago
make galago.conf
install heritrix 3.2.0
