/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.thialfihar.android.apg.service;

import org.thialfihar.android.apg.service.handler.IApgGetKeyringsHandler;

/**
 * All methods are oneway, which means they are asynchronous and non-blocking.
 * Results are returned into given Handler, which has to be implemented on client side.
 */
interface IApgKeyService {

    oneway void getPublicKeyRings(in long[] masterKeyIds, in boolean asAsciiArmoredStringArray,
            in IApgGetKeyringsHandler handler);

    oneway void getSecretKeyRings(in long[] masterKeyIds, in boolean asAsciiArmoredStringArray,
            in IApgGetKeyringsHandler handler);
}