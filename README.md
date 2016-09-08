# Social Search - indexing and query tool
This repository contains some rudimentary code to index Slack messages into an instance of Elastic Search, along with a CLI application that allows one to submit questions for analysis, suggesting a list of users most suited to answer the question.

To run either of the tools here, `Scala`, `SBT` and `Elastic Search` are required. If running on a Mac, these are all easily installable using `homebrew`:

```
$ brew install scala sbt elasticsearch
...
$ elasticsearch &
```

Note, the application config has the default Elastic Search address of `localhost:9200`. If your instance of Elastic Search is at a different location, you will need to edit the `src/main/resources/application.conf` file accordingly. You will also need to enter the Meaning Cloud API key in the config, if you wish to extract any entities/concepts.

## Indexing
At present, the indexing job requires an json data file, generated using the `slack-api-export` tool (which can be found in the Innovation space on Gitlab). The file contains a list of users and their messages. As part of the indexing job, each message is passed through a [third-party topic extraction API](https://www.meaningcloud.com/developer/topics-extraction) which identifies entities and concepts. These, along with the original message contents, are indexed into Elastic Search.

The topic extraction API requires an API key (a free key can be obtained, providing upto 40,000 requests).

Before indexing, create a new index and then specify a mapping, configuring an analyser to generate [shingles](https://en.wikipedia.org/wiki/W-shingling) (word-based ngrams.

```
curl -XPUT 'http://localhost:9200/messages/' -d '{
  "settings" : {
    "index" : {
      "number_of_shards" : 1
    },
    "analysis": {
      "filter": {
        "my_shingle_filter": {
          "type": "shingle",
          "min_shingle_size": 2,
          "max_shingle_size": 4,
          "output_unigrams": false
        }
      },
      "analyzer": {
        "my_shingle_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": [
            "lowercase",
            "my_shingle_filter"
          ]
        }
      }
    }
  }
}'
```

```
curl -XPUT 'http://localhost:9200/messages/_mapping/slack' -d '{
  "slack": {
    "properties": {
      "content": {
        "type": "string",
        "fields": {
          "shingles": {
            "type": "string",
            "analyzer": "my_shingle_analyzer"
          }
        }
      }
    }
  }
}'
```

To run the indexing job:

```bash
sbt "run-main Indexer"
```

Note, the code assumes that Elastic Search is accessible at `localhost:9200`. The code will also need to be edited to include an API key from MeaningCloud.

## Querying
A simple tool can be run from your terminal or IDE. It can be run as an interactive console application, or as a webservice. It accepts a question (or any text in fact) and performs a query against the Elastic Search instance to determine which users may best be able to engage in a discussion.

To run the CLI query application:

```bash
sbt "run-main Query -i"
```

To run the webserver, simply omit the `-i` flag;

```bash
sbt "run-main Query"
```

Then using `curl` or a web browser, perform a query using the following endpoint:

```bash
curl 'http://localhost:8080/ask?q=question+goes+here'
```

Example response:

```
{
  "question" : "question goes here",
  "entities" : [ ],
  "concepts" : [ "question", "location" ],
  "users" : [ {
    "user_id" : "U123ABC",
    "score" : 1.234567
  }, {
    "user_id" : "U456DEF",
    "score" : 0.987654
  }]
}
```
