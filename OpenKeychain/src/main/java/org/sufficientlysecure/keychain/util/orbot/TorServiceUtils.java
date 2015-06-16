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

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.StringTokenizer;

/**
 * This class is taken from the NetCipher library: https://github.com/guardianproject/NetCipher/
 */

public class TorServiceUtils {

    private final static String TAG = "TorUtils";
    // various console cmds
    public final static String SHELL_CMD_PS = "ps";
    public final static String SHELL_CMD_PIDOF = "pidof";

    public static int findProcessId(String command) {
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

        Process procPs = null;

        String baseName = new File(command).getName();
        // fix contributed my mikos on 2010.12.10
        procPs = r.exec(new String[]{
                SHELL_CMD_PIDOF, baseName
        });
        // procPs = r.exec(SHELL_CMD_PIDOF);

        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line = null;

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

        Process procPs = null;

        procPs = r.exec(SHELL_CMD_PS);

        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line = null;

        while ((line = reader.readLine()) != null) {
            if (line.indexOf(' ' + command) != -1) {

                StringTokenizer st = new StringTokenizer(line, " ");
                st.nextToken(); // proc owner

                procId = Integer.parseInt(st.nextToken().trim());

                break;
            }
        }

        return procId;

    }
}
