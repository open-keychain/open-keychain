/* This is the license for Orlib, a free software project to
        provide anonymity on the Internet from a Google Android smartphone.

        For more information about Orlib, see https://guardianproject.info/

        If you got this file as a part of a larger bundle, there may be other
        license terms that you should be aware of.
        ===============================================================================
        Orlib is distributed under this license (aka the 3-clause BSD license)

        Copyright (c) 2009-2010, Nathan Freitas, The Guardian Project

        Redistribution and use in source and binary forms, with or without
        modification, are permitted provided that the following conditions are
        met:

        * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.

        * Redistributions in binary form must reproduce the above
        copyright notice, this list of conditions and the following disclaimer
        in the documentation and/or other materials provided with the
        distribution.

        * Neither the names of the copyright owners nor the names of its
        contributors may be used to endorse or promote products derived from
        this software without specific prior written permission.

        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
        "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
        LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
        A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
        OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
        SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
        LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
        DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
        THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
        (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
        OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

        *****
        Orlib contains a binary distribution of the JSocks library:
        http://code.google.com/p/jsocks-mirror/
        which is licensed under the GNU Lesser General Public License:
        http://www.gnu.org/licenses/lgpl.html

        *****
*/

package org.sufficientlysecure.keychain.util.orbot;

import android.content.Context;

import org.sufficientlysecure.keychain.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.StringTokenizer;

/**
 * This class is taken from the NetCipher library: https://github.com/guardianproject/NetCipher/
 */
public class TorServiceUtils {

    private final static String TAG = "TorUtils";
    // various console cmds
    public final static String SHELL_CMD_CHMOD = "chmod";
    public final static String SHELL_CMD_KILL = "kill -9";
    public final static String SHELL_CMD_RM = "rm";
    public final static String SHELL_CMD_PS = "ps";
    public final static String SHELL_CMD_PIDOF = "pidof";

    public final static String CHMOD_EXE_VALUE = "700";

    public static boolean isRootPossible() {

        StringBuilder log = new StringBuilder();

        try {

            // Check if Superuser.apk exists
            File fileSU = new File("/system/app/Superuser.apk");
            if (fileSU.exists())
                return true;

            fileSU = new File("/system/app/superuser.apk");
            if (fileSU.exists())
                return true;

            fileSU = new File("/system/bin/su");
            if (fileSU.exists()) {
                String[] cmd = {
                        "su"
                };
                int exitCode = doShellCommand(cmd, log, false, true);
                if (exitCode != 0)
                    return false;
                else
                    return true;
            }

            // Check for 'su' binary
            String[] cmd = {
                    "which su"
            };
            int exitCode = doShellCommand(cmd, log, false, true);

            if (exitCode == 0) {
                android.util.Log.d(TAG, "root exists, but not sure about permissions");
                return true;

            }

        } catch (IOException e) {
            // this means that there is no root to be had (normally) so we won't
            // log anything
            android.util.Log.e(TAG, "Error checking for root access", e);

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error checking for root access", e);
            // this means that there is no root to be had (normally)
        }

        android.util.Log.e(TAG, "Could not acquire root permissions");

        return false;
    }

    public static int findProcessId(Context context) {
        String dataPath = context.getFilesDir().getParentFile().getParentFile().getAbsolutePath();
        String command = dataPath + "/" + OrbotHelper.ORBOT_PACKAGE_NAME + "/app_bin/tor";
        int procId = -1;

        try {
            procId = findProcessIdWithPidOf(command);

            if (procId == -1)
                procId = findProcessIdWithPS(command);
        } catch (Exception e) {
            try {
                procId = findProcessIdWithPS(command);
            } catch (Exception e2) {
                Log.e(TAG, "Unable to get proc id for command: " + URLEncoder.encode(command), e2);
            }
        }

        return procId;
    }

    // use 'pidof' command
    public static int findProcessIdWithPidOf(String command) throws Exception {

        int procId = -1;

        Runtime r = Runtime.getRuntime();

        Process procPs;

        String baseName = new File(command).getName();
        // fix contributed my mikos on 2010.12.10
        procPs = r.exec(new String[]{
                SHELL_CMD_PIDOF, baseName
        });
        // procPs = r.exec(SHELL_CMD_PIDOF);

        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null) {

            try {
                // this line should just be the process id
                procId = Integer.parseInt(line.trim());
                break;
            } catch (NumberFormatException e) {
                Log.e("TorServiceUtils", "unable to parse process pid: " + line, e);
            }
        }

        return procId;

    }

    // use 'ps' command
    public static int findProcessIdWithPS(String command) throws Exception {

        int procId = -1;

        Runtime r = Runtime.getRuntime();

        Process procPs;

        procPs = r.exec(SHELL_CMD_PS);

        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.contains(' ' + command)) {

                StringTokenizer st = new StringTokenizer(line, " ");
                st.nextToken(); // proc owner

                procId = Integer.parseInt(st.nextToken().trim());

                break;
            }
        }

        return procId;

    }

    public static int doShellCommand(String[] cmds, StringBuilder log, boolean runAsRoot,
                                     boolean waitFor) throws Exception {

        Process proc = null;
        int exitCode = -1;

        if (runAsRoot)
            proc = Runtime.getRuntime().exec("su");
        else
            proc = Runtime.getRuntime().exec("sh");

        OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());

        for (int i = 0; i < cmds.length; i++) {
            // TorService.logMessage("executing shell cmd: " + cmds[i] +
            // "; runAsRoot=" + runAsRoot + ";waitFor=" + waitFor);

            out.write(cmds[i]);
            out.write("\n");
        }

        out.flush();
        out.write("exit\n");
        out.flush();

        if (waitFor) {

            final char buf[] = new char[10];

            // Consume the "stdout"
            InputStreamReader reader = new InputStreamReader(proc.getInputStream());
            int read = 0;
            while ((read = reader.read(buf)) != -1) {
                if (log != null)
                    log.append(buf, 0, read);
            }

            // Consume the "stderr"
            reader = new InputStreamReader(proc.getErrorStream());
            read = 0;
            while ((read = reader.read(buf)) != -1) {
                if (log != null)
                    log.append(buf, 0, read);
            }

            exitCode = proc.waitFor();

        }

        return exitCode;

    }
}