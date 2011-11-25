/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

package org.mule.module.dynamic;

import java.util.List;

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.tools.module.browsing.NexusBrowser;

import org.sonatype.nexus.client.NexusClientException;
import org.sonatype.nexus.client.NexusConnectionException;
import org.sonatype.nexus.rest.model.NexusArtifact;

/**
 * Dynamic Module
 *
 * @author MuleSoft, Inc.
 */
@Module(name="dynamic", schemaVersion="1.0-SNAPSHOT")
public class DynamicModule {

    private final NexusBrowser browser = new NexusBrowser();

    /**
     * List all MuleForge modules.
     *
     * {@sample.xml ../../../doc/Dynamic-module.xml.sample dynamic:list-modules}
     *
     * @return ee
     */
    @Processor
    public List<List<NexusArtifact>> listModules() throws NexusClientException, NexusConnectionException {
        return this.browser.listArtifacts();
    }

}