# componentless
The Authentic componentless plugin for Bloomreach Experience Manager provides for the addition of Component Dialog Parameters through JCR configuration of the component. This means adding properties to existing components with no coding.
   
## Installation
Installing the componentless plugin into a Hippo CMS project requires two modifications:
   - modify the pom.xml of the site/components module to include the library in the maven build
   - modify the web.xml of the cms module to add a startup listener to initialize the "ParameterInfo override classes" that decorate the out of the box classes with additional functionality.
   
### cms/pom.xml
Add a dependency to the cms/pom.xml to compile in the library classes:

------
                <dependency>
                        <groupId>com.authentic</groupId>
                        <artifactId>authentic-componentless</artifactId>
                        <version>3.0.0</version>
                </dependency>
		
### modify cms/src/main/webapp/WEB-INF/web.xml 
A startup listener must be added to the web.xml of the site module to do some dynamic class loading 

------
                <listener>
                        <listener-class>com.authentic.componentless.initialize.ParamInfoStartupListener</listener-class>
                </listener>

## Usage

### Adding parameters to components
Components can have parameters added to them directly in the console by modifying the "catalog component node". For each parameter, a new child node of the component node is created of type "hst:abstractcomponent". The order of these nodes determines the order of the properties in the dialog. The "hst:abstractcomponent" needs to have the "hippostd:relaxed" mixin applied to allow properties to be added as desired. The parameter is configured by adding properties to control the behavior of the parameter. All features of the ParameterInfo interface are supported. 

Below are two example parameters.

#### Simple String parameter
    /MyTestProperty:
    jcr:primaryType: hst:abstractcomponent
    jcr:mixinTypes: ['hippostd:relaxed']
    jcr:uuid: 4bfb55f8-cb5d-4514-9ba6-3d80329dddfc
    defaultValue: Test Default
    groupLabel: New Group
    label: My New Property
    name: mynewproperty
    
#### Drop down list 
    /MyTestProperty2:
    jcr:primaryType: hst:abstractcomponent
    jcr:mixinTypes: ['hippostd:relaxed']
    jcr:uuid: 92f93b4b-d336-4d0e-9a65-4ddf5e385efe
    dropDownListDisplayValues: [A, B]
    dropDownListValues: [a, b]
    groupLabel: New Group
    label: My Second Property
    name: mynewproperty2
    required: false
    type: VALUE_FROM_LIST
    
    
    
Copyright 2019 AuthX Consulting LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

