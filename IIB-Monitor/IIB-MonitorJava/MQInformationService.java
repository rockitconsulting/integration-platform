

import java.util.StringTokenizer;

import com.ibm.broker.config.proxy.BrokerProxy;
import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.plugin.MbElement;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;

public class MQInformationService {

	private static MQQueueManager myManager= null;
	
	public static Long getQueueDepth(String queueName) {
		MQQueueManager manager = null;
		MQQueue queue = null;
		try {
			manager = getQueueManager();
			queue = manager.accessQueue(queueName, CMQC.MQOO_INQUIRE | CMQC.MQOO_INPUT_AS_Q_DEF, null, null, null);
			return (long) queue.getCurrentDepth();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (queue!=null && queue.isOpen()){
				try {
					queue.close();
				} catch (MQException e) {
					e.printStackTrace();
				}
			}
		}
		return -1l;
	}

	public static void getQueues(MbElement result) {
		MQQueueManager manager = null;
		try {
			manager = getQueueManager();
			PCFMessageAgent pcfAgent = new PCFMessageAgent(manager);

			PCFMessage pcfCmd = new PCFMessage(MQConstants.MQCMD_INQUIRE_Q_STATUS);
			pcfCmd.addParameter(MQConstants.MQCA_Q_NAME, "*");
			pcfCmd.addParameter(CMQCFC.MQIACF_Q_STATUS_ATTRS, new int[] { CMQC.MQCA_Q_NAME, CMQC.MQIA_CURRENT_Q_DEPTH, CMQCFC.MQCACF_LAST_GET_DATE,
					CMQCFC.MQCACF_LAST_GET_TIME, CMQCFC.MQCACF_LAST_PUT_DATE, CMQCFC.MQCACF_LAST_PUT_TIME, CMQCFC.MQIACF_OLDEST_MSG_AGE,
					CMQC.MQIA_OPEN_INPUT_COUNT, CMQC.MQIA_OPEN_OUTPUT_COUNT, CMQCFC.MQIACF_UNCOMMITTED_MSGS });
			PCFMessage[] pcfResponse = pcfAgent.send(pcfCmd);

			for (PCFMessage msg : pcfResponse) {
				MbElement element = result.createElementAsLastChild(MbElement.TYPE_NAME, "Queue", null);
				//element.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "queueManager", manager.getName());
				element.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "name", msg.getParameterValue(MQConstants.MQCA_Q_NAME));
				element.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "depth", msg.getParameterValue(MQConstants.MQIA_CURRENT_Q_DEPTH));
				element.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "openInputCount", msg.getParameterValue(MQConstants.MQIA_OPEN_INPUT_COUNT));
				element.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "openOutputCount", msg.getParameterValue(MQConstants.MQIA_OPEN_OUTPUT_COUNT));
			}

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	/**
	 * Liefert die Informationen fuer die Liste der uebergebenen Queues
	 * @param queueNames die Liste der Queuenamen, die der Benutzer sehen moechte "QUEUENAME1;QueueNAMe2;..." Semikolon getrennt
	 * @param result INOUT Parameter fuer die Nachricht, der Broker kann damit umgehen
	 * */
	public static void getQueueListDetails(String queueNames, MbElement result) {
		MQQueueManager manager = null;
		try {
			manager = getQueueManager();
			StringTokenizer tokens = new StringTokenizer(queueNames, ";");

			while(tokens.hasMoreTokens()) {
				String queueName = tokens.nextToken();
				MQQueue queue = null;
				try {
					queue = manager.accessQueue(queueName, CMQC.MQOO_INQUIRE | CMQC.MQOO_INPUT_AS_Q_DEF, null, null, null);
					MbElement element = result.createElementAsLastChild(MbElement.TYPE_NAME, "element", null);
					element.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "name", queue.getName());
					element.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "queueManager", manager.getName());
					element.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "depth", queue.getCurrentDepth());
					element.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "openInputCount", queue.getOpenInputCount());
					element.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "openOutputCount", queue.getOpenOutputCount());
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (queue!=null && queue.isOpen()){
						try {
							queue.close();
						} catch (MQException e) {
							e.printStackTrace();
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	
	public static void getQueue(String queueName, MbElement result) {
		MQQueueManager manager = null;
		MQQueue queue = null;
		try {
			manager = getQueueManager();
			queue = manager.accessQueue(queueName, CMQC.MQOO_INQUIRE | CMQC.MQOO_INPUT_AS_Q_DEF, null, null, null);
			result.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "name", queue.getName());
			result.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "depth", queue.getCurrentDepth());
			result.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "inhibitGet", queue.getInhibitGet());
			result.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "inhibitPut", queue.getInhibitPut());
			result.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "openInputCount", queue.getOpenInputCount());
			result.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "openOutputCount", queue.getOpenOutputCount());
			result.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "description", queue.getDescription());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (queue!=null && queue.isOpen()){
				try {
					queue.close();
				} catch (MQException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static MQQueueManager getQueueManager() throws ConfigManagerProxyLoggedException, MQException, ConfigManagerProxyPropertyNotInitializedException {
		if (myManager== null || !myManager.isConnected()){
			BrokerProxy broker = BrokerProxy.getLocalInstance();
			myManager = new MQQueueManager(broker.getQueueManagerName());
		}
		return myManager;
	}

}
