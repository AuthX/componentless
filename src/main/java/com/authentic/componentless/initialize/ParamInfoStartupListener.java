package com.authentic.componentless.initialize;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParamInfoStartupListener implements ServletContextListener {
	private static final Logger log = LoggerFactory.getLogger(ParamInfoStartupListener.class);

	public void contextInitialized(ServletContextEvent event) {
    	try {

    		//First install the parameters info processor wrapper for handling REST calls from channel manager
			TypePool typePool = TypePool.Default.of(org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType.class.getClassLoader()); //use shared classloader via common class
			TypeDescription desc = typePool.describe("org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor").resolve();
			TypeDescription desc2 = typePool.describe("com.authentic.componentless.wrapper.ParametersInfoProcessorDelegate").resolve();
			
			new ByteBuddy()
			  .rebase(desc,ClassFileLocator.ForClassLoader.of(this.getClass().getClassLoader()))
			  .method(ElementMatchers.named("getProperties")).intercept(MethodDelegation.to(desc2))
			  .method(ElementMatchers.named("getPopulatedProperties")).intercept(MethodDelegation.to(desc2))
			  .make()
			  .load(this.getClass().getClassLoader());

		} catch (Exception e) {
			log.error("Unable to set up componentless", e);
		}
    }

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// No need to handle context destruction
	}
}
