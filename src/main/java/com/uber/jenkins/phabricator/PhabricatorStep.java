package com.uber.jenkins.phabricator;

import java.io.IOException;
import java.io.Serial;
import java.util.Set;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.tasks.SimpleBuildStep;

/**
 * Pipeline step for posting build results to Phabricator.
 *
 * <pre>
 * phabricatorStep(
 *     commentOnSuccess: true,
 *     uberallsEnabled: false,
 *     coverageCheck: false
 * )
 * </pre>
 */
public class PhabricatorStep extends Step {

    private boolean commentOnSuccess;
    private boolean uberallsEnabled;
    private boolean coverageCheck;
    private double coverageThreshold;
    private double minCoverageThreshold;
    private String coverageReportPattern;
    private boolean preserveFormatting = true;
    private String commentFile;
    private String commentSize;
    private boolean commentWithConsoleLinkOnFailure;
    private boolean customComment;
    private boolean processLint;
    private String lintFile;
    private String lintFileSize;
    private boolean sendPartialResults;

    @DataBoundConstructor
    public PhabricatorStep() {
    }

    public boolean isCommentOnSuccess() {
        return commentOnSuccess;
    }

    @DataBoundSetter
    public void setCommentOnSuccess(boolean commentOnSuccess) {
        this.commentOnSuccess = commentOnSuccess;
    }

    public boolean isUberallsEnabled() {
        return uberallsEnabled;
    }

    @DataBoundSetter
    public void setUberallsEnabled(boolean uberallsEnabled) {
        this.uberallsEnabled = uberallsEnabled;
    }

    public boolean isCoverageCheck() {
        return coverageCheck;
    }

    @DataBoundSetter
    public void setCoverageCheck(boolean coverageCheck) {
        this.coverageCheck = coverageCheck;
    }

    public double getCoverageThreshold() {
        return coverageThreshold;
    }

    @DataBoundSetter
    public void setCoverageThreshold(double coverageThreshold) {
        this.coverageThreshold = coverageThreshold;
    }

    public double getMinCoverageThreshold() {
        return minCoverageThreshold;
    }

    @DataBoundSetter
    public void setMinCoverageThreshold(double minCoverageThreshold) {
        this.minCoverageThreshold = minCoverageThreshold;
    }

    public String getCoverageReportPattern() {
        return coverageReportPattern;
    }

    @DataBoundSetter
    public void setCoverageReportPattern(String coverageReportPattern) {
        this.coverageReportPattern = coverageReportPattern;
    }

    public boolean isPreserveFormatting() {
        return preserveFormatting;
    }

    @DataBoundSetter
    public void setPreserveFormatting(boolean preserveFormatting) {
        this.preserveFormatting = preserveFormatting;
    }

    public String getCommentFile() {
        return commentFile;
    }

    @DataBoundSetter
    public void setCommentFile(String commentFile) {
        this.commentFile = commentFile;
    }

    public String getCommentSize() {
        return commentSize;
    }

    @DataBoundSetter
    public void setCommentSize(String commentSize) {
        this.commentSize = commentSize;
    }

    public boolean isCommentWithConsoleLinkOnFailure() {
        return commentWithConsoleLinkOnFailure;
    }

    @DataBoundSetter
    public void setCommentWithConsoleLinkOnFailure(boolean commentWithConsoleLinkOnFailure) {
        this.commentWithConsoleLinkOnFailure = commentWithConsoleLinkOnFailure;
    }

    public boolean isCustomComment() {
        return customComment;
    }

    @DataBoundSetter
    public void setCustomComment(boolean customComment) {
        this.customComment = customComment;
    }

    public boolean isProcessLint() {
        return processLint;
    }

    @DataBoundSetter
    public void setProcessLint(boolean processLint) {
        this.processLint = processLint;
    }

    public String getLintFile() {
        return lintFile;
    }

    @DataBoundSetter
    public void setLintFile(String lintFile) {
        this.lintFile = lintFile;
    }

    public String getLintFileSize() {
        return lintFileSize;
    }

    @DataBoundSetter
    public void setLintFileSize(String lintFileSize) {
        this.lintFileSize = lintFileSize;
    }

    public boolean isSendPartialResults() {
        return sendPartialResults;
    }

    @DataBoundSetter
    public void setSendPartialResults(boolean sendPartialResults) {
        this.sendPartialResults = sendPartialResults;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new PhabricatorStepExecution(this, context);
    }

    @Override
    public StepDescriptor getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    private static class PhabricatorStepExecution extends SynchronousNonBlockingStepExecution<Void> {

        @Serial
        private static final long serialVersionUID = 1L;

        private final transient PhabricatorStep step;

        PhabricatorStepExecution(PhabricatorStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            Run<?, ?> build = getContext().get(Run.class);
            FilePath workspace = getContext().get(FilePath.class);
            TaskListener listener = getContext().get(TaskListener.class);
            Launcher launcher = getContext().get(Launcher.class);

            PhabricatorNotifier notifier = new PhabricatorNotifier(
                    step.commentOnSuccess,
                    step.uberallsEnabled,
                    step.coverageCheck,
                    step.coverageThreshold,
                    step.minCoverageThreshold,
                    step.coverageReportPattern,
                    step.preserveFormatting,
                    step.commentFile,
                    step.commentSize,
                    step.commentWithConsoleLinkOnFailure,
                    step.customComment,
                    step.processLint,
                    step.lintFile,
                    step.lintFileSize,
                    step.sendPartialResults
            );

            notifier.perform(build, workspace, launcher, listener);
            return null;
        }
    }

    @Symbol("phabricatorStep")
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "phabricatorStep";
        }

        @Override
        public String getDisplayName() {
            return "Post to Phabricator";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Set.of(Run.class, FilePath.class, Launcher.class, TaskListener.class, EnvVars.class);
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return false;
        }
    }
}
