package com.uber.jenkins.phabricator.coverage;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Value;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.coverage.metrics.steps.CoverageBuildAction;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoveragePluginCoverageProviderTest {

    @Test
    public void conversion() {
        CoverageBuildAction action = getMockAction();
        CodeCoverageMetrics metrics = CoveragePluginCoverageProvider.convertCoverage(action);
        assertEquals(75.0f, metrics.getLineCoveragePercent(), 0.0f);
        assertEquals(80.0f, metrics.getConditionalCoveragePercent(), 0.0f);
        assertEquals(60.0f, metrics.getMethodCoveragePercent(), 0.0f);
    }

    @Test
    public void conversionWithNullAction() {
        CodeCoverageMetrics metrics = CoveragePluginCoverageProvider.convertCoverage(null);
        assertEquals(null, metrics);
    }

    private CoverageBuildAction getMockAction() {
        CoverageBuildAction action = mock(CoverageBuildAction.class);
        when(action.getValueForMetric(Baseline.PROJECT, Metric.LINE))
                .thenReturn(Optional.of(new Value(Metric.LINE, 75, 100)));
        when(action.getValueForMetric(Baseline.PROJECT, Metric.BRANCH))
                .thenReturn(Optional.of(new Value(Metric.BRANCH, 80, 100)));
        when(action.getValueForMetric(Baseline.PROJECT, Metric.METHOD))
                .thenReturn(Optional.of(new Value(Metric.METHOD, 60, 100)));
        when(action.getValueForMetric(Baseline.PROJECT, Metric.CLASS))
                .thenReturn(Optional.of(new Value(Metric.CLASS, 70, 100)));
        when(action.getValueForMetric(Baseline.PROJECT, Metric.FILE))
                .thenReturn(Optional.of(new Value(Metric.FILE, 90, 100)));
        when(action.getValueForMetric(Baseline.PROJECT, Metric.PACKAGE))
                .thenReturn(Optional.of(new Value(Metric.PACKAGE, 85, 100)));
        return action;
    }
}
