# Ophan Google Search Index Checker

_This is to check if our stories appears in google search_
 
To understand more about this problem we are trying to solve check this [document](https://docs.google.com/document/d/1lWOM-6mkGaPsI0YpF2HjrkI--6X1AlinaeIOhfmCy4I/edit?hl=en-GB&forcehl=1) 

## Steps performed by the checker

1. Hits CAPI for stories published in the last hour. 
2. Hits [Google Custom Search JSON API](https://developers.google.com/custom-search/v1/introduction) to check if our stories are available in google search index.
3. Store whether each article is available or not in a database.

## Running the Checker locally

### Pre-requisites

These mostly match [the pre-requisites for running Ophan locally](https://github.com/guardian/ophan/blob/main/docs/developing-ophan/running-ophan-locally.md#pre-requisites) -
specifically Java 11 & `sbt`, but also especially the requirement to have
[`ophan` AWS credentials](https://janus.gutools.co.uk/credentials?permissionId=ophan-dev)
from [Janus](https://janus.gutools.co.uk/).

### Running the Lambda locally

Execute this on the command line:

```bash
$ sbt run
```