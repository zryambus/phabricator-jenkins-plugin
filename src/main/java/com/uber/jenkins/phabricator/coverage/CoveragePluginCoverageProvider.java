// Copyright (c) 2025
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.uber.jenkins.phabricator.coverage;

import java.io.File;
import java.util.Optional;
import java.util.Set;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Value;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.steps.CoverageBuildAction;

/**
 * Provide coverage data via the Jenkins Coverage Plugin (io.jenkins.plugins.coverage).
 */
public class CoveragePluginCoverageProvider extends XmlCoverageProvider {

    private final CoverageBuildAction buildAction;

    public CoveragePluginCoverageProvider(Set<File> coverageReports, Set<String> includeFiles, CoverageBuildAction buildAction) {
        super(coverageReports, includeFiles);
        this.buildAction = buildAction;
    }

    @Override
    protected void computeMetrics() {
        metrics = convertCoverage(buildAction);
    }

    static CodeCoverageMetrics convertCoverage(CoverageBuildAction action) {
        if (action == null) {
            return null;
        }

        float lineCoverage = getMetricPercentage(action, Metric.LINE);
        float branchCoverage = getMetricPercentage(action, Metric.BRANCH);
        float methodCoverage = getMetricPercentage(action, Metric.METHOD);
        float classCoverage = getMetricPercentage(action, Metric.CLASS);
        float fileCoverage = getMetricPercentage(action, Metric.FILE);
        float packageCoverage = getMetricPercentage(action, Metric.PACKAGE);
        long linesCovered = getMetricCount(action, Metric.LINE);
        long totalLines = getMetricTotalCount(action, Metric.LINE);

        return new CodeCoverageMetrics(
                packageCoverage,
                fileCoverage,
                classCoverage,
                methodCoverage,
                lineCoverage,
                branchCoverage,
                linesCovered,
                totalLines
        );
    }

    private static float getMetricPercentage(CoverageBuildAction action, Metric metric) {
        Optional<Value> value = action.getValueForMetric(Baseline.PROJECT, metric);
        if (value.isPresent()) {
            return (float) (value.get().asDouble() * 100);
        }
        return -1;
    }

    private static long getMetricCount(CoverageBuildAction action, Metric metric) {
        Optional<Value> value = action.getValueForMetric(Baseline.PROJECT, metric);
        if (value.isPresent()) {
            return value.get().getFraction().getNumerator();
        }
        return 0;
    }

    private static long getMetricTotalCount(CoverageBuildAction action, Metric metric) {
        Optional<Value> value = action.getValueForMetric(Baseline.PROJECT, metric);
        if (value.isPresent()) {
            return value.get().getFraction().getDenominator();
        }
        return 0;
    }
}
