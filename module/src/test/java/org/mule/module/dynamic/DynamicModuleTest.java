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

import java.util.Collection;

import org.junit.Assert;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.AbstractMuleTestCase;
import org.junit.Test;

public class DynamicModuleTest extends FunctionalTestCase {

    @Override
    protected String getConfigResources() {
        return "mule-config.xml";
    }

    @Test
    public void testFlow() throws Exception {
        Assert.assertFalse(Collection.class.cast(runFlow("list-ids")).isEmpty());
    }

    /**
    * Run the flow specified by name and return the output
    *
    * @param flowName The name of the flow to run
    * @return expect The output
    */
    protected <T> T runFlow(final String flowName) throws Exception {
        Flow flow = lookupFlowConstruct(flowName);
        MuleEvent event = AbstractMuleTestCase.getTestEvent(null);
        MuleEvent responseEvent = flow.process(event);

        return (T) responseEvent.getMessage().getPayload();
    }

    /**
     * Retrieve a flow by name from the registry
     *
     * @param name Name of the flow to retrieve
     */
    protected Flow lookupFlowConstruct(final String name) {
        return (Flow) AbstractMuleTestCase.muleContext.getRegistry().lookupFlowConstruct(name);
    }

}