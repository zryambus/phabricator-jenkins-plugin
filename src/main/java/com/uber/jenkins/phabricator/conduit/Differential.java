// Copyright (c) 2015 Uber Technologies, Inc.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.uber.jenkins.phabricator.conduit;

import com.uber.jenkins.phabricator.PhabricatorPostbuildAction;
import com.uber.jenkins.phabricator.PhabricatorPostbuildSummaryAction;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import hudson.model.Run;

public class Differential {

    private static final String UNKNOWN_AUTHOR = "unknown";
    private static final String UNKNOWN_EMAIL = "unknown";

    private final JSONObject rawJSON;
    private String commitMessage;

    public Differential(JSONObject rawJSON) {
        this.rawJSON = rawJSON;
    }

    public String getDiffID() {
        if (!rawJSON.has("id")) {
            return null;
        }
        Object rawDiffId = rawJSON.opt("id");
        if (rawDiffId == null || rawDiffId == JSONObject.NULL) {
            return null;
        }
        String diffId = rawDiffId.toString();
        if (diffId.isEmpty()) {
            return null;
        }
        return diffId;
    }

    public String getRevisionID(boolean formatted) {
        if (!rawJSON.has("revisionID")) {
            return "";
        }
        Object rawRevisionIdObj = rawJSON.opt("revisionID");
        String rawRevisionId;
        if (rawRevisionIdObj == null || rawRevisionIdObj == JSONObject.NULL) {
            return "";
        }
        rawRevisionId = rawRevisionIdObj.toString();
        if (rawRevisionId.isEmpty()) {
            return "";
        }
        if (formatted) {
            return String.format("D%s", rawRevisionId);
        }
        return rawRevisionId;
    }

    public String getPhabricatorLink(String phabricatorURL) {
        String revisionID = getRevisionID(true);
        try {
            URI base = URI.create(phabricatorURL);
            return base.resolve(revisionID).toString();
        } catch (IllegalArgumentException e) {
            return String.format("%s%s", phabricatorURL, revisionID);
        }
    }

    public void decorate(Run<?, ?> build, String phabricatorURL) {
        // Add a badge next to the build
        build.addAction(PhabricatorPostbuildAction.createShortText(
                getRevisionID(true),
                getPhabricatorLink(phabricatorURL)));
        // Add some long-form text
        PhabricatorPostbuildSummaryAction summary = createSummary(phabricatorURL);
        build.addAction(summary);

    }

    public PhabricatorPostbuildSummaryAction createSummary(String phabricatorURL) {
        return new PhabricatorPostbuildSummaryAction(
                "phabricator.png",
                getPhabricatorLink(phabricatorURL),
                getDiffID(),
                getRevisionID(true),
                getAuthorName(),
                getAuthorEmail(),
                getCommitMessage()
        );
    }

    private String getAuthorName() {
        return getOrElse(rawJSON, "authorName", UNKNOWN_AUTHOR);
    }

    public String getAuthorEmail() {
        return getOrElse(rawJSON, "authorEmail", UNKNOWN_EMAIL);
    }

    private String getOrElse(JSONObject json, String key, String orElse) {
        if (json.has(key)) {
            return json.getString(key);
        }
        return orElse;
    }

    public String getBaseCommit() {
        Object baseCommit = rawJSON.get("sourceControlBaseRevision");
        String commit = "(none)";
        if (baseCommit instanceof String) {
            commit = baseCommit.toString();
        } else if (baseCommit != null) {
            commit = baseCommit.toString();
        }
        return commit;
    }

    /**
     * Return the local branch name
     *
     * @return the name of the branch, or unknown
     */
    public String getBranch() {
        Object branchName = rawJSON.opt("branch");
        String branch;
        if (branchName == null || branchName == JSONObject.NULL) {
            branch = "(none)";
        } else if (branchName instanceof String) {
            branch = branchName.toString();
        } else {
            branch = "(unknown)";
        }
        if (branch.isEmpty()) {
            return "(none)";
        }
        return branch;
    }

    /**
     * Get the differential commit message.
     *
     * @return the differential commit message.
     */
    public String getCommitMessage() {
        return commitMessage;
    }

    /**
     * Set the differential commit message.
     *
     * @param commitMesasge the differential commit message.
     */
    public void setCommitMessage(String commitMesasge) {
        this.commitMessage = commitMesasge;
    }

    /**
     * Get the list of changed files in the diff.
     *
     * @return the list of changed files in the diff.
     */
    public Set<String> getChangedFiles() {
        Set<String> changedFiles = new HashSet<String>();
        JSONArray changes = rawJSON.getJSONArray("changes");
        for (int i = 0; i < changes.length(); i++) {
            JSONObject change = changes.getJSONObject(i);
            String file = (String) change.get("currentPath");
            if (file != null) {
                changedFiles.add(file);
            }
        }
        return changedFiles;
    }
}
