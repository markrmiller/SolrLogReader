A high performance Solr log reader / parser.

Early days.

SolrLogReader [file or folder path] {TextMatchAspect} {TextMatchAspect} ...

Example: SolrLogReader /solr/logs org.apache.solr.cloud

If a folder is given, logs are parsed in reverse order if they end with digits.

Example Output:

Using Text Aspect: org.apache.solr.cloud

Processing file: example1.log

Took 58ms

Searcher Report
-----------------
SolrIndexSearcher main open events: 1

SolrIndexSearcher realtime open events: 0

SolrIndexSearcher register events: 1

Commit Report
-----------------
Commits Found: 2

Contained Optimize: 1

Hard Commits: 2

Soft Commits: 0

With openSearcher: 2

Without openSearcher: 0

Query Report
-----------------
Query [timestamp=6083, query=static+firstSearcher+warming+in+solrconfig.xml, qtime=97, results=0]

Exceptions Report
-----------------
Exceptions found:0

TextMatch Report: org.apache.solr.cloud
-----------------
2871 [main] INFO  org.apache.solr.cloud.ZkController  ￢ﾀﾓ Register node as live in ZooKeeper:/live_nodes/127.0.1.1:8901_solr

7071 [coreZkRegister-1-thread-1] INFO  org.apache.solr.cloud.ZkController  ￢ﾀﾓ We are http://127.0.1.1:8901/solr/collection1/ and leader is http://127.0.1.1:8903/solr/collection1/
