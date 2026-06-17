package com.uber.jenkins.phabricator.tasks;

import com.uber.jenkins.phabricator.conduit.ConduitAPIException;
import com.uber.jenkins.phabricator.conduit.DifferentialClient;
import com.uber.jenkins.phabricator.conduit.HarbormasterClient.MessageType;
import com.uber.jenkins.phabricator.lint.LintResults;
import com.uber.jenkins.phabricator.unit.UnitResults;
import com.uber.jenkins.phabricator.utils.Logger;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class SendHarbormasterResultTask extends Task {

    private final DifferentialClient diffClient;
    private final String phid;
    private final MessageType messageType;
    private final Map<String, String> coverage;
    private final LintResults lintResults;
    private UnitResults unitResults;

    public SendHarbormasterResultTask(
            Logger logger, DifferentialClient diffClient, String phid,
            MessageType messageType, UnitResults unitResults,
            Map<String, String> harbormasterCoverage,
            LintResults lintResults) {
        super(logger);
        this.diffClient = diffClient;
        this.phid = phid;
        this.messageType = messageType;
        this.unitResults = unitResults;
        this.coverage = harbormasterCoverage;
        this.lintResults = lintResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getTag() {
        return "send-harbormaster-result";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setup() {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void execute() {
        try {
            if (!sendMessage(unitResults, coverage, lintResults)) {
                info("Error sending Harbormaster unit results, trying again without unit data (you may have an old Phabricator?).");
                sendMessage(null, null, null);
            }
        } catch (ConduitAPIException e) {
            printStackTrace(e);
            failTask();
        } catch (IOException e) {
            printStackTrace(e);
            failTask();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void tearDown() {
        // Do nothing
    }

    /**
     * Try to send a message to harbormaster
     *
     * @param unitResults the unit testing results to send
     * @param coverage the coverage data to send
     * @return false if an error was encountered
     */
    private boolean sendMessage(UnitResults unitResults, Map<String, String> coverage, LintResults lintResults) throws
            IOException, ConduitAPIException {
        JSONObject result = diffClient.sendHarbormasterMessage(phid, messageType, unitResults, coverage,
                lintResults);

        if (result.has("error_info") && !result.isNull("error_info")) {
            String errorInfo = result.optString("error_info", null);
            if (errorInfo != null && !"null".equals(errorInfo)) {
                info(String.format("Error from Harbormaster: %s", errorInfo));
                failTask();
                return false;
            }
        }
        this.result = Result.SUCCESS;
        return true;
    }

    private void failTask() {
        info("Unable to post to Harbormaster");
        result = result.FAILURE;
    }
}
