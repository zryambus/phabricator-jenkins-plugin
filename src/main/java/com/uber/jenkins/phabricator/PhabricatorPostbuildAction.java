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

package com.uber.jenkins.phabricator;

import org.kohsuke.stapler.export.Exported;

import hudson.model.BuildBadgeAction;

/**
 * Ripped from https://github.com/jenkinsci/groovy-postbuild-plugin/blob/master/src/main/java/org/jvnet/hudson/plugins/groovypostbuild/GroovyPostbuildAction.java
 */
public class PhabricatorPostbuildAction implements BuildBadgeAction {

    private final String iconPath;
    private final String text;
    private static final String COLOR = "#1FBAD6";
    private static final String BACKGROUND = "transparent";
    private static final String BORDER = "0";
    private static final String BORDER_COLOR = "transparent";
    private final String link;

    private PhabricatorPostbuildAction(String text, String link) {
        this.iconPath = null;
        this.text = text;
        this.link = link;
    }

    public static PhabricatorPostbuildAction createShortText(String text, String link) {
        return new PhabricatorPostbuildAction(text, link);
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "";
    }

    /* Action methods */
    public String getUrlName() {
        return "";
    }

    @Exported
    public boolean isTextOnly() {
        return (iconPath == null);
    }

    @Exported
    public String getIconPath() {
        return iconPath;
    }

    @Exported
    public String getText() {
        return text;
    }

    @Exported
    public String getColor() {
        return COLOR;
    }

    @Exported
    public String getBackground() {
        return BACKGROUND;
    }

    @Exported
    public String getBorder() {
        return BORDER;
    }

    @Exported
    public String getBorderColor() {
        return BORDER_COLOR;
    }

    @Exported
    public String getLink() {
        return link;
    }
}
