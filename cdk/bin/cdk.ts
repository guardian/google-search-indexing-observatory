import "source-map-support/register";
import { App } from "aws-cdk-lib";
import { GoogleSearchIndexingObservatory } from "../lib/google-search-indexing-observatory";
import {GuRootExperimental} from "@guardian/cdk/lib/experimental/constructs/root";

const app: App = new GuRootExperimental();
new GoogleSearchIndexingObservatory(app, "GoogleSearchIndexingObservatory-PROD", { stack: "ophan", stage: "PROD", env: {region: "eu-west-1"} });
