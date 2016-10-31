# food-stat
Download Review file and put it to project(default path "src/main/resources/Reviews.csv")
run code: 
```
sbt "run translate=true" -mem 512 with translating  or sbt sbt run  -mem 512 without translating
```
Answers.

1. Dublicates are deleted with Spark `distinct`.

2. Embedded spark instance is run with  `setMaster("local[4]").` which means spark will use 4 threads, so  will use multi core CPU power.

3. `-mem 512` allow to control max memory usage. We use embedded Spark. Spark 
 use caching to disk if nessesary.
Also translating job read reviews messages by chunks, so is effective in memory usage.
