/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.ssh.authentication.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import org.openintents.ssh.authentication.SshAuthenticationApi;

import java.util.ArrayList;
import java.util.List;


public class SshAuthenticationApiUtils {
    public static List<String> getAgentProviders(Context context) {
        Intent intent = new Intent(SshAuthenticationApi.SERVICE_INTENT);
        List<ResolveInfo> resolvedInfo = context.getPackageManager().queryIntentServices(intent, 0);
        ArrayList<String> providers = new ArrayList<>();
        if (resolvedInfo != null) {
            for (ResolveInfo resolveInfoEntry : resolvedInfo) {
                providers.add(resolveInfoEntry.serviceInfo.packageName);
            }
        }
        return providers;
    }

}
