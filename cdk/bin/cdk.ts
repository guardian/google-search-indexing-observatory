import "source-map-support/register";
import {GuRoot} from "@guardian/cdk/lib/constructs/root";
import type { App } from "aws-cdk-lib";
import { GoogleSearchIndexingObservatory } from "../lib/google-search-indexing-observatory";

const app: App = new GuRoot();
new GoogleSearchIndexingObservatory(app, "GoogleSearchIndexingObservatory-PROD", {
    stack: "ophan",
    stage: "PROD",
    env: {region: "eu-west-1"},
    withBackup: true
});
