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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.assertj.core.api.Assertions;

import com.tibco.ep.sb.services.stubs.admin.Command;
import com.tibco.ep.sb.services.stubs.admin.Command.Execution;

/**
 * <p>
 * JUnit tests for the assertion handling of {@link BaseTestMojo}.
 * </p>
 * <p>
 * Assertions are enabled in the deployed test JVM by default. Whatever the user configures is appended after that
 * default, because the last of the assertion options wins in the JVM.
 * </p>
 * <p>
 * The base class is a JUnit 3 test case, so fixtures are {@code setUp}/{@code tearDown} and test methods are
 * found by name - JUnit 4 annotations would be ignored.
 * </p>
 */
public class AssertionsTest extends BetterAbstractMojoTestCase {
    private static final String PROJECT_DIRECTORY = "target/projects/java";

    private static final String TEST_CLASSES_DIRECTORY = PROJECT_DIRECTORY
        + "/target/test-classes/com/tibco/ep/buildmavenplugin";

    private static final String ENABLE_ASSERTIONS = "-enableassertions";

    private final SimulatedLog simulatedLog = new SimulatedLog(false);

    /**
     * <p>
     * Install the stub product, give the project a test class to run, and start the project's node.
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

        // The test goal only deploys when it finds a test class.
        new File(TEST_CLASSES_DIRECTORY).mkdirs();
        Files.copy(new File("target/test-classes/com/tibco/ep/buildmavenplugin/DummyTest.class").toPath(),
            new File(TEST_CLASSES_DIRECTORY, "DummyTest.class").toPath(), StandardCopyOption.REPLACE_EXISTING);

        StartNodesMojo startNodes = (StartNodesMojo) lookupConfiguredMojo(new File(PROJECT_DIRECTORY, "start.xml"),
            "start-nodes");
        startNodes.setLog(this.simulatedLog);
        startNodes.execute();
    }

    /**
     * <p>
     * Remove the project's node.
     * </p>
     *
     * @throws Exception on unexpected failure
     */
    @Override
    protected void tearDown() throws Exception {
        try {
            StopNodesMojo stopNodes = (StopNodesMojo) lookupConfiguredMojo(new File(PROJECT_DIRECTORY, "stop.xml"),
                "stop-nodes");
            stopNodes.setLog(this.simulatedLog);
            stopNodes.execute();
        } finally {
            super.tearDown();
        }
    }

    /**
     * <p>
     * With no java options, assertions are enabled.
     * </p>
     *
     * @throws Exception on unexpected failure
     */
    public void testAssertionsEnabledByDefault() throws Exception {
        final List<String> options = deployOptions(null, null);

        Assertions.assertThat(options).contains(ENABLE_ASSERTIONS);
    }

    /**
     * <p>
     * A disable assertions option from the pom follows the default, so it takes effect.
     * </p>
     *
     * @throws Exception on unexpected failure
     */
    public void testJavaOptionsDisableAssertions() throws Exception {
        final List<String> options = deployOptions(new String[] {"-disableassertions"}, null);

        Assertions.assertThat(options).contains(ENABLE_ASSERTIONS, "-disableassertions");
        Assertions.assertThat(options.indexOf("-disableassertions")).isGreaterThan(options.indexOf(ENABLE_ASSERTIONS));
    }

    /**
     * <p>
     * A disable assertions option from the command line follows the default, so it takes effect.
     * </p>
     *
     * @throws Exception on unexpected failure
     */
    public void testOptionsPropertyDisableAssertions() throws Exception {
        final List<String> options = deployOptions(null, new String[] {"-da"});

        Assertions.assertThat(options).contains(ENABLE_ASSERTIONS, "-da");
        Assertions.assertThat(options.indexOf("-da")).isGreaterThan(options.indexOf(ENABLE_ASSERTIONS));
    }

    /**
     * Run the project's test goal and return the execution options of the test deployment.
     */
    private List<String> deployOptions(final String[] javaOptions, final String[] optionsProperty) throws Exception {
        TestJavaFragmentMojo mojo = (TestJavaFragmentMojo) lookupConfiguredMojo(
            new File(PROJECT_DIRECTORY, "test.xml"), "test-java-fragment");
        mojo.setLog(this.simulatedLog);
        mojo.javaOptions = javaOptions;
        mojo.optionsProperty = optionsProperty;

        Command.clearExecutions();
        mojo.execute();

        final List<Execution> deploys = Command.getExecutions().stream() //
            .filter(execution -> "deploy".equals(execution.command())) //
            .toList();
        Assertions.assertThat(deploys).hasSize(1);
        return deploys.get(0).executionOptions();
    }
}
