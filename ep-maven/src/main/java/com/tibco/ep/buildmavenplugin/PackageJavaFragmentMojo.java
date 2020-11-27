/*******************************************************************************
 * Copyright (C) 2018 - 2019, TIBCO Software Inc.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

/**
 * <p>Build a java fragment zip archive.</p>
 *
 * <p>The packaging rules are as follows :-</p>
 * <ol>
 * <li>A mainifest file is created at /META-INF/MANIFEST.MF containing :-
 *      <ul>
 *              <li>Archiver-Version: Plexus Archiver</li>
 *              <li>Built-By: build user</li>
 *              <li>Build-Jdk: jdk version</li>
 *              <li>Package-Title: project groupId and name</li>
 *              <li>Package-Version: project version and buildNumber</li>
 *              <li>Package-Vendor: project organization name (if set)</li>
 *              <li>Main-Class: mainClass parameter (if set)</li>
 *              <li>TIBCO-EP-Build-Product-Version: product version</li>
 *      </ul>
 * <li>pom.xml copied to /META-INF/maven/groupId/artifactId/pom.xml</li>
 * <li>pom.properties created in /META-INF/maven/groupId/artifactId/pom.properties</li>
 * <li>The project's built java classes files are copied into /classes</li>
 * <li>The project's resource files are copied into /</li>
 * <li>Java runtime dependencies are copied into /dependencies/java/</li>
 * </ol>
 *
 * <p>The plexus archiver is used to create the archive via the maven assembly
 * plugin.</p>
 *
 * <p>Native archives (nar) are included - in this case a mapping is included
 * from the nar AOL values (http://maven-nar.github.io/aol.html) to internal
 * values.</p>
 *
 * <p>The generated filename is &lt;artifactId&gt;-&lt;version&gt;-ep-java-fragment.zip</p>
 */
@Mojo(name = "package-java-fragment", defaultPhase = PACKAGE, threadSafe = true)
public class PackageJavaFragmentMojo extends BasePackageMojo {

    /**
     * <p>Java Main class name.  This will be added to the MANIFEST file.</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/package-mainClass.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    String mainClass;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().debug("Creating fragment zip");

        prechecks();

        newArchiveGenerator().withMainClass(mainClass).generate();
    }
}
