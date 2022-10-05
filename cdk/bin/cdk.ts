import "source-map-support/register";
import { App } from "aws-cdk-lib";
import { GoogleSearchIndexChecker } from "../lib/google-search-indexing-observatory";
import {GuRootExperimental} from "@guardian/cdk/lib/experimental/constructs/root";

const app: App = new GuRootExperimental();
new GoogleSearchIndexChecker(app, "GoogleSearchIndexChecker-PROD", { stack: "ophan", stage: "PROD", env: {region: "eu-west-1"} });
