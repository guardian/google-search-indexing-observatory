import { App } from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { GoogleSearchIndexChecker } from "./google-search-index-checker";

describe("The GoogleSearchIndexChecker stack", () => {
  it("matches the snapshot", () => {
    const app = new App();
    const stack = new GoogleSearchIndexChecker(app, "GoogleSearchIndexChecker", { stack: "ophan", stage: "TEST" });
    const template = Template.fromStack(stack);
    expect(template.toJSON()).toMatchSnapshot();
  });
});
