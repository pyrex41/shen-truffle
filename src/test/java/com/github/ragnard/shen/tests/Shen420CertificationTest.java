package com.github.ragnard.shen.tests;

import com.github.ragnard.shen.ShenRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke gate for the vendored Shen 42.0 certification corpus. */
class Shen420CertificationTest {
    private static final String CORPUS = "kernel-tests";

    @Test
    void certificationCorpusIsVersionedWhenPresent() throws IOException, URISyntaxException {
        var url = getClass().getClassLoader().getResource(CORPUS);
        assertTrue(url != null, "src/test/resources/kernel-tests must be packaged");
        assertTrue(url.getProtocol().equals("file"), "certification corpus must be filesystem-backed");
        Path root = Path.of(url.toURI());
        assertTrue(Files.exists(root));
        try (Stream<Path> files = Files.walk(root)) {
            long count = files.filter(Files::isRegularFile).count();
            assertTrue(count >= 50, "canonical Shen 42.0 corpus appears incomplete");
        }
        assertTrue(Files.exists(root.resolve("runme.shen")), "core Shen certification suite missing");
        assertTrue(Files.exists(root.resolve("prolog.shen")), "Prolog certification fixture missing");
        assertTrue(Files.exists(root.resolve("calculator.shen")), "calculator certification fixture missing");
        Path manifest = root.resolve("VERSION");
        if (Files.exists(manifest)) {
            String version = Files.readString(manifest, StandardCharsets.UTF_8);
            assertTrue(version.contains("42.0"), "certification manifest must identify Shen 42.0");
        }
    }

    @Test
    void certificationManifestHasNoPlaceholderVersion() throws IOException, URISyntaxException {
        var resource = getClass().getClassLoader().getResource(CORPUS + "/VERSION");
        assertTrue(resource != null, "kernel-tests/VERSION is required");
        String version = Files.readString(Path.of(resource.toURI()), StandardCharsets.UTF_8).trim();
        assertFalse(version.isBlank());
        assertFalse(version.toLowerCase().contains("placeholder"));
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.MINUTES)
    @EnabledIfSystemProperty(named = "shen.certify", matches = "true")
    void canonicalKernelSuitePasses() throws Exception {
        Path root = Path.of(getClass().getClassLoader().getResource(CORPUS).toURI());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] continueOnFailure = "y\n".repeat(256).getBytes(StandardCharsets.UTF_8);
        try (ShenRuntime runtime = ShenRuntime.builder()
                .workingDirectory(root)
                .standardLibrary(false)
                .in(new ByteArrayInputStream(continueOnFailure))
                .out(output)
                .err(output)
                .build()) {
            runtime.load(root.resolve("runme.shen"));
        }
        String report = output.toString(StandardCharsets.UTF_8);
        assertTrue(report.contains("failed ... 0"), () -> "kernel failures:\n" + report);
        assertTrue(report.contains("pass rate ... 100"), () -> "incomplete certification:\n" + report);
    }
}
