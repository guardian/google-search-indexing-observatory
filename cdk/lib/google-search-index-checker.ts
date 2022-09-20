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
import {Table} from "aws-cdk-lib/aws-dynamodb";
import {AttributeType} from "aws-cdk-lib/aws-dynamodb/lib/table";
import {
  Effect,
  OrganizationPrincipal,
  PolicyStatement,
} from "@aws-cdk/aws-iam";

export class GoogleSearchIndexChecker extends GuStack {
  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);
    const noMonitoring: NoMonitoring = { noMonitoring: true };
    const lambda = new GuScheduledLambda(this, "scheduledLambda", {
      app: "google-search-index-checker",
      fileName: "google-search-index-checker.jar",
      handler: "ophan.google.index.checker.Lambda::handler",
      monitoringConfiguration: noMonitoring,
      rules: [{ schedule: Schedule.rate(Duration.minutes(2)) }],
      runtime: Runtime.JAVA_11
    })

    const table = new Table(this, "AvailabilityTable", {
      partitionKey: {
        name: "capiId",
        type: AttributeType.STRING
      },
      tableName: `google-search-index-availability`
    });

    lambda.addToRolePolicy(
        new PolicyStatement({
          effect: Effect.ALLOW,
          resources: [table.tableArn],
          actions: ["dynamodb:BatchGetItem", "dynamodb:UpdateItem"]
        })
    );

  }
}
