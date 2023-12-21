import { App } from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { GoogleSearchIndexingObservatory } from "./google-search-indexing-observatory";

describe("The GoogleSearchIndexingObservatory stack", () => {
  it("matches the snapshot", () => {
    const app = new App();
    const stack = new GoogleSearchIndexingObservatory(app, "GoogleSearchIndexingObservatory", {
      stack: "ophan",
      stage: "TEST",
      withBackup: true,
    });
    const template = Template.fromStack(stack);
    expect(template.toJSON()).toMatchSnapshot();
  });
});
