/**
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright 2012 Julien Eluard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     [http://www.apache.org/licenses/LICENSE-2.0]
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.mule.tools.module.discovery.wagon;

/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.sonatype.aether.connector.wagon.WagonProvider;

public class ManualWagonProvider implements WagonProvider {

    @Override
    public final Wagon lookup(final String roleHint) throws Exception {
        if ("file".equals(roleHint)) {
            return new FileWagon();
        } else if ("http".equals(roleHint)) {
            return new LightweightHttpWagon();
        }
        return null;
    }

    @Override
    public final void release(final Wagon wagon) {
    }

}