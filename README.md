# Ophan Google Search Index Checker

_Checking if Guardian content appears in Google search_
 
To understand more about this problem we are trying to solve check this [document](https://docs.google.com/document/d/1lWOM-6mkGaPsI0YpF2HjrkI--6X1AlinaeIOhfmCy4I/edit?hl=en-GB&forcehl=1) 

## Steps performed by the checker

1. Hits the Guardian's Content API (CAPI) for stories published in the last few hours. 
2. Hits [Google Custom Search Site Restricted JSON API](https://developers.google.com/custom-search/v1/site_restricted_api)
   to check if our stories are available in Google search.
   [API Consumption](https://console.cloud.google.com/apis/api/customsearch.googleapis.com/metrics?project=ophan-reborn-2017) &
   [Cost 💰💰💰](https://console.cloud.google.com/apis/api/customsearch.googleapis.com/cost?project=ophan-reborn-2017)
   for this can be monitored in the Google Cloud console.
3. Stores whether each article is available (or not) in an AWS DynamoDb table.

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