/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.killbill.osgi.libs.killbill;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSGIKillbillEventDispatcher extends OSGIKillbillLibraryBase {

    private final Logger logger = LoggerFactory.getLogger(OSGIKillbillEventDispatcher.class);

    private static final String OBSERVABLE_SERVICE_NAME = "java.util.Observable";

    private final ServiceTracker observableTracker;

    private final Map<Object, Observer> handlerToObserver;

    public OSGIKillbillEventDispatcher(final BundleContext context) {
        handlerToObserver = new HashMap<Object, Observer>();
        observableTracker = new ServiceTracker(context, OBSERVABLE_SERVICE_NAME, null);
        observableTracker.open();
    }

    public void close() {
        if (observableTracker != null) {
            observableTracker.close();
        }
        handlerToObserver.clear();
    }

    public void registerEventHandler(final OSGIKillbillEventHandler handler) {
        final Observer observer = new Observer() {
            @Override
            public void update(final Observable o, final Object arg) {
                if (!(arg instanceof ExtBusEvent)) {
                    logger.debug("OSGIKillbillEventDispatcher unexpected event type " + (arg != null ? arg.getClass() : "null"));
                    return;
                }

                //
                // This is similar to what we did for API calls through ContextClassLoaderHelper, where we ensure that
                // plugin is called with a ContextClassLoader correctly initialized with the one from the bundle
                //
                final ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(handler.getClass().getClassLoader());
                try {
                    handler.handleKillbillEvent((ExtBusEvent) arg);
                } finally {
                    Thread.currentThread().setContextClassLoader(initialContextClassLoader);
                }
            }
        };
        registerEventHandler(handler, observer);
    }

    public void registerEventHandler(final OSGIFrameworkEventHandler handler) {
        final Observer observer = new Observer() {
            @Override
            public void update(final Observable o, final Object arg) {
                if (!(arg instanceof Event)) {
                    logger.debug("OSGIFrameworkEventHandler unexpected event type " + (arg != null ? arg.getClass() : "null"));
                    return;
                }

                final String topic = ((Event) arg).getTopic();

                //
                // This is similar to what we did for API calls through ContextClassLoaderHelper, where we ensure that
                // plugin is called with a ContextClassLoader correctly initialized with the one from the bundle
                //
                final ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(handler.getClass().getClassLoader());
                try {
                    if ("org/killbill/billing/osgi/lifecycle/STARTED".equals(topic)) {
                        handler.started();
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(initialContextClassLoader);
                }
            }
        };
        registerEventHandler(handler, observer);
    }

    public void registerEventHandler(final Object handler, final Observer observer) {
        withServiceTracker(observableTracker,
                           new APICallback<Void, Observable>(OBSERVABLE_SERVICE_NAME) {
                               @Override
                               public Void executeWithService(final Observable service) {
                                   handlerToObserver.put(handler, observer);
                                   service.addObserver(observer);
                                   return null;
                               }
                           });
    }

    public void unregisterEventHandler(final Object handler) {
        withServiceTracker(observableTracker,
                           new APICallback<Void, Observable>(OBSERVABLE_SERVICE_NAME) {
                               @Override
                               public Void executeWithService(final Observable service) {
                                   final Observer observer = handlerToObserver.get(handler);
                                   if (observer != null) {
                                       service.deleteObserver(observer);
                                       handlerToObserver.remove(handler);
                                   }
                                   return null;
                               }
                           });
    }

    public void unregisterAllHandlers() {
        withServiceTracker(observableTracker,
                           new APICallback<Void, Observable>(OBSERVABLE_SERVICE_NAME) {
                               @Override
                               public Void executeWithService(final Observable service) {
                                   // Go through all known handlers (OSGIFrameworkEventHandler and OSGIKillbillEventHandler)
                                   // and remove them from the list of Observers
                                   for (final Object handler : handlerToObserver.keySet()) {
                                       final Observer observer = handlerToObserver.get(handler);
                                       if (observer != null) {
                                           service.deleteObserver(observer);
                                       }
                                   }
                                   handlerToObserver.clear();
                                   return null;
                               }
                           });
    }



    public interface OSGIKillbillEventHandler {

        public void handleKillbillEvent(final ExtBusEvent killbillEvent);
    }

    public interface OSGIFrameworkEventHandler {

        public void started();
    }
}
