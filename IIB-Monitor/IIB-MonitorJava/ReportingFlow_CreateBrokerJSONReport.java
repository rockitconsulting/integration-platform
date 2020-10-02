import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import com.ibm.broker.config.proxy.AdministeredObject;
import com.ibm.broker.config.proxy.ApplicationProxy;
import com.ibm.broker.config.proxy.BrokerProxy;
import com.ibm.broker.config.proxy.ConfigManagerProxyException;
import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.ConfigurableService;
import com.ibm.broker.config.proxy.ExecutionGroupProxy;
import com.ibm.broker.config.proxy.LibraryProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy.Node;
import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbJSON;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;


public class ReportingFlow_CreateBrokerJSONReport extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		MbOutputTerminal alt = getOutputTerminal("alternate");
		BrokerProxy bproxy = null;
		try {
			// optionally copy message headers
			//copyMessageHeaders(inMessage, outMessage);
			
			// ----------------------------------------------------------
			// Add user code below
			bproxy = BrokerProxy.getLocalInstance();
			
			//Creating environment entries for ELK
			MbElement varElement = inAssembly.getGlobalEnvironment().getRootElement().createElementAsLastChild( MbElement.TYPE_NAME, 
					"Variables", null);
			varElement.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, 
					"ELKIndex", null);
			varElement.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, 
					"ELKDatatype", null);
			
			//output broker message
			outputBroker(inAssembly, out, alt, bproxy);
						
			//Output execution group messages
			outputExecutionGroups(inAssembly, out, bproxy);

			//Output application messages
			outputApplication(inAssembly, out, bproxy);
			
			// End of user code
			// ----------------------------------------------------------
		} catch (MbException e) {
			// Re-throw to allow Broker handling of MbException
			throw e;
		} catch (RuntimeException e) {
			// Re-throw to allow Broker handling of RuntimeException
			throw e;
		} catch (Exception e) {
			// Consider replacing Exception with type(s) thrown by user code
			// Example handling ensures all exceptions are re-thrown to be handled in the flow
			throw new MbUserException(this, "evaluate()", "", "", e.toString(),
					null);
		}
		finally {
			if (bproxy != null) {
				bproxy.disconnect();							
			}
		}
	}

	private void outputApplication(MbMessageAssembly inAssembly,
			MbOutputTerminal out, BrokerProxy bproxy) throws MbException, UnknownHostException,
			ConfigManagerProxyException {
		MbMessageAssembly outAssembly;
		MbElement outBody;
		MbElement outJsonData;
		
		Enumeration<ExecutionGroupProxy> enumGroups = bproxy.getExecutionGroups(null);
		String indexName = "edc_infra_application";
		while (enumGroups.hasMoreElements()) {
			ExecutionGroupProxy executionGroupProxy = (ExecutionGroupProxy) enumGroups
					.nextElement();
			
			Enumeration<ApplicationProxy> enumApps = executionGroupProxy.getApplications(null);
			
			while (enumApps.hasMoreElements()) {
				ApplicationProxy applicationProxy = (ApplicationProxy) enumApps
						.nextElement();
				
				outAssembly = createMessageAssembly(inAssembly);
				
				//set ELK index and data type
				setElkEnvironment(outAssembly, indexName, "brokerreport");
				
				outBody = outAssembly.getMessage().getRootElement().createElementAsLastChild(MbJSON.PARSER_NAME);	
				
				outJsonData = outBody.createElementAsLastChild(MbElement.TYPE_NAME, MbJSON.DATA_ELEMENT_NAME, null);
				createJsonProperty(outJsonData, "@timestamp", BrokerProxyService.toSimpleDateFormat(new Date()));
				createJsonProperty(outJsonData, "IntegrationNode", bproxy.getName());
				createJsonProperty(outJsonData, "IntegrationServer", executionGroupProxy.getName());
				
				MbElement outExGroup = createJsonObject(outJsonData, "Application");
				displayComponentInfo(applicationProxy, outExGroup);
				
				out.propagate(outAssembly);
			}
			
		}
		
	}
	
	private MbMessageAssembly createMessageAssembly(MbMessageAssembly inAssembly) throws MbException {
		// create new empty message
		MbMessage outMessage = new MbMessage();
		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = new MbMessageAssembly(inAssembly, outMessage);
		
		//copy the property header and set UTF-8 ccsid
		MbElement outRoot = outMessage.getRootElement();
		MbElement header = inMessage.getRootElement().getFirstChild().copy();
		header.getFirstElementByPath("CodedCharSetId").setValue(1208);
		outRoot.addAsLastChild(header);
		
		return outAssembly;
	}

	private void outputBroker(MbMessageAssembly inAssembly, MbOutputTerminal out, MbOutputTerminal alt,
			BrokerProxy bproxy) throws MbException, UnknownHostException,
			ConfigManagerProxyException {
		
		// create new output message
		MbMessageAssembly outAssembly = createMessageAssembly(inAssembly);

		//set ELK index and data type
		String indexName = "edc_infra_broker";
		setElkEnvironment(outAssembly, indexName, "brokerreport");
		
		MbElement outBody = outAssembly.getMessage().getRootElement().createElementAsLastChild(MbJSON.PARSER_NAME);
		
		MbElement outJsonData = 
				outBody.createElementAsLastChild(MbElement.TYPE_NAME, MbJSON.DATA_ELEMENT_NAME, null);
		// add CURRENT_TIMESTAMP to the out message
//			Calendar ts = Calendar.getInstance();
//			MbTimestamp mbNow = new MbTimestamp(ts.get(Calendar.YEAR), ts.get(Calendar.MONTH), ts.get(Calendar.DATE), ts.get(Calendar.HOUR_OF_DAY), ts.get(Calendar.MINUTE), ts.get(Calendar.SECOND));
		createJsonProperty(outJsonData, "@timestamp", BrokerProxyService.toSimpleDateFormat(new Date()));

		MbElement outBrokerReport = createJsonObject(outJsonData, "BrokerReport");

		// add Hostname to the out message
		createJsonProperty(outBrokerReport, "Hostname", InetAddress.getLocalHost().getHostName());
		  
		createBrokerReport(bproxy, outBrokerReport);
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly, false);

		alt.propagate(outAssembly);
	}

	private void setElkEnvironment(MbMessageAssembly outAssembly, String elkIndex, String elkDatatype)
			throws MbException {
		MbElement envElement = outAssembly.getGlobalEnvironment().getRootElement().getFirstElementByPath("Variables/ELKIndex");
		envElement.setValue(elkIndex);
		envElement = outAssembly.getGlobalEnvironment().getRootElement().getFirstElementByPath("Variables/ELKDatatype");
		envElement.setValue(elkDatatype);
	}

	private void outputExecutionGroups(MbMessageAssembly inAssembly,
			MbOutputTerminal out, BrokerProxy bproxy)
			throws ConfigManagerProxyPropertyNotInitializedException,
			MbException, ConfigManagerProxyLoggedException,
			UnknownHostException {
		
		MbMessageAssembly outAssembly;
		MbElement outBody;
		MbElement outJsonData;
		
		String indexName = "edc_infra_executiongroup";
		Enumeration<ExecutionGroupProxy> enumGroups = bproxy.getExecutionGroups(null);
		while (enumGroups.hasMoreElements()) {
			ExecutionGroupProxy executionGroupProxy = (ExecutionGroupProxy) enumGroups
					.nextElement();
			
			outAssembly = createMessageAssembly(inAssembly);
			
			//set ELK index and data type
			setElkEnvironment(outAssembly, indexName, "brokerreport");

			outBody = outAssembly.getMessage().getRootElement().createElementAsLastChild(MbJSON.PARSER_NAME);	
			
			outJsonData = outBody.createElementAsLastChild(MbElement.TYPE_NAME, MbJSON.DATA_ELEMENT_NAME, null);
			createJsonProperty(outJsonData, "@timestamp", BrokerProxyService.toSimpleDateFormat(new Date()));
			
			MbElement outExGroup = createJsonObject(outJsonData, "IntegrationServer");
			displayComponentInfo(executionGroupProxy, outExGroup);
			
			out.propagate(outAssembly);
		}
	}

	public void copyMessageHeaders(MbMessage inMessage, MbMessage outMessage)
			throws MbException {
		MbElement outRoot = outMessage.getRootElement();

		// iterate though the headers starting with the first child of the root
		// element
		MbElement header = inMessage.getRootElement().getFirstChild();
		while (header != null && header.getNextSibling() != null) // stop before
																	// the last
																	// child
																	// (body)
		{
			// copy the header and add it to the out message
			outRoot.addAsLastChild(header.copy());
			// move along to next header
			header = header.getNextSibling();
		}
	}
	
    private void createBrokerReport(BrokerProxy b, MbElement outBrokerReport)
    throws ConfigManagerProxyException, MbException, UnknownHostException {
             
	    MbElement outBroker = createJsonObject(outBrokerReport, "IntegrationNode");
	    displayComponentInfo(b, outBroker);
	    getSubComponents(b, outBroker);
    }    

    
    private void getSubComponents(AdministeredObject ParentAO, MbElement ParentOut) 
    		throws 	ConfigManagerProxyPropertyNotInitializedException, 
    				MbException, 
    				ConfigManagerProxyLoggedException, 
    				UnknownHostException 
    {
    	Enumeration<AdministeredObject> thisAO = ParentAO.getManagedSubcomponents(null);
    	MbElement ChildOut = ParentOut;
    	MbElement childArray = ParentOut;
    	String prevComponent = null;
    	while (thisAO.hasMoreElements()) {
    		AdministeredObject ChildA0 = thisAO.nextElement();
    		String Component = ChildA0.getConfigurationObjectType().toString();

    		// Name Transformation
    		if (Component.equals("MessageProcessingNodeType")) {
    			Component = "MessageFlow";
    		}
    		else if (Component.equals("ExecutionGroup")) {
    			Component = "IntegrationServer";
    		}
    		if (!Component.equals("ResourceManager") 
    				&& !Component.equals("EventManager")
    				&& !Component.equals("WebAdmin")
    				&& !Component.equals("Log")
    				&& !Component.equals("AdminQueue")) {
    			if (prevComponent == null || !prevComponent.equals(Component)) {    				
    				childArray = createJsonArray(ParentOut, Component);
    			}
    			prevComponent = Component;
    			ChildOut = createJsonProperty_InArrayEmpty(childArray);
    			displayComponentInfo(ChildA0, ChildOut);

    			getSubComponents(ChildA0, ChildOut);
    		}
    	}
    }

    
    private void displayComponentInfo(AdministeredObject ao, MbElement outRef) 
    		throws	ConfigManagerProxyPropertyNotInitializedException, 
    				MbException, 
    				ConfigManagerProxyLoggedException, 
    				UnknownHostException 
    {
    	// for all administered objects
    	createJsonProperty(outRef, "Name", ao.getName());
    	//outRef.createElementAsLastChild(MbXMLNSC.FIELD, "UUID", ao.getUUID());    	
    	createJsonProperty(outRef, "ConfigurationObjectType", ao.getConfigurationObjectType().toString());
    	createJsonProperty(outRef, "Type", ao.getType());
    	createJsonProperty(outRef, "ShortDescription", ao.getShortDescription());
    	createJsonProperty(outRef, "LongDescription", ao.getLongDescription());
    	createJsonProperty(outRef, "hasBeenPopulatedByBroker", ao.hasBeenPopulatedByBroker());
    	createJsonProperty(outRef, "hasBeenRestrictedByBroker", ao.hasBeenRestrictedByBroker());
    	createJsonProperty(outRef, "LastBIPMessages", BrokerProxyService.getStringValue(ao.getLastBIPMessages()));
    	createJsonProperty(outRef, "NumberOfSubcomponents", ao.getNumberOfSubcomponents());
    	
    	createJsonStructureFromProperties(ao.getBasicProperties(), "BasicProperties", outRef);
    	createJsonStructureFromProperties(ao.getAdvancedProperties(), "AdvancedProperties", outRef);
    	//createJsonStructureFromProperties(ao.getProperties(), "Properties", outRef);

    	
//    	String Component = ao.getConfigurationObjectType().toString();
    	String Component = ao.getType();
    	switch (Component)	{
	    	case "Broker": 						displayBrokerInfo( (BrokerProxy) ao, outRef);
	    										break;
	    	case "ExecutionGroup": 				displayEgInfo( (ExecutionGroupProxy) ao, outRef); // includes message sets 
												break;    				
	    	case "Application": 				displayApplicationInfo( (ApplicationProxy) ao, outRef);
	    										break;
	    	case "Library": 					displayLibInfo( (LibraryProxy) ao, outRef);
												break;
//	    	case "SharedLibrary": 				displayLibInfo( (SharedLibraryProxy) ao, outRef);
//												break;    																
	    	case "MessageProcessingNodeType": 	displayMessageFlowInfo( (MessageFlowProxy) ao, outRef);
												break;
//	    	case "DataCapure": 					displayDataCaptureInfo( (DataCaptureProxy) ao, outRef);
//												break;    				
//	    	case "ResourceManager": 			displayRmInfo( (ResourceManagerProxy) ao, outRef);
//												break;    				
//	    	case "ActivityLog": 				displayActivityLogInfo( (ActivityLogProxy) ao, outRef);
//												break;   
//			case "AdminQueue": 					displayAdminQueueInfo( (AdminQueueProxy) ao, outRef);
//												break;    				
//	    	case "Log": 						displayLogInfo( (LogProxy) ao, outRef);
//												break;    				
//	    	case "WebAdmin": 					displayWebAdminInfo( (WebAdminProxy) ao, outRef);
//												break;    				
//	    	case "WebUser": 					displayWebUserInfo( (WebUserProxy) ao, outRef);
//												break;   				
    	}
    	
	}
	
	
    private void displayBrokerInfo(BrokerProxy thisBroker, MbElement outBroker)
    throws ConfigManagerProxyPropertyNotInitializedException, MbException, UnknownHostException {

	    createJsonProperty(outBroker, "isRunning", thisBroker.isRunning());
	    
	    createJsonProperty(outBroker, "BrokerVersion", thisBroker.getBrokerVersion());	    
	    createJsonProperty(outBroker, "BrokerLongVersion", thisBroker.getBrokerLongVersion());
	    createJsonProperty(outBroker, "UUID", thisBroker.getUUID());
	    createJsonProperty(outBroker, "OperationMode", thisBroker.getOperationMode());
	    
	    createJsonProperty(outBroker, "OSArch", thisBroker.getBrokerOSArch());
	    createJsonProperty(outBroker, "OSName", thisBroker.getBrokerOSName());
	    createJsonProperty(outBroker, "OSVersion", thisBroker.getBrokerOSVersion());
	    
	    createJsonProperty(outBroker, "QueueManagerName", thisBroker.getQueueManagerName());
	    
	    createJsonProperty(outBroker, "TimeOfLastUpdate", thisBroker.getTimeOfLastUpdate().getTime().toString());
	    createJsonProperty(outBroker, "TimeOfLastCompletionCode", thisBroker.getTimeOfLastCompletionCode().getTime().toString());
	    createJsonProperty(outBroker, "LastCompletionCode", thisBroker.getLastCompletionCode().toString());
	    
//	    outBroker.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Queues", Arrays.toString(thisBroker.getQueues()));
	    // Array aufgedroeselt und Queues einzeln ausgegeben:
//	    MbElement outQueues = outBroker.createElementAsLastChild(MbXMLNSC.FOLDER, "Queues", null);
	    //MQInformationService.getQueues(outQueues);
	   
//	    for (String q : thisBroker.getQueues()) {
//	    	outQueues.createElementAsLastChild(MbXMLNSC.FIELD, "Queue", q);
//	    }
	    
	    
//	    outBroker.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "CacheManagerPropertyNames", Arrays.toString(thisBroker.getCacheManagerPropertyNames()));
//	    outBroker.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "HTTPListenerPropertyNames", Arrays.toString(thisBroker.getHTTPListenerPropertyNames()));

	    //Configurable Services
		String[] availableCSTypes = thisBroker.getConfigurableServiceTypes();
		Arrays.sort(availableCSTypes);
		if (availableCSTypes.length > 0) {
			MbElement servicesElement = createJsonArray(outBroker, "Configurable Services");
			
			for (int indT = 0; indT < availableCSTypes.length; indT++) {
				ConfigurableService[] csa = thisBroker
						.getConfigurableServices(availableCSTypes[indT]);
				for (int i = 0; i < csa.length; i++) {
					MbElement sElement = createJsonProperty_InArrayEmpty(servicesElement);

					createJsonProperty(sElement, "Type", csa[i].getType());
					createJsonProperty(sElement, "Name", csa[i].getName());
					createJsonStructureFromProperties(csa[i].getProperties(), "Properties", sElement);
				}
			}
		}

    }
    
    
    private void displayEgInfo(ExecutionGroupProxy thisExecutionGroup, MbElement outEG)
    throws ConfigManagerProxyPropertyNotInitializedException, ConfigManagerProxyLoggedException, MbException {
 
        createJsonProperty(outEG, "isRunning", thisExecutionGroup.isRunning());
       
        createJsonProperty(outEG, "isRunEnabled", thisExecutionGroup.isRunEnabled());

        
        createJsonProperty(outEG, "DebugPort", thisExecutionGroup.getDebugPort());
        createJsonProperty(outEG, "isDebugPortActive", thisExecutionGroup.isDebugPortActive());
        
//	    outEG.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Queues", Arrays.toString(thisExecutionGroup.getQueues()));
        
//        outEG.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "RuntimePropertyNames", Arrays.toString(thisExecutionGroup.getRuntimePropertyNames()));
 
    }

    
    private void displayMessageFlowInfo(MessageFlowProxy thisFlow, MbElement outFlow)
    throws ConfigManagerProxyPropertyNotInitializedException, ConfigManagerProxyLoggedException, MbException {
        
        createJsonProperty(outFlow, "FullName", thisFlow.getFullName());
        createJsonProperty(outFlow, "FileExtension", thisFlow.getFileExtension());
        createJsonProperty(outFlow, "isRunning", thisFlow.isRunning());
       
        createJsonProperty(outFlow, "Version", thisFlow.getVersion());
        
        createJsonProperty(outFlow, "BARFileName", thisFlow.getBARFileName());
        createJsonProperty(outFlow, "DeployTime", BrokerProxyService.toSimpleDateFormat(thisFlow.getDeployTime()));
        createJsonProperty(outFlow, "ModifyTime", BrokerProxyService.toSimpleDateFormat(thisFlow.getModifyTime()));
 
        createJsonProperty(outFlow, "DeployProperties", thisFlow.getDeployProperties().toString());
        createJsonProperty(outFlow, "Keywords", Arrays.toString(thisFlow.getKeywords()));
        
        createJsonProperty(outFlow, "StartMode", thisFlow.getStartMode());
        createJsonProperty(outFlow, "AdditionalInstances", thisFlow.getAdditionalInstances());
        
        String[] udpNames = thisFlow.getUserDefinedPropertyNames();
        if (udpNames.length > 0) { 
        	MbElement outUdpArray =createJsonArray(outFlow, "UserDefinedProperties");
        	for (int i = 0; i < udpNames.length; i++) {
        		String udpName = udpNames[i];
        		MbElement outUdpItem = createJsonProperty_InArrayEmpty(outUdpArray);
        		createJsonProperty(outUdpItem, "Name", udpName);
        		createJsonProperty(outUdpItem, "Value", BrokerProxyService.getStringValue(thisFlow.getUserDefinedProperty(udpName)));
        	}
        }
        createJsonProperty(outFlow, "Queues", Arrays.toString(thisFlow.getQueues()));
//        outFlow.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "RuntimePropertyNames", Arrays.toString(thisFlow.getRuntimePropertyNames()));
        
        MbElement outNodes = createJsonArray(outFlow, "Nodes");
	   	Enumeration<Node> AllNodes = thisFlow.getNodes();
	   	while (AllNodes.hasMoreElements()) {
	   		Node thisNode = AllNodes.nextElement();

	   		MbElement outNode = createJsonProperty_InArrayEmpty(outNodes);
	   		outNode = createJsonArray(outNode, "Node");
	   		outNode = createJsonProperty_InArrayEmpty(outNode);
	   		createJsonProperty(outNode, "Name", thisNode.getName());
	   		createJsonProperty(outNode, "Type", thisNode.getType());
	   		outNode = createJsonArray(outNode, "NodeProperties");
	        Properties nodeProperties = thisNode.getProperties();
	        Enumeration<?> propertiesEnum = nodeProperties.keys();
	        while (propertiesEnum.hasMoreElements()) {
	        	MbElement outNodeProps = createJsonProperty_InArrayEmpty(outNode);
	            String propertiesKey = ""+propertiesEnum.nextElement();
	            String propertiesValue = nodeProperties.getProperty(propertiesKey);
	            createJsonProperty(outNodeProps, "Name", propertiesKey);
	            createJsonProperty(outNodeProps, "Value", propertiesValue);
	        }
	   		
	   	}
	   	
    }
    

    private void displayLibInfo(LibraryProxy thisLib, MbElement outLib)
    		throws ConfigManagerProxyPropertyNotInitializedException, MbException {
    	createJsonProperty(outLib, "DeployProperties", thisLib.getDeployProperties().toString());
	}
    
    
 	private void createJsonStructureFromProperties(Properties props, String folder, MbElement outRef) 
	throws MbException {
 		
		MbElement outRefProps = outRef;		
 		if (folder != "")  {
 			outRefProps = outRef.createElementAsLastChild(MbElement.TYPE_NAME, folder, null);
 			}

	 	Enumeration<Object> e = props.keys();
	 	 while (e.hasMoreElements()) {
	 		 String key = e.nextElement().toString(); 
	 		 String value = props.getProperty(key);
	 		 if (value != null && value.isEmpty() == false) {
	 			 outRefProps.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, key, value);
//	 		 String[] parts = key.replace(".", "/").split("/");
	 		 }
	      }	   
 	}

    
    private void displayApplicationInfo(ApplicationProxy thisApp, MbElement outApp)
    throws ConfigManagerProxyPropertyNotInitializedException, ConfigManagerProxyLoggedException, MbException {
    	
        createJsonProperty(outApp, "isRunning", thisApp.isRunning());
        createJsonProperty(outApp, "FullName", thisApp.getFullName());
        createJsonProperty(outApp, "DeployTime", BrokerProxyService.toSimpleDateFormat(thisApp.getDeployTime()));
        createJsonProperty(outApp, "ModifyTime", BrokerProxyService.toSimpleDateFormat(thisApp.getModifyTime()));

    }
    
    
	private void createJsonProperty(MbElement outRef, String key, Object value)
    throws MbException {
        outRef.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, key, value);
    }

    private MbElement createJsonProperty_InArrayEmpty(MbElement outRef)
    throws MbException {
        return outRef.createElementAsLastChild(MbElement.TYPE_NAME, MbJSON.ARRAY_ITEM_NAME, null);
    }

    @SuppressWarnings("unused")
    private void createJsonProperty_InArray(MbElement outRef, String key, Object value)
    throws MbException {
        MbElement help = createJsonProperty_InArrayEmpty(outRef);
        createJsonProperty(help, "Name", key);
        createJsonProperty(help, "Value", value);
    }

    private MbElement createJsonArray(MbElement outRef, String name)
    throws MbException {
        return outRef.createElementAsLastChild(MbJSON.ARRAY, name, null);
    }

    private MbElement createJsonObject(MbElement outRef, String name)
    throws MbException {
        return outRef.createElementAsLastChild(MbElement.TYPE_NAME, name, null);
    }

    @SuppressWarnings("unused")
    private MbElement createJsonBody(MbElement outRef)
    throws MbException {
        return outRef.createElementAsLastChild(MbJSON.PARSER_NAME).createElementAsLastChild(MbElement.TYPE_NAME, MbJSON.DATA_ELEMENT_NAME, null);
    }


	
	

}
