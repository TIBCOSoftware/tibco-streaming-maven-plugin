/*******************************************************************************
 * Copyright (C) 2018-2026 Cloud Software Group, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for automatic discovery port selection.
 */
public class DiscoveryPortTest {

    /**
     * A band the operating system does not assign automatically on any
     * supported platform, used to check that a configured range is honoured.
     */
    private static final int TEST_RANGE_MIN = 20000;

    private static final int TEST_RANGE_MAX = 21000;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private TestMojo mojo;

    /**
     * Minimal concrete mojo - getDiscoveryPort() is the unit under test.
     */
    private static class TestMojo extends BaseExecuteMojo {
        @Override
        public void execute() {
            // nothing to execute
        }
    }

    /**
     * Build a mojo with a discovery port file in a scratch directory.
     *
     * @throws IOException on error
     */
    @Before
    public void setUp() throws IOException {
        mojo = new TestMojo();
        mojo.setLog(new SimulatedLog(false));
        mojo.discoveryPortFile = new File(folder.newFolder(), "discovery.port");
    }

    /**
     * An explicitly configured port is returned unchanged, and no file is written.
     */
    @Test
    public void explicitPortIsUsed() {
        mojo.discoveryPort = 12345;

        assertEquals(12345, mojo.getDiscoveryPort());
        assertTrue("no port file should be written",
            !mojo.discoveryPortFile.exists());
    }

    /**
     * An explicitly configured port wins over a configured range.
     */
    @Test
    public void explicitPortWinsOverRange() {
        mojo.discoveryPort = 12345;
        mojo.discoveryPortRangeMin = TEST_RANGE_MIN;
        mojo.discoveryPortRangeMax = TEST_RANGE_MAX;

        assertEquals(12345, mojo.getDiscoveryPort());
    }

    /**
     * A previously persisted port is reused.
     *
     * @throws IOException on error
     */
    @Test
    public void persistedPortIsReused() throws IOException {
        write(mojo.discoveryPortFile, "23456");

        assertEquals(23456, mojo.getDiscoveryPort());
    }

    /**
     * With no range configured, selection uses the IANA dynamic port range.
     * This is the behaviour existing consumers rely on.
     */
    @Test
    public void defaultRangeIsIanaDynamicRange() {
        assertEquals(49152, BaseExecuteMojo.DEFAULT_DISCOVERY_PORT_RANGE_MIN);
        assertEquals(65535, BaseExecuteMojo.DEFAULT_DISCOVERY_PORT_RANGE_MAX);

        int port = mojo.getDiscoveryPort();

        assertInRange(port, BaseExecuteMojo.DEFAULT_DISCOVERY_PORT_RANGE_MIN,
            BaseExecuteMojo.DEFAULT_DISCOVERY_PORT_RANGE_MAX);
    }

    /**
     * A configured range is honoured, keeping the port out of the range the
     * operating system assigns automatically.
     */
    @Test
    public void configuredRangeIsHonoured() {
        mojo.discoveryPortRangeMin = TEST_RANGE_MIN;
        mojo.discoveryPortRangeMax = TEST_RANGE_MAX;

        assertInRange(mojo.getDiscoveryPort(), TEST_RANGE_MIN, TEST_RANGE_MAX);
    }

    /**
     * A single-port range selects that port.
     */
    @Test
    public void singlePortRangeSelectsThatPort() {
        mojo.discoveryPortRangeMin = TEST_RANGE_MIN;
        mojo.discoveryPortRangeMax = TEST_RANGE_MIN;

        assertEquals(TEST_RANGE_MIN, mojo.getDiscoveryPort());
    }

    /**
     * The selected port is persisted so later invocations reuse it.
     *
     * @throws IOException on error
     */
    @Test
    public void selectedPortIsPersisted() throws IOException {
        mojo.discoveryPortRangeMin = TEST_RANGE_MIN;
        mojo.discoveryPortRangeMax = TEST_RANGE_MAX;

        int port = mojo.getDiscoveryPort();

        assertTrue("port file should be written", mojo.discoveryPortFile.exists());
        assertEquals(port, Integer.parseInt(
            new String(Files.readAllBytes(mojo.discoveryPortFile.toPath()),
                StandardCharsets.UTF_8).trim()));

        // a second call returns the persisted value rather than reselecting
        //
        assertEquals(port, mojo.getDiscoveryPort());
    }

    /**
     * An inverted range is rejected and the default range used instead.
     */
    @Test
    public void invertedRangeFallsBackToDefault() {
        mojo.discoveryPortRangeMin = TEST_RANGE_MAX;
        mojo.discoveryPortRangeMax = TEST_RANGE_MIN;

        assertInRange(mojo.getDiscoveryPort(),
            BaseExecuteMojo.DEFAULT_DISCOVERY_PORT_RANGE_MIN,
            BaseExecuteMojo.DEFAULT_DISCOVERY_PORT_RANGE_MAX);
    }

    /**
     * An out of bounds range is rejected and the default range used instead.
     */
    @Test
    public void outOfBoundsRangeFallsBackToDefault() {
        mojo.discoveryPortRangeMin = 0;
        mojo.discoveryPortRangeMax = 70000;

        assertInRange(mojo.getDiscoveryPort(),
            BaseExecuteMojo.DEFAULT_DISCOVERY_PORT_RANGE_MIN,
            BaseExecuteMojo.DEFAULT_DISCOVERY_PORT_RANGE_MAX);
    }

    private void assertInRange(int port, int min, int max) {
        assertTrue("port " + port + " below " + min, port >= min);
        assertTrue("port " + port + " above " + max, port <= max);
    }

    private void write(File file, String contents) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
            new FileWriter(file, StandardCharsets.UTF_8))) {
            writer.write(contents + "\n");
        }
    }
}
