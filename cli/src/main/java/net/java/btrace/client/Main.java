/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package net.java.btrace.client;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import sun.misc.Signal;
import sun.misc.SignalHandler;
import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.extensions.ExtensionsRepository;
import net.java.btrace.api.extensions.ExtensionsRepositoryFactory;
import net.java.btrace.util.Messages;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the main class for a simple command line
 * BTrace client. It is possible to create a GUI
 * client using the Client class.
 *
 * @author A. Sundararajan
 */
public final class Main {
    public static volatile boolean exiting;
    public static final boolean DEBUG;
    public static final boolean TRACK_RETRANSFORM;
    public static final boolean UNSAFE;
    public static final boolean DUMP_CLASSES;
    public static final String DUMP_DIR;
    public static final String PROBE_DESC_PATH;
    public static final int BTRACE_DEFAULT_PORT = 2020;
    private static final Console con;

    static {
        DEBUG = Boolean.getBoolean("net.java.btrace.debug");
        if (DEBUG) {
            BTraceLogger.debugPrint("btrace debug mode is set");
        }
        
        TRACK_RETRANSFORM = Boolean.getBoolean("net.java.btrace.trackRetransforms");
        if (TRACK_RETRANSFORM) {
            BTraceLogger.debugPrint("trackRetransforms flag is set");
        }
        UNSAFE = Boolean.getBoolean("net.java.btrace.unsafe");
        if (UNSAFE) {
            BTraceLogger.debugPrint("btrace unsafe mode is set");
        }
        DUMP_CLASSES = Boolean.getBoolean("net.java.btrace.dumpClasses");
        if (DUMP_CLASSES) {
            BTraceLogger.debugPrint("dumpClasses flag is set");
        }
        DUMP_DIR = System.getProperty("net.java.btrace.dumpDir", ".");
        if (DUMP_CLASSES) {
            BTraceLogger.debugPrint("dumpDir is " + DUMP_DIR);
        }
        PROBE_DESC_PATH = System.getProperty("net.java.btrace.probeDescPath", ".");
        con = System.console();
//        out = (con != null) ? con.writer() : new PrintWriter(System.out);
    }

    private static String getJarFor(String clz) {
        ClassLoader cl =  Main.class.getClassLoader();
        URL resURL = cl.getResource(clz);
        if (resURL == null)  return null;
        
        String jarPath = resURL.toString().replace("jar:file:", "");
        jarPath = jarPath.substring(0, jarPath.indexOf(".jar!") + 4);
        
        return jarPath;
    }
    
