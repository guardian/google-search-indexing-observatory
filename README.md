# Google Search Indexing Observatory

_How long does it take for content published by news organisations to be available in Google search?_

This broadens [Ophan's Google Search Index Checker](https://github.com/guardian/ophan-google-search-index-checker)
to check for content published by _many_ news organisations, not just the Guardian. We're trying to work out if
the intermittent multi-hour delays we've seen for some Guardian articles to be available in Google Search are
typical for other news organisations too, or if there's actually something particular to the Guardian that needs
to be fixed.

It's an 'observatory' in the same way that the [EFF SSL Observatory](https://www.eff.org/observatory) is - creating
and collating observations of distant sites and processes that are visible to us but beyond our control.

## Steps performed by the Observatory

1. Fetch the Sitemap XML for a news site
2. Hit the [Discovery Engine API (service is named Google Vertex Agent Builder)](https://cloud.google.com/generative-ai-app-builder/docs/reference/rest)
   to check if the content listed is available in Google search.
   [API Consumption](https://console.cloud.google.com/gen-app-builder/monitoring?inv=1&invt=AbigZA&project=ophan-reborn-2017) &
   [Cost 💰💰💰](https://console.cloud.google.com/apis/api/customsearch.googleapis.com/cost?project=ophan-reborn-2017)
   for this can be monitored in the Google Cloud console.
3. Stores whether each article is available (or not) in an AWS DynamoDb table.

## Vertex Agent Builder

When setting up search functionality in the GCP Agent Builder, we need to create both an app and a dataStore in the Agent Builder for each website we want to search (in this case BBC, DailyMail, and NYT).
While GCP's interface suggests this process creates a new search engine with its own database, this isn't actually what happens. Instead, it creates a filtered view of Google Search results, limited to the specific website URL we specify.
_Note:_ Even though our code doesn't directly reference the App ID, you must still create both the app and the dataStore for each website - creating just the dataStore isn't sufficient and leads to API errors.

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
