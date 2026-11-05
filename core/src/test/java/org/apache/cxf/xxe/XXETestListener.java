/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.xxe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Permission;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fixture shared by the XXE regression tests.
 *
 * <p>It creates a scratch directory holding a "canary" document and installs a fully permissive
 * {@link SecurityManager} that does nothing except record every attempt to <em>read</em> that
 * canary. XML machinery that resolves an external entity or an external DTD subset pointing at
 * the canary therefore leaves a trace, which lets a test assert - offline and deterministically -
 * that the out-of-band fetch either did or did not happen.
 *
 * <p>Installing a permissive SecurityManager has a useful side effect: it makes the
 * {@code AccessController.doPrivileged} blocks in the resolution code paths actually execute,
 * so those are covered too.
 *
 * <p>Not a test class - it deliberately carries no {@code @Test} methods and its name does not
 * match the surefire include patterns. Always {@link #close()} it in a {@code finally} block so
 * the previous SecurityManager is restored.
 */
public final class XXETestListener {

    /** Name of the file that must never be fetched by an XML parser. */
    public static final String CANARY_NAME = "xxe-canary.txt";

    private static final String CANARY_CONTENT = "cxf-xxe-canary";

    private final File directory;
    private final File canary;
    private final String canaryUri;
    private final List<String> reads = new CopyOnWriteArrayList<String>();
    private final SecurityManager previous;
    private boolean closed;

    public XXETestListener() throws IOException {
        directory = createTempDirectory("cxf-xxe");
        canary = new File(directory, CANARY_NAME);
        writeFile(canary, CANARY_CONTENT);
        // computed up front: File.toURI() stats the file, which would count as a canary read
        canaryUri = canary.toURI().toString();
        previous = System.getSecurityManager();
        System.setSecurityManager(new ReadRecordingSecurityManager());
    }

    /** The scratch directory holding the attacker controlled documents. */
    public File directory() {
        return directory;
    }

    /** {@code file:} URI of the scratch directory, with a trailing slash. */
    public String directoryUri() {
        String uri = directory.toURI().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }

    /** {@code file:} URI of the canary document. */
    public String canaryUri() {
        return canaryUri;
    }

    /** Writes an attacker controlled document into the scratch directory. */
    public File write(String name, String content) throws IOException {
        File file = new File(directory, name);
        writeFile(file, content);
        return file;
    }

    public String uriOf(File file) {
        return file.toURI().toString();
    }

    /** True when something opened the canary document. */
    public boolean canaryWasRead() {
        return !reads.isEmpty();
    }

    public List<String> reads() {
        return Collections.unmodifiableList(reads);
    }

    /** Forgets the recorded reads, so one test can run a control pass and then the real one. */
    public void clear() {
        reads.clear();
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        System.setSecurityManager(previous);
        File[] entries = directory.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                // best effort cleanup of a temporary directory
                entry.delete();
            }
        }
        // best effort cleanup of a temporary directory
        directory.delete();
    }

    private void record(String file) {
        if (file != null && file.endsWith(CANARY_NAME)) {
            reads.add(file);
        }
    }

    private static File createTempDirectory(String prefix) throws IOException {
        File dir = File.createTempFile(prefix, "");
        if (!dir.delete() || !dir.mkdir()) {
            throw new IOException("Could not create the temporary directory " + dir);
        }
        return dir;
    }

    private static void writeFile(File file, String content) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            out.write(content.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /**
     * Grants everything - the point is purely to observe {@code checkRead} for the canary.
     * {@link java.io.FileInputStream} (which every {@code file:} URL connection ends up using)
     * routes through it, so an external entity fetch cannot avoid being seen.
     */
    private final class ReadRecordingSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
        }

        @Override
        public void checkRead(String file) {
            record(file);
        }

        @Override
        public void checkRead(String file, Object context) {
            record(file);
        }
    }
}