    public static void main(String[] args) {
        String bootstrap = getJarFor("net/java/btrace/api/core/BTraceRuntime.class");
        String agentpath = getJarFor("net/java/btrace/agent/Main.class");
        
        String defaultExtPath = getJarFor("net/java/btrace/ext/Printer.class") + File.pathSeparator + 
                         getJarFor("net/java/btrace/ext/profiling/Profiler.class") + File.pathSeparator +
                         getJarFor("net/java/btrace/ext/aggregations/Aggregations.class") + File.pathSeparator +
                         getJarFor("net/java/btrace/ext/collections/Collections.class") + File.pathSeparator + 
                         getJarFor("net/java/btrace/ext/export/Export.class") + File.separator + 
                         getJarFor("net/java/btrace/ext/sys/Memory.class");
        
        defaultExtPath = defaultExtPath.replace("null" + File.pathSeparator, "");
        
        if (args.length < 2) {
            usage();
        }

        int port = BTRACE_DEFAULT_PORT;
        String classPath = ".";
        String extPath = "";
        String includePath = null;

        int count = 0;
        boolean portDefined = false;
        boolean classpathDefined = false;
        boolean includePathDefined = false;

        for (;;) {
            if (args[count].charAt(0) == '-') {
                if (args.length <= count + 1) {
                    usage();
                }
                if (args[count].equals("-p") && !portDefined) {
                    try {
                        port = Integer.parseInt(args[++count]);
                        BTraceLogger.debugPrint("accepting port " + port);
                    } catch (NumberFormatException nfe) {
                        usage();
                    }
                    portDefined = true;
                } else if ((args[count].equals("-cp")
                        || args[count].equals("-classpath"))
                        && !classpathDefined) {
                    classPath = args[++count];
                    BTraceLogger.debugPrint("accepting classpath " + classPath);
                    classpathDefined = true;
                } else if (args[count].equals("-I") && !includePathDefined) {
                    includePath = args[++count];
                    BTraceLogger.debugPrint("accepting include path " + includePath);
                    includePathDefined = true;
                } else if (args[count].equals("-x")
                        || args[count].equals("-extpath")) {
                    extPath = args[++count];
                    BTraceLogger.debugPrint("accepting extensions path " + extPath);
                } else {
                    usage();
                }
                count++;
                if (count >= args.length) {
                    break;
                }
            } else {
                break;
            }
        }

        if (!portDefined) {
            BTraceLogger.debugPrint("assuming default port " + port);
        }

        if (!classpathDefined) {
            BTraceLogger.debugPrint("assuming default classpath '" + classPath + "'");
        }

        if (args.length < (count + 1)) {
            usage();
        }

        String pid = args[count];
        String fileName = args[count + 1];
        String[] btraceArgs = new String[args.length - count];
        if (btraceArgs.length > 0) {
            System.arraycopy(args, count, btraceArgs, 0, btraceArgs.length);
        }

        try {
            if (!new File(fileName).exists()) {
                errorExit("File not found: " + fileName, 1);
            }
            ExtensionsRepository extRepository = getRepository(defaultExtPath.isEmpty() ? extPath : defaultExtPath + File.pathSeparator + extPath);
            Compiler compiler = new Compiler(UNSAFE, extRepository);
            byte[] code = compiler.compile(fileName, classPath, includePath);
            if (code == null) {
                errorExit("BTrace compilation failed", 1);
            }

            Client client = Client.forProcess(Integer.valueOf(pid));
            if (agentpath != null) {
                client.setAgentPath(agentpath);
            }
            if (bootstrap != null) {
                client.setBootstrapPath(bootstrap);
            }
            client.setProbeDescPath(PROBE_DESC_PATH);
            client.setExtRepository(extRepository);
            client.setTrackRetransforms(TRACK_RETRANSFORM);
            client.setUnsafe(UNSAFE);
            client.setDumpClasses(DUMP_CLASSES);
            client.setDumpDir(DUMP_DIR);
            client.setPort(port);
            client.attach();
            
            registerExitHook(client);
            if (con != null) {
                registerSignalHandler(client);
            }
            BTraceLogger.debugPrint("submitting the BTrace program");
            client.submit(fileName, code, args);
        } catch (IOException exp) {
            exp.printStackTrace();
            errorExit(exp.getMessage(), 1);
        }
    }

    private static ExtensionsRepository getRepository(String extPath) {
        BTraceLogger.debugPrint("getting repository for " + extPath);
        return ExtensionsRepositoryFactory.composite(
                ExtensionsRepository.Location.BOTH, 
                ExtensionsRepositoryFactory.builtin(ExtensionsRepository.Location.BOTH), 
                ExtensionsRepositoryFactory.fixed(ExtensionsRepository.Location.BOTH, extPath)
        );
    }

    private static AtomicInteger exitCode = new AtomicInteger(-1); // -1 == normal shutdown
    private static void registerExitHook(final Client client) {
        BTraceLogger.debugPrint("registering shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread(
            new Runnable() {

                public void run() {
                    BTraceLogger.debugPrint("exitting btrace client");
                    client.exit(exitCode.get());
                }
            })
        );
    }

    private static void registerSignalHandler(final Client client) {
        BTraceLogger.debugPrint("registering signal handler for SIGINT");
        Signal.handle(new Signal("INT"),
            new SignalHandler() {

                public void handle(Signal sig) {
                    try {
                        con.printf("Please enter your option:\n");
                        con.printf("\t1. exit\n\t2. send an event\n\t3. send a named event\n");
                        con.flush();
                        String option = con.readLine();
                        option = option.trim();
                        if (option == null) {
                            return;
                        }
                        if (option.equals("1")) {
                            System.exit(0);
                        } else if (option.equals("2")) {
                            BTraceLogger.debugPrint("sending event command");
                            sendEvent(client);
                        } else if (option.equals("3")) {
                            con.printf("Please enter the event name: ");
                            String name = con.readLine();
                            if (name != null) {
                                BTraceLogger.debugPrint("sending event command");
                                sendEvent(client, name);
                            }
                        } else {
                            con.printf("invalid option!\n");
                        }
                    } catch (IOException ioexp) {
                        BTraceLogger.debugPrint(ioexp.toString());
                    }
                }
            }
        );
    }

    private static void usage() {
        System.err.println(Messages.get("btrace.usage"));
        System.exit(1);
    }

    private static boolean isUnsafe() {
        return UNSAFE;
    }

    private static void errorExit(String msg, int code) {
        System.err.println(msg);
        exitCode.set(code);
        System.exit(code);
    }

    private static void sendEvent(Client client) throws IOException {
        sendEvent(client, null);
    }

    private static void sendEvent(Client client, final String eName) throws IOException {
        client.sendEvent(eName);
    }
}
