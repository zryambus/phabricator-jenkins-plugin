package com.uber.jenkins.phabricator.coverage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XmlCoverageProviderTest {

    private static final String TEST_COVERAGE_FILE = "go-torch-coverage.xml";
    private static final String TEST_COVERAGE_FILE_1 = "go-torch-coverage1.xml";
    private static final String TEST_COVERAGE_FILE_2 = "go-torch-coverage2.xml";
    private static final String TEST_COVERAGE_FILE_3 = "go-torch-coverage3.xml";
    private static final String TEST_COVERAGE_FILE_MULTIPLE_INCLUDE = "multiple-include-coverage.xml";
    private static final String TEST_COVERAGE_FILE_INVALID = "invalid-coverage.xml";
    private static final String XXE_MARKER = "XXE_SECRET_MARKER_42";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void emptyCoverage() {
        CoverageProvider provider = new XmlCoverageProvider(Collections.emptySet());
        assertFalse(provider.hasCoverage());
    }

    @Test
    public void cobertura() {
        CoverageProvider provider = new XmlCoverageProvider(getResources(TEST_COVERAGE_FILE));
        assertTrue(provider.hasCoverage());

        Map<String, List<Integer>> coverage = provider.getLineCoverage();
        assertEquals(1, coverage.get("github.com/uber/go-torch/visualization/visualization.go").get(66).longValue());
        assertEquals(new CodeCoverageMetrics(100.0f, 100.0f, 86.666664f, 83.33333f, 89.69697f, 100.0f, 148, 165),
                provider.getMetrics());
    }

    @Test
    public void jacoco() {
        CoverageProvider provider = new XmlCoverageProvider(getResources("jacoco-coverage.xml"));

        assertTrue(provider.hasCoverage());
        Map<String, List<Integer>> coverage = provider.getLineCoverage();
        assertEquals(1, coverage.get("com/uber/nullaway/jarinfer/StubxWriter.java").get(72).longValue());
        assertEquals(0, coverage.get("com/uber/nullaway/jarinfer/StubxWriter.java").get(73).longValue());
        assertEquals(new CodeCoverageMetrics(100.0f, 100.0f, 100.f, 92.59259f, 90.10989f, 69.09091f, 328, 364),
                provider.getMetrics());
    }

    @Test
    public void cloverPhpunit() {
        CoverageProvider provider = new XmlCoverageProvider(getResources("clover-phpunit-coverage.xml"));

        assertTrue(provider.hasCoverage());

        Map<String, List<Integer>> lineCoverage = provider.getLineCoverage();
        String expectedKey = "/home/ubuntu/example-php/src/Example/Example.php";

        assertNull(lineCoverage.get(expectedKey).get(4));
        assertEquals(1, lineCoverage.get(expectedKey).get(6).longValue());
        assertEquals(0, lineCoverage.get(expectedKey).get(7).longValue());
        assertEquals(1, lineCoverage.get(expectedKey).get(10).longValue());
        assertEquals(new CodeCoverageMetrics(100.0f, 100.0f, 100.f, 100.0f, 66.66667f, 100.0f, 2, 3),
                provider.getMetrics());
    }

    @Test public void cloverWithIncludeFiles() {
        CoverageProvider provider = new XmlCoverageProvider(getResources("clover-phpunit-coverage.xml"),
                Collections.singleton("src/Example/Example.php"));

        assertTrue(provider.hasCoverage());

        Map<String, List<Integer>> lineCoverage = provider.getLineCoverage();
        List<Integer> exampleCoverage = lineCoverage.get("src/Example/Example.php");
        assertNotNull(exampleCoverage);
    }

    @Test
    public void lineCoverageAggregation() {
        CoverageProvider provider = new XmlCoverageProvider(getResources(
                TEST_COVERAGE_FILE_1,
                TEST_COVERAGE_FILE_2,
                TEST_COVERAGE_FILE_3));

        Map<String, List<Integer>> lineCoverage = provider.getLineCoverage();
        List<Integer> mainCoverage = lineCoverage.get("github.com/uber/go-torch/main.go");
        assertEquals(246, mainCoverage.size());
        assertNull(mainCoverage.get(0));
        assertNull(mainCoverage.get(1));
        assertEquals(1, mainCoverage.get(78).longValue());
        assertNull(mainCoverage.get(79));
        assertEquals(1, mainCoverage.get(85).longValue());
        assertEquals(0, mainCoverage.get(102).longValue());

        List<Integer> graphCoverage = lineCoverage.get("github.com/uber/go-torch/graph/graph.go");
        assertEquals(1, graphCoverage.get(234).longValue());
        assertNull(graphCoverage.get(235));
    }

    @Test
    public void lineCoverageWithIncludes() {
        CoverageProvider provider = new XmlCoverageProvider(getResources(TEST_COVERAGE_FILE),
                Collections.singleton("github.com/uber/go-torch/main.go"));

        Map<String, List<Integer>> lineCoverage = provider.getLineCoverage();
        List<Integer> mainCoverage = lineCoverage.get("github.com/uber/go-torch/main.go");
        assertEquals(1, mainCoverage.get(212).longValue());

        List<Integer> graphCoverage = lineCoverage.get("github.com/uber/go-torch/graph.go");
        assertNull(graphCoverage);
    }

    @Test
    public void lineCoverageWithMultipleIncludes() {
        CoverageProvider provider = new XmlCoverageProvider(getResources(TEST_COVERAGE_FILE_MULTIPLE_INCLUDE),
                new HashSet<>(Arrays.asList("com/uber/jenkins/phabricator/packageA/Greet.java", "com/uber/jenkins"
                        + "/phabricator/packageB/Greet.java", "eet.java", "kageB/Greet.java")));

        Map<String, List<Integer>> lineCoverage = provider.getLineCoverage();
        List<Integer> greetACoverage = lineCoverage.get("com/uber/jenkins/phabricator/packageA/Greet.java");
        List<Integer> greetBCoverage = lineCoverage.get("com/uber/jenkins/phabricator/packageB/Greet.java");
        List<Integer> eetCoverage = lineCoverage.get("eet.java");
        List<Integer> partialMatchCoverage = lineCoverage.get("kageB/Greet.java");
        assertEquals(0, greetACoverage.get(6).longValue());
        assertEquals(1, greetBCoverage.get(6).longValue());
        assertNull(eetCoverage);
        assertNull(partialMatchCoverage);
    }

    @Test(expected = IllegalStateException.class)
    public void invalidCoverage() {
        CoverageProvider provider = new XmlCoverageProvider(getResources(TEST_COVERAGE_FILE_INVALID));
        provider.getLineCoverage();
    }

    @Test
    public void xxeExternalGeneralEntityBlocked() throws Exception {
        File secretFile = tmp.newFile("secret.txt");
        FileUtils.write(secretFile, XXE_MARKER, StandardCharsets.UTF_8);

        String maliciousXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE foo [\n" +
                "  <!ENTITY xxe SYSTEM \"file://" + secretFile.getAbsolutePath() + "\">\n" +
                "]>\n" +
                "<coverage>\n" +
                "  <packages>\n" +
                "    <package name=\"test\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "      <classes>\n" +
                "        <class name=\"Test\" filename=\"Test.java\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "          <lines>\n" +
                "            <line number=\"1\" hits=\"1\"/>\n" +
                "          </lines>\n" +
                "        </class>\n" +
                "      </classes>\n" +
                "    </package>\n" +
                "  </packages>\n" +
                "  &xxe;\n" +
                "</coverage>";

        File xxeFile = writeTempFile("xxe-external-entity.xml", maliciousXml);
        String parsedXml = parseWithPluginParser(xxeFile);
        assertFalse("External general entity content must not be resolved",
                parsedXml.contains(XXE_MARKER));
    }

    @Test
    public void xxeExternalParameterEntityBlocked() throws Exception {
        File secretFile = tmp.newFile("secret_param.txt");
        FileUtils.write(secretFile, XXE_MARKER, StandardCharsets.UTF_8);

        String maliciousXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE foo [\n" +
                "  <!ENTITY % xxe SYSTEM \"file://" + secretFile.getAbsolutePath() + "\">\n" +
                "  %xxe;\n" +
                "]>\n" +
                "<coverage>\n" +
                "  <packages>\n" +
                "    <package name=\"test\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "      <classes>\n" +
                "        <class name=\"Test\" filename=\"Test.java\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "          <lines>\n" +
                "            <line number=\"1\" hits=\"1\"/>\n" +
                "          </lines>\n" +
                "        </class>\n" +
                "      </classes>\n" +
                "    </package>\n" +
                "  </packages>\n" +
                "</coverage>";

        File xxeFile = writeTempFile("xxe-param-entity.xml", maliciousXml);
        String parsedXml = parseWithPluginParser(xxeFile);
        assertFalse("External parameter entity content must not be resolved",
                parsedXml.contains(XXE_MARKER));
    }

    @Test
    public void xxeSSRFAttackBlocked() throws Exception {
        File secretFile = tmp.newFile("ssrf_secret.txt");
        FileUtils.write(secretFile, XXE_MARKER, StandardCharsets.UTF_8);

        String maliciousXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE foo [\n" +
                "  <!ENTITY xxe SYSTEM \"file://" + secretFile.getAbsolutePath() + "\">\n" +
                "]>\n" +
                "<coverage>\n" +
                "  <packages>\n" +
                "    <package name=\"test\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "      <classes>\n" +
                "        <class name=\"Test\" filename=\"Test.java\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "          <lines>\n" +
                "            <line number=\"1\" hits=\"1\"/>\n" +
                "          </lines>\n" +
                "        </class>\n" +
                "      </classes>\n" +
                "    </package>\n" +
                "  </packages>\n" +
                "  &xxe;\n" +
                "</coverage>";

        File xxeFile = writeTempFile("xxe-ssrf.xml", maliciousXml);
        String parsedXml = parseWithPluginParser(xxeFile);
        assertFalse("SSRF entity content must not be resolved",
                parsedXml.contains(XXE_MARKER));
    }

    @Test
    public void xxeExternalDTDLoadingBlocked() throws Exception {
        File secretFile = tmp.newFile("dtd_secret.txt");
        FileUtils.write(secretFile, XXE_MARKER, StandardCharsets.UTF_8);

        String maliciousXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE foo [\n" +
                "  <!ENTITY xxe SYSTEM \"file://" + secretFile.getAbsolutePath() + "\">\n" +
                "]>\n" +
                "<coverage>\n" +
                "  <packages>\n" +
                "    <package name=\"test\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "      <classes>\n" +
                "        <class name=\"Test\" filename=\"Test.java\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "          <lines>\n" +
                "            <line number=\"1\" hits=\"1\"/>\n" +
                "          </lines>\n" +
                "        </class>\n" +
                "      </classes>\n" +
                "    </package>\n" +
                "  </packages>\n" +
                "  &xxe;\n" +
                "</coverage>";

        File xxeFile = writeTempFile("xxe-external-dtd.xml", maliciousXml);
        String parsedXml = parseWithPluginParser(xxeFile);
        assertFalse("External DTD entity content must not be resolved",
                parsedXml.contains(XXE_MARKER));
    }

    @Test
    public void xxeLocalFileExtractionBlocked() throws Exception {
        File secretFile = tmp.newFile("local_secret.txt");
        FileUtils.write(secretFile, XXE_MARKER, StandardCharsets.UTF_8);

        String maliciousXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE foo [\n" +
                "  <!ENTITY xxe SYSTEM \"file://" + secretFile.getAbsolutePath() + "\">\n" +
                "]>\n" +
                "<coverage>\n" +
                "  <packages>\n" +
                "    <package name=\"test\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "      <classes>\n" +
                "        <class name=\"Test\" filename=\"Test.java\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "          <lines>\n" +
                "            <line number=\"1\" hits=\"1\"/>\n" +
                "          </lines>\n" +
                "        </class>\n" +
                "      </classes>\n" +
                "    </package>\n" +
                "  </packages>\n" +
                "  &xxe;\n" +
                "</coverage>";

        File xxeFile = writeTempFile("xxe-local-file.xml", maliciousXml);
        String parsedXml = parseWithPluginParser(xxeFile);
        assertFalse("Local file content must not be extracted via XXE",
                parsedXml.contains(XXE_MARKER));
    }

    @Test
    public void xxeNestedEntityBlocked() throws Exception {
        File secretFile1 = tmp.newFile("nested_secret1.txt");
        FileUtils.write(secretFile1, XXE_MARKER, StandardCharsets.UTF_8);
        File secretFile2 = tmp.newFile("nested_secret2.txt");
        FileUtils.write(secretFile2, "ANOTHER_SECRET_99", StandardCharsets.UTF_8);

        String maliciousXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE foo [\n" +
                "  <!ENTITY xxe1 SYSTEM \"file://" + secretFile1.getAbsolutePath() + "\">\n" +
                "  <!ENTITY xxe2 SYSTEM \"file://" + secretFile2.getAbsolutePath() + "\">\n" +
                "  <!ENTITY combined \"&xxe1;&xxe2;\">\n" +
                "]>\n" +
                "<coverage>\n" +
                "  <packages>\n" +
                "    <package name=\"test\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "      <classes>\n" +
                "        <class name=\"Test\" filename=\"Test.java\" line-rate=\"1.0\" branch-rate=\"1.0\">\n" +
                "          <lines>\n" +
                "            <line number=\"1\" hits=\"1\"/>\n" +
                "          </lines>\n" +
                "        </class>\n" +
                "      </classes>\n" +
                "    </package>\n" +
                "  </packages>\n" +
                "  &combined;\n" +
                "</coverage>";

        File xxeFile = writeTempFile("xxe-nested.xml", maliciousXml);
        String parsedXml = parseWithPluginParser(xxeFile);
        assertFalse("Nested entity content must not be resolved",
                parsedXml.contains(XXE_MARKER));
        assertFalse("Nested entity content must not be resolved",
                parsedXml.contains("ANOTHER_SECRET_99"));
    }

    private String parseWithPluginParser(File xmlFile) throws Exception {
        DocumentBuilder db = XmlCoverageProvider.createSecureDocumentBuilder();
        Document doc = db.parse(xmlFile);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private File writeTempFile(String name, String content) throws IOException {
        File file = tmp.newFile(name);
        try (OutputStream out = new FileOutputStream(file)) {
            IOUtils.write(content, out, StandardCharsets.UTF_8);
        }
        return file;
    }

    private Set<File> getResources(String... resources) {
        Set<File> copiedFiles = new HashSet<>();
        for (String resource : resources) {
            try {
                File copiedFile = tmp.newFile(resource);
                try (InputStream in = getClass().getResourceAsStream(resource);
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(copiedFile))) {
                    IOUtils.copy(in, out);
                    copiedFiles.add(copiedFile);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return copiedFiles;
    }
}
