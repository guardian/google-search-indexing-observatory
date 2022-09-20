import "source-map-support/register";
import { App } from "aws-cdk-lib";
import { GoogleSearchIndexChecker } from "../lib/google-search-index-checker";

const app = new App();
new GoogleSearchIndexChecker(app, "GoogleSearchIndexChecker-PROD", { stack: "ophan", stage: "PROD" });
