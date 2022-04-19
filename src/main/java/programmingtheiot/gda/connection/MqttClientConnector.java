/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.connection;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;

import programmingtheiot.common.SimpleCertManagementUtil;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class MqttClientConnector implements IPubSubClient, MqttCallbackExtended
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientConnector.class.getName());
	
	// params
	private MqttClient           mqttClient = null;
	private MqttConnectOptions   connOpts = null;
	private MemoryPersistence    persistence = null;
	private IDataMessageListener dataMsgListener = null;

	private String  clientID = null;
	private String  brokerAddr = null;
	//private String  host = ConfigConst.DEFAULT_HOST;
	private String  host = ConfigConst.DEFAULT_HOST;
	private String  protocol = ConfigConst.DEFAULT_MQTT_PROTOCOL;
	//private String  protocol = ConfigConst.DEFAULT_MQTT_SECURE_PROTOCOL;
	private int     port = ConfigConst.DEFAULT_MQTT_PORT;
	//private int     port = ConfigConst.DEFAULT_MQTT_SECURE_PORT;
	private int     brokerKeepAlive = ConfigConst.DEFAULT_KEEP_ALIVE;
	private String pemFileName = null;
	private boolean enableEncryption = false;
	private boolean useCleanSession = false;
	private boolean enableAutoReconnect = true;
	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public MqttClientConnector(String dup)
	{
		//super();
		ConfigUtil configUtil = ConfigUtil.getInstance();

		this.host =
		    configUtil.getProperty(
		        ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);

		this.port =
		    configUtil.getInteger(
		        ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);
		
		        /*this.port =
			    configUtil.getInteger(
			        ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.SECURE_PORT_KEY, ConfigConst.DEFAULT_MQTT_SECURE_PORT);*/

		this.brokerKeepAlive =
		    configUtil.getInteger(
		        ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);

		this.clientID = MqttClient.generateClientId();
		this.persistence = new MemoryPersistence();
		this.connOpts = new MqttConnectOptions();

		this.connOpts.setKeepAliveInterval(this.brokerKeepAlive);
		this.connOpts.setCleanSession(false);
		this.connOpts.setAutomaticReconnect(true);
		this.brokerAddr = this.protocol + "://" + this.host + ":" + this.port;
	}
	
	/**
	 *
	 *Duplicate constructor
	 *
	 */
	public MqttClientConnector()
	{
		this("Duplicate");
		
		initClientParameters(ConfigConst.MQTT_GATEWAY_SERVICE);
	}
	
	// public methods
	
	@Override
	public boolean connectClient()
	{
		try {
			if (this.mqttClient == null) {
				this.mqttClient = new MqttClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient.setCallback(this);
			}
			
			if (! this.mqttClient.isConnected()) {
				_Logger.info("MQTT client connecting to broker: " + this.brokerAddr);
				this.mqttClient.connect(this.connOpts);
				return true;
			} else {
				_Logger.warning("MQTT client already connected to broker: " + this.brokerAddr);
			}
		} catch (MqttException e) {
			_Logger.log(Level.SEVERE, "Failed to connect MQTT client to broker.", e);
		}
		
		return false;
	}
	
	/**
	 * Disconnect the MQTT client
	 */
	@Override
	public boolean disconnectClient()
	{
		try {
			if (this.mqttClient != null) {
				if (this.mqttClient.isConnected()) {
					_Logger.info("Disconnecting MQTT client from broker: " + this.brokerAddr);
					this.mqttClient.disconnect();
					return true;
				} else {
					_Logger.warning("MQTT client not connected to broker: " + this.brokerAddr);
				}
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to disconnect MQTT client from broker: " + this.brokerAddr, e);
		}
		
		return false;
	}

	public boolean isConnected()
	{
		return false;
	}
	
	/**
	 * Publish 
	 * @param topicName - MQTT topic name
	 * @param msg - message to publish
	 * @param qos - MQTT QoS
	 * @return true if success in publish
	 */
	@Override
	public boolean publishMessage(ResourceNameEnum topicName, String msg, int qos)
	{
		// TODO: determine how verbose your logging should be, especially if this method is called often
		if (topicName == null) {
			//_Logger.warning("Resource is null. Unable to publish message: " + this.brokerAddr);
			return false;
		}
		
		if (msg == null || msg.length() == 0) {
			//_Logger.warning("Message is null or empty. Unable to publish message: " + this.brokerAddr);
			return false;
		}
		
		if (qos < 0 || qos > 2) {
			qos = ConfigConst.DEFAULT_QOS;
		}
		
		try {
			byte[] payload = msg.getBytes();
			MqttMessage mqttMsg = new MqttMessage(payload);
			mqttMsg.setQos(qos);
			this.mqttClient.publish(topicName.getResourceName(), mqttMsg);
			return true;
		} catch (Exception e) {
			//_Logger.log(Level.SEVERE, "Failed to publish message to topic: " + topicName, e);
		}
		
		return false;
	}

	/**
	 * Subscribe to topic 
	 * @param topicName - MQTT topic name
	 * @param qos - MQTT QoS
	 * @return true if success
	 */
	@Override
	public boolean subscribeToTopic(ResourceNameEnum topicName, int qos)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to subscribe to topic: " + this.brokerAddr);
			return false;
		}
		
		if (qos < 0 || qos > 2) {
			qos = ConfigConst.DEFAULT_QOS;
		}
		
		try {
			this.mqttClient.subscribe(topicName.getResourceName(), qos);
			_Logger.info("Successfully subscribed to topic: " + topicName.getResourceName());
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to subscribe to topic: " + topicName, e);
		}
		
		return false;
	}
	
	/**
	 * Unsubscribe 
	 * @param topicName - MQTT topic name
	 * @param qos - MQTT QoS
	 * @return true if success
	 */
	@Override
	public boolean unsubscribeFromTopic(ResourceNameEnum topicName)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to unsubscribe from topic: " + this.brokerAddr);
			return false;
		}
		
		try {
			this.mqttClient.unsubscribe(topicName.getResourceName());
			_Logger.info("Successfully unsubscribed from topic: " + topicName.getResourceName());
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to unsubscribe from topic: " + topicName, e);
		}
		
		return false;
	}

	/**
	 * Set the data message listener
	 */
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
			return true;
		}
		
		return false;
	}
	// callbacks
	
	/**
	 * Callback after connection is established 
	 * @param topicName - MQTT topic name
	 * @param qos - MQTT QoS
	 * @return true if success
	 */
	@Override
	public void connectComplete(boolean reconnect, String serverURI)
	{
		_Logger.info("Connection completed ");
		_Logger.info("MQTT connection successful (is reconnect = " + reconnect + "). Broker: " + serverURI);
		
		int qos = 1;
		
		// Option 2
		try {
			_Logger.info("Subscribing to topic: " + ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE.getResourceName());
				
			this.mqttClient.subscribe(
				ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE.getResourceName(),
				qos,
				new ActuatorResponseMessageListener(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, this.dataMsgListener));
		} catch (MqttException e) {
			_Logger.warning("Failed to subscribe to CDA actuator response topic.");
		}
		
		try {
			_Logger.info("Subscribing to topic: " + ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceName());
				
			this.mqttClient.subscribe(
				ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE.getResourceName(),
				qos,
				new SensorResponseMessageListener(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, this.dataMsgListener));
		} catch (MqttException e) {
			_Logger.warning("Failed to subscribe to CDA sensor topic.");
		}
		
		try {
			_Logger.info("Subscribing to topic: " + ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE.getResourceName());
				
			this.mqttClient.subscribe(
				ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE.getResourceName(),
				qos,
				new SystemPerformanceMessageListener(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, this.dataMsgListener));
		} catch (MqttException e) {
			_Logger.warning("Failed to subscribe to CDA system performance topic.");
		}
	}

	@Override
	public void connectionLost(Throwable t)
	{
		_Logger.info("Connection lost ");
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token)
	{
		//_Logger.info("Delivery completed");
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception
	{
		_Logger.info("Message arrived ");
	}

	
	// private methods
	
	/**
	 * Called by the constructor to set the MQTT client parameters to be used for the connection.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initClientParameters(String configSectionName)
	{
		// TODO: implement this
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.host =
			configUtil.getProperty(
				configSectionName, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);
		this.port =
			configUtil.getInteger(
				configSectionName, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);
		this.brokerKeepAlive =
			configUtil.getInteger(
				configSectionName, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);
		this.enableEncryption =
			configUtil.getBoolean(
				configSectionName, ConfigConst.ENABLE_CRYPT_KEY);
		this.pemFileName =
			configUtil.getProperty(
				configSectionName, ConfigConst.CERT_FILE_KEY);
		
		// Paho Java client requires a client ID - for now, it should be 'GatewayDevice'
		this.clientID =
			configUtil.getProperty(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.DEVICE_LOCATION_ID_KEY, MqttClient.generateClientId());
		
		// these are specific to the MQTT connection which will be used during connect
		this.persistence = new MemoryPersistence();
		this.connOpts    = new MqttConnectOptions();
		
		this.connOpts.setKeepAliveInterval(this.brokerKeepAlive);
		this.connOpts.setCleanSession(this.useCleanSession);
		this.connOpts.setAutomaticReconnect(this.enableAutoReconnect);
		
		// if encryption is enabled, try to load and apply the cert(s)
		if (this.enableEncryption) {
			initSecureConnectionParameters(configSectionName);
		}
		
		// if there's a credential file, try to load and apply them
		if (configUtil.hasProperty(configSectionName, ConfigConst.CRED_FILE_KEY)) {
			initCredentialConnectionParameters(configSectionName);
		}
		
		// NOTE: URL does not have a protocol handler for "tcp" or "ssl",
		// so construct the URL manually
		this.brokerAddr  = this.protocol + "://" + this.host + ":" + this.port;
		
		_Logger.info("Using URL for broker conn: " + this.brokerAddr);

	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to load credentials.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initCredentialConnectionParameters(String configSectionName)
	{
		// TODO: implement this
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		try {
			_Logger.info("Checking if credentials file exists and us loadable...");
			
			Properties props = configUtil.getCredentials(configSectionName);
			
			if (props != null) {
				this.connOpts.setUserName(props.getProperty(ConfigConst.USER_NAME_TOKEN_KEY, ""));
				this.connOpts.setPassword(props.getProperty(ConfigConst.USER_AUTH_TOKEN_KEY, "").toCharArray());
				
				_Logger.info("Credentials now set.");
			} else {
				_Logger.warning("No credentials are set.");
			}
		} catch (Exception e) {
			_Logger.log(Level.WARNING, "Credential file non-existent. Disabling auth requirement.");
		}
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to enable encryption.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initSecureConnectionParameters(String configSectionName)
	{
		// TODO: implement this
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		try {
			_Logger.info("Configuring TLS...");
			
			if (this.pemFileName != null) {
				File file = new File(this.pemFileName);
				
				if (file.exists()) {
					_Logger.info("PEM file valid. Using secure connection: " + this.pemFileName);
				} else {
					this.enableEncryption = false;
					
					_Logger.log(Level.WARNING, "PEM file invalid. Using insecure connection: " + this.pemFileName, new Exception());
					
					return;
				}
			}
			
			SSLSocketFactory sslFactory =
				SimpleCertManagementUtil.getInstance().loadCertificate(this.pemFileName);
			
			this.connOpts.setSocketFactory(sslFactory);
			
			// override current config parameters
			this.port =
				configUtil.getInteger(
					configSectionName, ConfigConst.SECURE_PORT_KEY, ConfigConst.DEFAULT_MQTT_SECURE_PORT);
			
			this.protocol = ConfigConst.DEFAULT_MQTT_SECURE_PROTOCOL;
			
			_Logger.info("TLS enabled.");
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to initialize secure MQTT connection. Using insecure connection.", e);
			
			this.enableEncryption = false;
		}
	}
	
	
	private class ActuatorResponseMessageListener implements IMqttMessageListener
	{
		private ResourceNameEnum resource = null;
		private IDataMessageListener dataMsgListener = null;
		
		ActuatorResponseMessageListener(ResourceNameEnum resource, IDataMessageListener dataMsgListener)
		{
			this.resource = resource;
			this.dataMsgListener = dataMsgListener;
		}
		
		/**
		 * Callback after Actuator Response Message received
		 * @param topic - MQTT topic name
		 * @param message - mqtt message received
		 */
		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception
		{
			try {
				ActuatorData actuatorData =
					DataUtil.getInstance().jsonToActuatorData(new String(message.getPayload()));
				
				// optionally, log a message indicating data was received
				_Logger.info("Received Actuator response: " + actuatorData.getValue());
					

				if (this.dataMsgListener != null) {
					this.dataMsgListener.handleActuatorCommandResponse(resource, actuatorData);
				}
			} catch (Exception e) {
				_Logger.warning("Failed to convert message payload to ActuatorData.");
			}
		}
		
	}
	
	
	private class SensorResponseMessageListener implements IMqttMessageListener
	{
		private ResourceNameEnum resource = null;
		private IDataMessageListener dataMsgListener = null;
		
		SensorResponseMessageListener(ResourceNameEnum resource, IDataMessageListener dataMsgListener)
		{
			this.resource = resource;
			this.dataMsgListener = dataMsgListener;
		}
		
		/**
		 * Callback after Sensor Response Message received
		 * @param topic - MQTT topic name
		 * @param message - mqtt message received
		 */
		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception
		{
			try {
				SensorData sensorData =
					DataUtil.getInstance().jsonToSensorData(new String(message.getPayload()));
				
				// optionally, log a message indicating data was received
				_Logger.info("Received Sensor response: " + sensorData.getValue());
					

				if (this.dataMsgListener != null) {
					this.dataMsgListener.handleSensorMessage(resource, sensorData);
				}
			} catch (Exception e) {
				_Logger.warning("Failed to convert message payload to SensorData.");
			}
		}
		
	}
	
	private class SystemPerformanceMessageListener implements IMqttMessageListener
	{
		private ResourceNameEnum resource = null;
		private IDataMessageListener dataMsgListener = null;
		
		SystemPerformanceMessageListener(ResourceNameEnum resource, IDataMessageListener dataMsgListener)
		{
			this.resource = resource;
			this.dataMsgListener = dataMsgListener;
		}
		
		/**
		 * Callback after System Performance Response Message received
		 * @param topic - MQTT topic name
		 * @param message - mqtt message received
		 */
		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception
		{
			try {
				SystemPerformanceData sysPerfData =
					DataUtil.getInstance().jsonToSystemPerformanceData(new String(message.getPayload()));
				
				// optionally, log a message indicating data was received
				_Logger.info("Received Cpu util response: " + sysPerfData.getCpuUtilization() + " Memory utilization: " + sysPerfData.getMemoryUtilization());
					

				if (this.dataMsgListener != null) {
					this.dataMsgListener.handleSystemPerformanceMessage(resource, sysPerfData);
				}
			} catch (Exception e) {
				_Logger.warning("Failed to convert message payload to SystemPerformanceManager.");
			}
		}
		
	}
}
