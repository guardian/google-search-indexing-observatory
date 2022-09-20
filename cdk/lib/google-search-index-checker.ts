import { join } from "path";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { GuStack } from "@guardian/cdk/lib/constructs/core";
import type { App } from "aws-cdk-lib";
import { CfnInclude } from "aws-cdk-lib/cloudformation-include";
import {GuScheduledLambda} from "@guardian/cdk";
import {Runtime} from "aws-cdk-lib/aws-lambda";
import {Schedule} from "aws-cdk-lib/aws-events";
import {Duration} from "aws-cdk-lib";
import {NoMonitoring} from "@guardian/cdk/lib/constructs/cloudwatch";

export class GoogleSearchIndexChecker extends GuStack {
  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);
    const noMonitoring: NoMonitoring = { noMonitoring: true };
    new GuScheduledLambda(this, "scheduledLambda", {
      app: "google-search-index-checker",
      fileName: "google-search-index-checker.jar",
      handler: "ophan.google.index.checker.Lambda::handler",
      monitoringConfiguration: noMonitoring,
      rules: [{ schedule: Schedule.rate(Duration.minutes(1)) }],
      runtime: Runtime.JAVA_11
    })
  }
}
