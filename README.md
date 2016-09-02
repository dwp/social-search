# Social Search - indexing and query tool
This repository contains some rudimentary code to index Slack messages into an instance of Elastic Search, along with a CLI application that allows one to submit questions for analysis, suggesting a list of users most suited to answer the question.

To run either of the tools here, `Scala`, `SBT` and `Elastic Search` are required. If running on a Mac, these are all easily installable using `homebrew`:

```
$ brew install scala sbt elasticsearch
...
$ elasticsearch &
```

## Indexing
At present, the indexing job requires an json data file, generated using the `slack-api-export` tool (which can be found in the Innovation space on Gitlab). The file contains a list of users and their messages. As part of the indexing job, each message is passed through a [third-party topic extraction API](https://www.meaningcloud.com/developer/topics-extraction) which identifies entities and concepts. These, along with the original message contents, are indexed into Elastic Search.

The topic extraction API requires an API key (a free key can be obtained, providing upto 40,000 requests).

To run the indexing job:

```bash
sbt "run-main Indexer"
```

Note, the code assumes that Elastic Search is accessible at `localhost:9200`. The code will also need to be edited to include an API key from MeaningCloud.

## Querying
A simple CLI tool can be run from your terminal or IDE. The tool accepts a question (or any text in fact), runs the input through the same topic extraction API used during the indexing job, performs some very basic text manipulation to remove stop words and unnecessary punctuation, and then performs a query against the Elastic Search instance to determine which users may best be able to engage in a discussion on the topics/keywords obtained from the input.

To run the CLI query application:

```bash
sbt "run-main Query"
```

Note, the code assumes that Elastic Search is accessible at `localhost:9200`. The code will also need to be edited to include an API key from MeaningCloud.
