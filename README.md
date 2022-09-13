# Ophan GeoIP DB Refresher

_Retrieves regular updates of the [MaxMind GeoIP Database](https://www.maxmind.com/en/geoip2-databases)
and uploads them to S3 for Ophan_

Ophan uses geolocation by IP address to work out where in the world our readers are.
[MaxMind](https://www.maxmind.com/) provide the proprietary GeoIP database we use to
perform that lookup.

## Steps performed by the Refresher

1. Download the database archive file (`.tar.gz`) from the MaxMind permalink url, along
   with the archive's SHA256 hash (`.tar.gz.sha256`).
2. Compute the SHA256 hash of the downloaded archive, verify it matches the stated hash.
3. Inspect the database archive file to find the archive entry that corresponds to the
   database file (`GeoIP2-City.mmdb`) itself.
4. Stream the archive entry directly from the archive up to S3, where it can be used by
   the Slab, and accessed by our tests running on TeamCity.

## Running the Refresher locally

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

...you should see output [a bit like this](https://gist.github.com/rtyley/9e6e0c0b3e371e25bf177eccce3f8d65).

Note that the `GeoIP2-City.mmdb` is quite large, so for quicker runs you may want to
use the [`GeoIP2-Country.mmdb`](https://github.com/guardian/ophan-geoip-db-refresher/blob/916525c6082615401054d51d8888a7df72140a03/src/main/scala/ophan/geoip/db/refresher/MaxmindDatabaseEdition.scala#L46-L47)
file [instead](https://github.com/guardian/ophan-geoip-db-refresher/blob/916525c6082615401054d51d8888a7df72140a03/src/main/scala/ophan/geoip/db/refresher/Lambda.scala#L16).
