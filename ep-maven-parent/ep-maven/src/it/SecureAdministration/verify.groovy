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

// Every command after the install reached a node that only accepts clients presenting the configured
// credentials and trusting its certificate.
checkSequence(new File(basedir, "build.log"),
        "[INFO] [Node1.secure_cluster] Running \"install node\"",
        "[INFO] [Node1.secure_cluster] Finished \"install node\"",
        "[INFO] [secure_cluster] Running \"start node\"",
        "[INFO] [secure_cluster] Finished \"start node\"",
        "[INFO] [Node1.secure_cluster] Finished \"display node\"",
        "[INFO] [Node1.secure_cluster] Finished \"stop node\"",
        "[INFO] [Node1.secure_cluster] Finished \"remove node\"",
        "BUILD SUCCESS")

static def checkSequence(File file, String... sequence) {

    List<String> contents = new ArrayList<>(Arrays.asList(sequence))
    List<String> lines = []
    file.eachLine {lines.add(it) }

    System.out.println("Scanning file: " + file)

    for (int i = 0 ; i < lines.size() ; i++) {
        if (contents.isEmpty()) {
            return;
        }
        if (lines.get(i).contains(contents.get(0))) {
            System.out.println("Line " + i + ": " + contents.get(0))
            contents.remove(0);
        }
    }

    if (!contents.isEmpty()) {
        throw new AssertionError("Could not find " + contents.get(0))
    }
    System.out.println("Scan OK.")
}