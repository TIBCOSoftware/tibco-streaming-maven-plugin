/*******************************************************************************
 * Copyright (C) 2026 Cloud Software Group, Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.tibco.ep.buildmavenplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.assertj.core.api.Assertions;

import com.tibco.ep.sb.services.stubs.admin.Command;
import com.tibco.ep.sb.services.stubs.admin.Command.Execution;
import com.tibco.ep.sb.services.stubs.admin.RuntimeAdminService;

/**
 * <p>
 * JUnit tests for the key store and trust store parameters of {@link BaseExecuteMojo}.
 * </p>
 * <p>
 * Installing a node from an application whose node deploy configuration names a secure communication profile
 * switches the node to that profile as the install's last step. So the install must connect with the default
 * certificate, and every command after it with the configured trust store.
 * </p>
 * <p>
 * The base class is a JUnit 3 test case, so fixtures are {@code setUp}/{@code tearDown} and test methods are
 * found by name - JUnit 4 annotations would be ignored.
 * </p>
 */
public class TLSCredentialsTest extends BetterAbstractMojoTestCase {

    private static final String PROJECT_DIRECTORY = "target/projects/tls";

    /**
     * Directory of the single node the project installs - default node name plus the project's cluster name
     */
    private static final File NODE_DIRECTORY = new File(PROJECT_DIRECTORY, "target/test-nodes/A.tls");

    private final SimulatedLog simulatedLog = new SimulatedLog(false);

    /**
     * <p>
     * Install the stub product.
     * </p>
     *
     * @throws Exception on unexpected failure
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        InstallProductMojo installProduct = (InstallProductMojo) lookupConfiguredMojo(
            new File("target/projects", "pom.xml"), "install-product");
        installProduct.setLog(this.simulatedLog);
        installProduct.execute();

        // The stub "remove node" leaves the node directory behind; start each test without a leftover node.
        deleteRecursively(TLSCredentialsTest.NODE_DIRECTORY.toPath());
        Command.clearExecutions();
    }

    /**
     * <p>
     * Restore TLS support in the stub runtime and remove any installed nodes.
     * </p>
     *
     * @throws Exception on unexpected failure
     */
    @Override
    protected void tearDown() throws Exception {
        try {
            RuntimeAdminService.setTLSCredentialsSupported(true);

            StopNodesMojo stopNodes = (StopNodesMojo) lookupConfiguredMojo(new File(PROJECT_DIRECTORY, "pom.xml"),
                "stop-nodes");
            stopNodes.setLog(this.simulatedLog);
            stopNodes.execute();
        } finally {
            super.tearDown();
        }
    }

    /**
     * <p>
     * start-nodes installs without TLS credentials and starts with them; later goals use them throughout.
     * </p>
     *
     * @throws Exception on unexpected failure
     */
    public void testInstallNodeOmitsTLSCredentials() throws Exception {
        runGoal("pom.xml", "start-nodes");

        List<Execution> executions = Command.getExecutions();
        Assertions.assertThat(executions).extracting(Execution::command) //
            .containsExactly("install", "start");
        assertNoTLSCredentials(executions.get(0));
        assertTLSCredentials(executions.get(1));

        Command.clearExecutions();
        runGoal("status.xml", "administer-nodes");
        runGoal("pom.xml", "stop-nodes");

        executions = Command.getExecutions();
        Assertions.assertThat(executions).extracting(Execution::command) //
            .containsExactly("display", "stop", "remove");
        executions.forEach(TLSCredentialsTest::assertTLSCredentials);
    }

    /**
     * <p>
     * A node left over from an earlier run was installed from the same application, so it is already on the
     * secure profile: start-nodes stops and removes it with TLS credentials before installing without them.
     * </p>
     *
     * @throws Exception on unexpected failure
     */
    public void testLeftoverNodeRemovedWithTLSCredentials() throws Exception {
        Files.createDirectories(TLSCredentialsTest.NODE_DIRECTORY.toPath());

        runGoal("pom.xml", "start-nodes");

        List<Execution> executions = Command.getExecutions();
        Assertions.assertThat(executions).extracting(Execution::command) //
            .containsExactly("stop", "remove", "install", "start");
        assertTLSCredentials(executions.get(0));
        assertTLSCredentials(executions.get(1));
        assertNoTLSCredentials(executions.get(2));
        assertTLSCredentials(executions.get(3));
    }

    /**
     * <p>
     * A runtime that cannot apply TLS credentials fails the goal before any node is installed.
     * </p>
     *
     * @throws Exception on unexpected failure
     */
    public void testUnsupportedRuntimeFailsBeforeInstall() throws Exception {
        RuntimeAdminService.setTLSCredentialsSupported(false);

        Assertions.assertThatThrownBy(() -> runGoal("pom.xml", "start-nodes")) //
            .isInstanceOf(MojoExecutionException.class) //
            .hasMessageContaining("keystore and truststore are not supported");
        Assertions.assertThat(Command.getExecutions()).isEmpty();
    }

    private static void assertNoTLSCredentials(final Execution execution) {
        Assertions.assertThat(execution.truststore()).as("truststore for %s", execution.command()).isEmpty();
        Assertions.assertThat(execution.keystore()).as("keystore for %s", execution.command()).isEmpty();
    }

    private static void assertTLSCredentials(final Execution execution) {
        Assertions.assertThat(execution.truststore()).as("truststore for %s", execution.command()) //
            .map(Path::getFileName) //
            .contains(Path.of("truststore.jks"));
        Assertions.assertThat(execution.keystore()).as("keystore for %s", execution.command()) //
            .map(Path::getFileName) //
            .contains(Path.of("keystore.jks"));
    }

    private static void deleteRecursively(final Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private void runGoal(final String pom, final String goal) throws Exception {
        BaseExecuteMojo mojo = (BaseExecuteMojo) lookupConfiguredMojo(new File(PROJECT_DIRECTORY, pom), goal);
        mojo.setLog(this.simulatedLog);
        mojo.execute();
    }
}
