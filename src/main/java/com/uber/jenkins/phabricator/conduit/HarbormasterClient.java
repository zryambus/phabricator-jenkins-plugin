package com.uber.jenkins.phabricator.conduit;

import com.uber.jenkins.phabricator.lint.LintResults;
import com.uber.jenkins.phabricator.unit.UnitResults;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HarbormasterClient {

    /**
     * See https://secure.phabricator.com/conduit/method/harbormaster.sendmessage/
     */
    public enum MessageType {
        pass,
        fail,
        work,
    }

    private final ConduitAPIClient conduit;

    public HarbormasterClient(ConduitAPIClient conduit) {
        this.conduit = conduit;
    }

    /**
     * Sets a sendHarbormasterMessage build status
     *
     * @param phid Phabricator object ID
     * @param messageType type of message to send; either 'pass', 'fail' or 'work'
     * @param unitResults the results from the unit tests
     * @param coverage the results from the coverage provider
     * @param lintResults
     * @return the Conduit API response
     * @throws IOException if there is a network error talking to Conduit
     * @throws ConduitAPIException if any error is experienced talking to Conduit
     */
    public JSONObject sendHarbormasterMessage(
            String phid,
            MessageType messageType,
            UnitResults unitResults,
            Map<String, String> coverage,
            LintResults lintResults) throws ConduitAPIException, IOException {

        List<JSONObject> unit = new ArrayList<>();

        if (unitResults != null) {
            unit.addAll(unitResults.toHarbormaster());
        }

        List<JSONObject> lint = new ArrayList<>();

        if (lintResults != null) {
            lint.addAll(lintResults.toHarbormaster());
        }

        if (coverage != null) {
            JSONObject coverageUnit = new JSONObject();
            coverageUnit.put("result", "pass");
            coverageUnit.put("name", "Coverage Data");
            coverageUnit.put("coverage", coverage);
            unit.add(coverageUnit);
        }

        JSONObject params = new JSONObject();
        params.put("type", messageType.name());
        params.put("buildTargetPHID", phid);

        if (!unit.isEmpty()) {
            params.put("unit", unit);
        }

        if (!lint.isEmpty()) {
            params.put("lint", lint);
        }

        return conduit.perform("harbormaster.sendmessage", params);
    }

    /**
     * Uploads a uri as an 'artifact' for Harbormaster to display
     *
     * @param phid Phabricator object ID
     * @param buildUri Uri to display, presumably the jenkins builds
     * @return the Conduit API response
     * @throws IOException if there is a network error talking to Conduit
     * @throws ConduitAPIException if any error is experienced talking to Conduit
     */
    public JSONObject sendHarbormasterUri(String phid, String buildUri) throws ConduitAPIException, IOException {
        JSONObject artifactData = new JSONObject();
        artifactData.put("uri", buildUri);
        artifactData.put("name", "Jenkins");
        artifactData.put("ui.external", true);

        JSONObject params = new JSONObject();
        params.put("buildTargetPHID", phid);
        params.put("artifactKey", "jenkins.uri");
        params.put("artifactType", "uri");
        params.put("artifactData", artifactData);

        return conduit.perform("harbormaster.createartifact", params);
    }
}
