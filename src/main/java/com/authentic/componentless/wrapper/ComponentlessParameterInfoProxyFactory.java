package com.authentic.componentless.wrapper;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstParameterValueConverter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ParameterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.hippoecm.hst.util.HstRequestUtils.isComponentRenderingPreviewRequest;

public class ComponentlessParameterInfoProxyFactory extends HstParameterInfoProxyFactoryImpl implements HstParameterInfoProxyFactory {
    private static final Logger log = LoggerFactory.getLogger(ComponentlessParameterInfoProxyFactory.class);
    private static final String COMPONENT_PARAMETER_MAP = "componentParameterMap";

    private HstParameterInfoProxyFactory zuper = null;

    @Override
    public <T> T createParameterInfoProxy(ParametersInfo parametersInfo, ParameterConfiguration parameterConfiguration, HttpServletRequest request, HstParameterValueConverter converter) {
        Map<String, String> parameters = parameterConfiguration.getParameters(RequestContextProvider.get().getResolvedSiteMapItem());

        T paramInfoProxy = zuper.createParameterInfoProxy(parametersInfo, parameterConfiguration, request, converter);
        if (isComponentRenderingPreviewRequest(RequestContextProvider.get()) && (request instanceof HstRequest)) {
            Map<String, String> cmsParameters = getCmsParameters(request, parameters);
            request.setAttribute(COMPONENT_PARAMETER_MAP, cmsParameters);
            ((HstRequest) request).setModel(COMPONENT_PARAMETER_MAP, cmsParameters);
        } else if (request instanceof HstRequest) {
            Map<String, String> targetedParameters = getTargetedParameters(request, parameterConfiguration, parameters);
            request.setAttribute(COMPONENT_PARAMETER_MAP, targetedParameters);
            ((HstRequest) request).setModel(COMPONENT_PARAMETER_MAP, targetedParameters);
        }

        // Call zuper instead of super, in case something else has already changed out the proxyfactory
        return paramInfoProxy;
    }

    private Map<String, String> getCmsParameters(HttpServletRequest request, Map<String, String> parameters) {
        // getParameterMap() and getParameterMap("") are not the same thing
        Map<String, String[]> parameterMap = ((HstRequest) request).getParameterMap("");
        HashMap<String, String> cmsParameters = new HashMap<>();

        // Take the parameters the CMS set for us and put them in our return value only if they are supposed to exist
        parameters.forEach((key, value) -> {
            if (!parameterMap.containsKey(key))
                return;
            String[] strings = parameterMap.get(key);
            if (strings == null || strings.length == 0)
                return;
            String strVal = strings[0];
            // We need to de-convert these values to make them consistent with the other parameter map
            if (strVal.equals("true"))
                strVal = "on";
            else if (strVal.equals("false"))
                strVal = "off";
            cmsParameters.put(key, strVal);
        });

        return cmsParameters;
    }

    private Map<String, String> getTargetedParameters(HttpServletRequest request, ParameterConfiguration parameterConfiguration, Map<String, String> parameters) {
        if (parameters.isEmpty())
            return parameters;

        try {
            String componentIdentifier = ((ComponentConfiguration)parameterConfiguration).getCanonicalIdentifier();
            Class targetingStateProvider = Class.forName("com.onehippo.cms7.targeting.TargetingStateProvider");
            Object targetingState = targetingStateProvider.getMethod("get").invoke(null);
            if (targetingState == null) // Null is a valid value here
                return parameters;
            Object targetingProfile = targetingState.getClass().getMethod("getProfile").invoke(targetingState);
            if (targetingProfile == null)
                return parameters;
            String variant = (String) targetingProfile.getClass().getMethod("getPrefixForComponent", String.class).invoke(targetingProfile, componentIdentifier);
            if (variant == null)
                return parameters;

            HashMap<String, String> targetedParameters = new HashMap<>();
            parameters.forEach((key, value) -> {
                // Targeted parameters are always inserted; defaults are inserted if a targeted one doesn't already exist
                if (!variant.isEmpty() && key.startsWith(variant)) // Include params that match currently selected variant
                    targetedParameters.put(key.substring(variant.length() + 1), value);
                else if (!key.startsWith("@")) // Exc   lude prefixed params that weren't previously matched
                    targetedParameters.putIfAbsent(key, value);
            });
            return targetedParameters;

        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log.debug("Targeting is not configured for this application; skipping targeting", e);
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException | NullPointerException e) {
            log.error("The targeting API is malfunctioning or may have changed", e);
        }

        return parameters;
    }

    public void setZuper(HstParameterInfoProxyFactory zuper) {
        this.zuper = zuper;
    }

    protected static class ComponentlessParameterInfoInvocationHandler extends ParameterInfoInvocationHandler implements InvocationHandler {

        public ComponentlessParameterInfoInvocationHandler(final ParameterConfiguration parameterConfiguration, final HttpServletRequest request,
                                                    final HstParameterValueConverter converter,
                                                     final Class<?> parametersInfoType) {
            super(parameterConfiguration, request, converter, parametersInfoType);
        }
    }
}
/*
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
