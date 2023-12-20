import "source-map-support/register";
import {GuRootExperimental} from "@guardian/cdk/lib/experimental/constructs/root";
import type { App } from "aws-cdk-lib";
import { GoogleSearchIndexingObservatory } from "../lib/google-search-indexing-observatory";

const app: App = new GuRootExperimental();
new GoogleSearchIndexingObservatory(app, "GoogleSearchIndexingObservatory-PROD", {
    stack: "ophan",
    stage: "PROD",
    env: {region: "eu-west-1"},
    withBackup: true
});
