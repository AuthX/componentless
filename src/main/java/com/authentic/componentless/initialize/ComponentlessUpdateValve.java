package com.authentic.componentless.initialize;

import com.authentic.componentless.wrapper.ComponentlessParameterInfoProxyFactory;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;

public class ComponentlessUpdateValve extends AbstractOrderableValve {
    private static final ComponentlessParameterInfoProxyFactory PARAMETER_INFO_PROXY_FACTORY = new ComponentlessParameterInfoProxyFactory();

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HstMutableRequestContext requestContext = (HstMutableRequestContext) context.getRequestContext();

        // now we set a different parameterInfoProxyFactory on the HstRequestContext because we want
        // to depending on the current persona fetch a prefixed parameter name if it is available
        HstParameterInfoProxyFactory zuper = requestContext.getParameterInfoProxyFactory();
        PARAMETER_INFO_PROXY_FACTORY.setZuper(zuper);
        requestContext.setParameterInfoProxyFactory(PARAMETER_INFO_PROXY_FACTORY);

        context.invokeNext();
    }
}
