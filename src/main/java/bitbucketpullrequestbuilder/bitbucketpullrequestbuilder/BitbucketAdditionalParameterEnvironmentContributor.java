package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;

import java.io.IOException;
import java.util.ArrayList;

@Extension
public class BitbucketAdditionalParameterEnvironmentContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(Run run, EnvVars envVars, TaskListener taskListener)
            throws IOException, InterruptedException {

        BitbucketCause cause = (BitbucketCause) run.getCause(BitbucketCause.class);
        if (cause == null) {
            return;
        }

        run.addAction(getDefaultParameters(run));

        putEnvVar(envVars, "sourceBranch", cause.getSourceBranch());
        putEnvVar(envVars, "targetBranch", cause.getTargetBranch());
        putEnvVar(envVars, "repositoryOwner", cause.getRepositoryOwner());
        putEnvVar(envVars, "repositoryName", cause.getRepositoryName());
        putEnvVar(envVars, "pullRequestId", cause.getPullRequestId());
        putEnvVar(envVars, "destinationRepositoryOwner", cause.getDestinationRepositoryOwner());
        putEnvVar(envVars, "destinationRepositoryName", cause.getDestinationRepositoryName());
        putEnvVar(envVars, "pullRequestTitle", cause.getPullRequestTitle());
        putEnvVar(envVars, "pullRequestAuthor", cause.getPullRequestAuthor());

    }

    private static void putEnvVar(EnvVars envs, String name, String value) {
        envs.put(name, getString(value, ""));
    }

    private static String getString(String actual, String d) {
        return actual == null ? d : actual;
    }

    private ParametersAction getDefaultParameters(Run<?, ?> run) {
        ArrayList<ParameterValue> values = new ArrayList<ParameterValue>();
        ParametersDefinitionProperty definitionProperty = run.getParent().getProperty(ParametersDefinitionProperty.class);
        if (definitionProperty != null) {
            for (ParameterDefinition pd : definitionProperty.getParameterDefinitions()) {
                values.add(pd.getDefaultParameterValue());
            }
        }
        return new ParametersAction(values);
    }
}
