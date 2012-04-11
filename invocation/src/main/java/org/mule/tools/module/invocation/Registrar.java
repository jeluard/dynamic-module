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

package org.mule.tools.module.invocation;

import java.util.Map;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.tools.module.helper.LifeCycles;
import org.mule.tools.module.helper.MuleContexts;
import org.mule.tools.module.helper.Reflections;

public class Registrar implements Stoppable {

    private static class ListenerWrapper implements MessageProcessor {

        private final DynamicModule.Listener listener;

        private ListenerWrapper(final DynamicModule.Listener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("null listener");
            }

            this.listener = listener;
        }

        @Override
        public final MuleEvent process(final MuleEvent event) throws MuleException {
            this.listener.onEvent(event);
            return event;
        }

    }

    private final MuleContext context;
    private final MessageSource messageSource;

    public Registrar(final MuleContext context, final MessageSource messageSource) {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }
        if (messageSource == null) {
            throw new IllegalArgumentException("null messageSource");
        }

        this.context = context;
        this.messageSource = messageSource;

        try {
            initialise();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initialise() throws InitialisationException, MuleException {
        MuleContexts.inject(this.messageSource, this.context);

        LifeCycles.initialise(this.messageSource);
    }

    public final void start(final Map<String, Object> sourceParameters, final DynamicModule.Listener listener) throws MuleException {
        Reflections.set(this.messageSource, sourceParameters);

        this.messageSource.setListener(new ListenerWrapper(listener));

        LifeCycles.start(this.messageSource);
    }

    @Override
    public final void stop() throws MuleException {
        LifeCycles.stop(this.messageSource);
    }

}