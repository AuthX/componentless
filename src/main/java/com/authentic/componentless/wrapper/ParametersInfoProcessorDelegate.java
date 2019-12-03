package com.authentic.componentless.wrapper;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ParameterType;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class ParametersInfoProcessorDelegate {
	private static final Logger log = LoggerFactory.getLogger(ParametersInfoProcessorDelegate.class);
	
	private static ThreadLocal<Node> threadLocal = new ThreadLocal<>();

	private ParametersInfoProcessorDelegate() {}

	public static List<ContainerItemComponentPropertyRepresentation> getProperties(ParametersInfo parameterInfo,
			Locale locale, String contentPath, @SuperCall Callable<List<ContainerItemComponentPropertyRepresentation>> zuper) {
		
		List<ContainerItemComponentPropertyRepresentation> rtn = null;
		
		try {
			rtn = zuper.call();
			
			if (rtn == null)
				rtn = new ArrayList<>();
			
			//add the properties from our JcrConfig
			addPropertiesFromJcrConfig(threadLocal.get(), rtn, contentPath);
			
		} catch (Exception e) {
			log.error("Error in getting properties for component", e);
		}

		
		threadLocal.set(null);
		return rtn;
	}
	
	public static List<ContainerItemComponentPropertyRepresentation> getPopulatedProperties(
			ParametersInfo parametersInfo, Locale locale, String contentPath, String prefix, Node containerItemNode,
			ContainerItemHelper containerItemHelper, List<PropertyRepresentationFactory> propertyPresentationFactories,
			@SuperCall Callable<List<ContainerItemComponentPropertyRepresentation>> zuper) throws RepositoryException{
		
		threadLocal.set(containerItemNode);
		
		List<ContainerItemComponentPropertyRepresentation> rtn = null;
		
		try {
			rtn = zuper.call();
		} catch (Exception e) {
			log.error("Error in getting populated properties for component", e);
		}

		return rtn;
	}
	
	private static void addPropertiesFromJcrConfig(Node jcrItemNode, List<ContainerItemComponentPropertyRepresentation> properties, String contentPath) throws RepositoryException {
		//groupLabel defines a new group
		NodeIterator children = jcrItemNode.getNodes();

		while (children.hasNext()) {
			Node child = children.nextNode();
			try {
				if (!"hst:abstractcomponent".equalsIgnoreCase(child.getPrimaryNodeType().getName()))
					continue;

				addChildNodeProperties(child, properties, contentPath);
			} catch (RepositoryException e) {
				log.warn("Unable to add properties for child node", e);
			}
		}
	}

	private static void addChildNodeProperties(Node child, List<ContainerItemComponentPropertyRepresentation> properties, String contentPath) throws RepositoryException {
		PropertyIterator childProperties = child.getProperties();
		ContainerItemComponentPropertyRepresentation newProperty = new ContainerItemComponentPropertyRepresentation();
		ContainerItemComponentPropertyRepresentation oldProperty = newProperty;
		String propertyName = child.getProperty("name").getString();
		int oldPropertyIndex = findExistingProperty(properties, propertyName);
		if (oldPropertyIndex >-1)
			oldProperty = properties.get(oldPropertyIndex);

		while (childProperties.hasNext()){
			Property prop = childProperties.nextProperty();
			Field field = null;

			try {
				field = newProperty.getClass().getDeclaredField(prop.getName());
			} catch (NoSuchFieldException e) {
				log.info("Iterated no such field", e);
				//this will happen when a field doesn't exist, eat the exception
			}

			if (field == null)
				continue;

			field.setAccessible(true);

			configureFieldProperty(field, prop, oldProperty, newProperty);
		}

		if (!StringUtils.isEmpty(newProperty.getPickerInitialPath())) {
			if (StringUtils.isEmpty(newProperty.getPickerRootPath()))
				newProperty.setPickerRootPath(contentPath);
			if (StringUtils.isEmpty(oldProperty.getPickerRootPath()))
				oldProperty.setPickerRootPath(contentPath);
		}

		if (oldPropertyIndex == -1){
			properties.add(newProperty);
		}else if (newProperty.getGroupLabel()!=null && newProperty.getGroupLabel().trim().length()>0){
			properties.remove(oldPropertyIndex);
			properties.add(oldProperty);
		}
	}

	private static void configureFieldProperty(Field field, Property prop, ContainerItemComponentPropertyRepresentation oldProperty, ContainerItemComponentPropertyRepresentation newProperty) throws RepositoryException {
		//property should only be of type Boolean, String, or String[] with the exception of the type
		try {
			if ("type".equals(prop.getName()) && prop.getType()==PropertyType.STRING && !prop.isMultiple()) {
				newProperty.setType(ParameterType.valueOf(prop.getString()));
				oldProperty.setType(ParameterType.valueOf(prop.getString()));
			} else if (prop.getType()== PropertyType.BOOLEAN){
				field.set(newProperty, prop.getBoolean());
				field.set(oldProperty, prop.getBoolean());

			} else if (prop.getType()== PropertyType.STRING && !prop.isMultiple() ){
				field.set(newProperty, prop.getString());
				field.set(oldProperty, prop.getString());

			} else if (prop.getType()== PropertyType.STRING && prop.isMultiple() ){
				Value[] vals= prop.getValues();
				String[] values= new String[vals.length];
				for(int i =0 ; i < vals.length; i++){
					values[i]=vals[i].getString();
				}
				field.set(newProperty, values);
				field.set(oldProperty, values);

			}
		} catch (IllegalAccessException e) {
			log.warn("Illegal access exception while configuring a field property", e);
		}
	}

	private static int findExistingProperty(List<ContainerItemComponentPropertyRepresentation> properties, String propertyName){
		for (int i =0; i< properties.size(); i++ ) {
			ContainerItemComponentPropertyRepresentation oldProperty = properties.get(i);
			//if property exists we will merge them
			if (oldProperty !=null && oldProperty.getName()!=null && oldProperty.getName().equals(propertyName)){
				return i;
			}
		}
		return -1;
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
