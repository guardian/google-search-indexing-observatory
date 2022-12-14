import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { GuStack } from "@guardian/cdk/lib/constructs/core";
import type { App } from "aws-cdk-lib";
import {GuScheduledLambda} from "@guardian/cdk";
import {Runtime} from "aws-cdk-lib/aws-lambda";
import {Schedule} from "aws-cdk-lib/aws-events";
import {Duration} from "aws-cdk-lib";
import {NoMonitoring} from "@guardian/cdk/lib/constructs/cloudwatch";
import {AttributeType, Table} from "aws-cdk-lib/aws-dynamodb";

export class GoogleSearchIndexingObservatory extends GuStack {
  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);
    const noMonitoring: NoMonitoring = { noMonitoring: true };
    const scheduledLambda = new GuScheduledLambda(this, "scheduledLambda", {
      app: "google-search-indexing-observatory",
      fileName: "google-search-indexing-observatory.jar",
      handler: "ophan.google.indexing.observatory.Lambda::handler",
      monitoringConfiguration: noMonitoring,
      rules: [{ schedule: Schedule.rate(Duration.minutes(1)) }],
      runtime: Runtime.JAVA_11
    })
    const table = new Table(this, 'Table', {
      partitionKey: {
        name: 'uri',
        type: AttributeType.STRING,
      },
    });
    table.grantReadWriteData(scheduledLambda)
  }
}
