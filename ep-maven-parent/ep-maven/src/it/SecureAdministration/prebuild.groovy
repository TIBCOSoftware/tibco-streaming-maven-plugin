/*
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
 */

// Generate a self-signed node credential and a trust store holding it. The administration client checks the
// node's host name against a PKCS12 trust store, so the certificate must name this machine; it cannot be
// checked in. The same credential serves as the client's key store, since the profile requires client
// authentication and the trust store trusts it.

def directory = new File(basedir, "target/credentials")
directory.mkdirs()

def local = InetAddress.getLocalHost()
def names = ([local.getHostName(), local.getCanonicalHostName(), "localhost"] as LinkedHashSet)
def san = names.collect { "dns:" + it }.join(",") + ",ip:127.0.0.1"
def keytool = new File(System.getProperty("java.home"), "bin/keytool").absolutePath

def run(List<String> command, File directory) {
    System.out.println("Running: " + command.join(" "))
    def process = new ProcessBuilder(command).directory(directory).redirectErrorStream(true).start()
    process.inputStream.eachLine { System.out.println(it) }
    if (process.waitFor() != 0) {
        throw new AssertionError("Command failed: " + command.join(" "))
    }
}

["keystore.p12", "truststore.p12", "node.cer"].each { new File(directory, it).delete() }

run([keytool, "-genkeypair", "-alias", "node", "-keyalg", "RSA", "-keysize", "2048", "-validity", "30",
     "-dname", "CN=" + local.getHostName() + ",O=ep-maven-plugin integration test", "-ext", "san=" + san,
     "-keystore", "keystore.p12", "-storetype", "PKCS12", "-storepass", "secret", "-keypass", "secret"], directory)
run([keytool, "-exportcert", "-alias", "node", "-keystore", "keystore.p12", "-storepass", "secret",
     "-file", "node.cer"], directory)
run([keytool, "-importcert", "-noprompt", "-alias", "node", "-file", "node.cer",
     "-keystore", "truststore.p12", "-storetype", "PKCS12", "-storepass", "secret"], directory)

return true
