/*
 * Copyright 2015 Groupon, Inc
 * Copyright 2015 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.jruby;

import java.util.Dictionary;

import org.jruby.Ruby;
import org.killbill.billing.control.plugin.api.OnFailurePaymentControlResult;
import org.killbill.billing.control.plugin.api.OnSuccessPaymentControlResult;
import org.killbill.billing.control.plugin.api.PaymentControlApiException;
import org.killbill.billing.control.plugin.api.PaymentControlContext;
import org.killbill.billing.control.plugin.api.PaymentControlPluginApi;
import org.killbill.billing.control.plugin.api.PriorPaymentControlResult;
import org.killbill.billing.osgi.api.config.PluginRubyConfig;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

public class JRubyPaymentControlPlugin extends JRubyNotificationPlugin implements PaymentControlPluginApi {

    public JRubyPaymentControlPlugin(final PluginRubyConfig config, final BundleContext bundleContext, final LogService logger, final OSGIConfigPropertiesService configProperties) {
        super(config, bundleContext, logger, configProperties);
    }

    @Override
    protected ServiceRegistration doRegisterService(final BundleContext context, final Dictionary<String, Object> props) {
        return context.registerService(PaymentControlPluginApi.class.getName(), this, props);
    }

    @Override
    public PriorPaymentControlResult priorCall(final PaymentControlContext context, final Iterable<PluginProperty> properties) throws PaymentControlApiException {
        return callWithRuntimeAndChecking(new PluginCallback<PriorPaymentControlResult, PaymentControlApiException>() {
            @Override
            public PriorPaymentControlResult doCall(final Ruby runtime) throws PaymentControlApiException {
                return ((PaymentControlPluginApi) pluginInstance).priorCall(context, properties);
            }
        });
    }

    @Override
    public OnSuccessPaymentControlResult onSuccessCall(final PaymentControlContext context, final Iterable<PluginProperty> properties) throws PaymentControlApiException {
        return callWithRuntimeAndChecking(new PluginCallback<OnSuccessPaymentControlResult, PaymentControlApiException>() {
            @Override
            public OnSuccessPaymentControlResult doCall(final Ruby runtime) throws PaymentControlApiException {
                return ((PaymentControlPluginApi) pluginInstance).onSuccessCall(context, properties);
            }
        });
    }

    @Override
    public OnFailurePaymentControlResult onFailureCall(final PaymentControlContext context, final Iterable<PluginProperty> properties) throws PaymentControlApiException {
        return callWithRuntimeAndChecking(new PluginCallback<OnFailurePaymentControlResult, PaymentControlApiException>() {
            @Override
            public OnFailurePaymentControlResult doCall(final Ruby runtime) throws PaymentControlApiException {
                return ((PaymentControlPluginApi) pluginInstance).onFailureCall(context, properties);
            }
        });
    }
}
