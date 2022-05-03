/* This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */
package programmingtheiot.gda.connection;

import java.io.File;
import java.io.IOException;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;

import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;


public class SmtpClientConnector implements ISimpleMessagingClient
{
	// static
	private static final Logger _Logger = 
			Logger.getLogger(SmtpClientConnector.class.getName());
	
	public static final int DELAY_BETWEEN_CONN_RETRIES  =1000;
	public static final int MAX_CONN_RETRIES_BEFORE_COOLDOWN = 6;
	public static final int CONN_RETRY_COOLDOWN_TIMEOUT =
			DELAY_BETWEEN_CONN_RETRIES * MAX_CONN_RETRIES_BEFORE_COOLDOWN;
	
	
	// private var's
	private boolean isInitialized = false;
	private Session smtpSession = null;
	
	private String fromAddr = null;
	private String toAddr = null;
	
	private IDataMessageListener dataMsgListener = null;
	
	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public SmtpClientConnector()
	{
		super();
	}
	
	
	// public methods

	@Override
	public boolean connectClient() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean disconnectClient() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean sendMessage(ResourceNameEnum resource, String payload, int timeout)
	{
		if(resource != null && payload !=null){
			return configureAndSendMessage(resource, payload);
		}
		else {
			_Logger.warning("Input parameters are null or invalid, Ignoring. ");
			return false;
		}
	}

	@Override
	public boolean sendMessage(ResourceNameEnum resource, ActuatorData data) {
		if(resource != null && data !=null){
			String payload = DataUtil.getInstance().actuatorDataToJson(data);
			return configureAndSendMessage(resource, payload);
		}
		else {
			_Logger.warning("Input parameters are null or invalid, Ignoring. ");
			return false;
		}
	}


	@Override
	public boolean sendMessage(ResourceNameEnum resource, SensorData data) {
		if(resource != null && data !=null){
			String payload = DataUtil.getInstance().sensorDataToJson(data);
			return configureAndSendMessage(resource, payload);
		}
		else {
			_Logger.warning("Input parameters are null or invalid, Ignoring. ");
			return false;
		}
	}


	@Override
	public boolean sendMessage(ResourceNameEnum resource, SystemPerformanceData data) {
		if(resource != null && data !=null){
			String payload = DataUtil.getInstance().systemPerformanceDataToJson(data);
			return configureAndSendMessage(resource, payload);
		}
		else {
			_Logger.warning("Input parameters are null or invalid, Ignoring. ");
			return false;
		}
	}
	

	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if(listener != null) {
			this.dataMsgListener = listener;
			return true;
		}
		
		return false;
	}


	
	// private methods
	private boolean configureAndSendMessage(ResourceNameEnum resource, String msg)
	{
		return configureAndSendMessage(resource,msg,"text/plain");
	}
	
	private boolean configureAndSendMessage(ResourceNameEnum resource, String msg, String mimeType)
	{
		Message smtpMsg = createSmtpMessage(resource, resource.getResourceName(), msg);
		byte[] rawData = msg.getBytes();
		
		if(rawData != null)
		{
			if(mimeType == null)
			{
				mimeType = "text/plain";
			}
			
			try {
				MimeMultipart multiPart = (MimeMultipart) smtpMsg.getContent();
				MimeBodyPart bodyPart = new MimeBodyPart();
				
				try {
					ByteArrayDataSource baData = new ByteArrayDataSource(rawData, mimeType);
					DataHandler handler = new DataHandler(baData);
					bodyPart.setDataHandler(handler);
					bodyPart.setHeader("Content-ID", resource.getDeviceName());
					
					multiPart.addBodyPart(bodyPart);
					
				}
				catch(MessagingException e) {
					_Logger.log(Level.WARNING,"Failed to update multi-part body for smtp msg.",e);
				}
				
				smtpMsg.setContent(multiPart);
			}
			catch(MessagingException e) {
				_Logger.log(Level.WARNING,"Failed to embed MIME multipart content within SMTP Msg.",e);
			}
			catch(Exception e){
				_Logger.log(Level.WARNING,"No MIME Multipart data to update",e);
			}
		}
		else
		{
			_Logger.info("Preparing standard SMTP message(no embedded Media). ");
		}
		
		return sendSmtpMessage(smtpMsg);
	}
	
	private Message createSmtpMessage(ResourceNameEnum resource, String attachmentPath, String content)
	{
		String timeStamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
		
		return createSmtpMessage(resource, attachmentPath, content, timeStamp);
	}
	
	private Message createSmtpMessage(ResourceNameEnum resource, String attachmentPath, String content, String timeStamp) {
		if(!this.isInitialized)
		{
			initSmtpSession();
		}
		
		try {
			Message smtpMsg = new MimeMessage(this.smtpSession);
			InternetAddress fromAddr = new InternetAddress(this.fromAddr);
			InternetAddress toAddr = new InternetAddress(this.toAddr);
			//InternetAddress[] toAddr = InternetAddress.parse(this.toAddr);
			
			smtpMsg.setFrom(fromAddr);
			//smtpMsg.setRecipients(Message.RecipientType.TO, toAddr);
			smtpMsg.setRecipient(Message.RecipientType.TO, toAddr);
			
			String subject = createMessageSubject(resource);
			String body = createMessageBody(resource, attachmentPath, content, timeStamp);
			
			smtpMsg.setSubject(subject);
			
			try {
				MimeMultipart multiPart = new MimeMultipart();
				MimeBodyPart bodyPart = new MimeBodyPart();
				
				bodyPart.setText(body);
				multiPart.addBodyPart(bodyPart);
				smtpMsg.setContent(multiPart);
			}
			catch(MessagingException e)
			{
				_Logger.log(Level.WARNING,"Failed to update MIME Multipart body for SMTP msg.", e);
			}
			
			_Logger.info("Sending msg with embedded content to address: "+ this.toAddr);
			return smtpMsg;
		}
		catch(Exception e)
		{
			_Logger.log(Level.WARNING,"Failed to send SMTP Message from Address: "+ this.fromAddr, e);
		}
		
		return null;
	}
	
	private void initSmtpSession()
	{
		if(!this.isInitialized)
		{
			_Logger.info("Initializing SMTP Gateway..");
			
			int smtpPort = ConfigUtil.getInstance().getInteger(ConfigConst.SMTP_GATEWAY_SERVICE, ConfigConst.PORT_KEY);
			
			boolean enableTls = ConfigUtil.getInstance().getBoolean(ConfigConst.SMTP_GATEWAY_SERVICE, ConfigConst.ENABLE_CRYPT_KEY);
			
			boolean enableAuth = ConfigUtil.getInstance().getBoolean(ConfigConst.SMTP_GATEWAY_SERVICE, ConfigConst.ENABLE_AUTH_KEY);
			
			if(enableTls)
			{
				smtpPort = ConfigUtil.getInstance().getInteger(ConfigConst.SMTP_GATEWAY_SERVICE, ConfigConst.SECURE_PORT_KEY);
			}
			
			Properties smtpProps = ConfigUtil.getInstance().getCredentials(ConfigConst.SMTP_GATEWAY_SERVICE);
			
			this.fromAddr = smtpProps.getProperty(ConfigConst.FROM_ADDRESS_KEY);
			this.toAddr = smtpProps.getProperty(ConfigConst.TO_ADDRESS_KEY);
			
			Properties props  = new Properties();
			
			props.put(ConfigConst.SMTP_PROP_HOST_KEY,ConfigUtil.getInstance().getProperty(ConfigConst.SMTP_GATEWAY_SERVICE, ConfigConst.HOST_KEY));
			props.put(ConfigConst.SMTP_PROP_AUTH_KEY, enableAuth);
			props.put(ConfigConst.SMTP_PROP_PORT_KEY, smtpPort);
			props.put(ConfigConst.SMTP_PROP_ENABLE_TLS_KEY, enableTls);
			
			_Logger.info(props.toString());
			
			Authenticator authenticator = new SmtpAuthenticator();
			
			this.smtpSession = Session.getInstance(props, authenticator);
			this.isInitialized = true;
		}
		else {
			_Logger.info("SMTP Gateway Connection Already initialized");
		}
	}
	
	private boolean sendSmtpMessage(Message smtpMsg)
	{
		try {
			_Logger.info("Sending SMTP message to: "+ smtpMsg.getAllRecipients());
			Transport.send(smtpMsg);
			
			return true;
		} catch(SendFailedException se) {
			_Logger.log(Level.SEVERE,"Failed to send SMTP message", se);
			
		} catch(MessagingException me) {
			_Logger.log(Level.SEVERE,"Failed to send SMTP message", me);
		}
		return false;
	}
	
	private String createMessageSubject(ResourceNameEnum resource)
	{
		String locationID = ConfigUtil.getInstance().getProperty(ConfigConst.GATEWAY_DEVICE, ConfigConst.DEVICE_LOCATION_ID_KEY);
		
		StringBuilder buf = new StringBuilder();
		
		buf.append("Gateway Message from ");
		buf.append(locationID).append(" - ");
		buf.append(resource.getResourceType());
		
		return buf.toString();
	}
	
	private String createMessageBody(ResourceNameEnum resource, String dataPath, String content, String timestamp)
	{
		String locationID = ConfigUtil.getInstance().getProperty(ConfigConst.GATEWAY_DEVICE, ConfigConst.DEVICE_LOCATION_ID_KEY);
		
		StringBuilder buf = new StringBuilder();
		
		buf.append("Gateway Message content:\n");
		buf.append("\n\tDevice:    ").append(locationID);
		buf.append("\n\tResource:  ").append(resource.getResourceName());
		buf.append("\n\tTimeStamp: ").append(timestamp);
		buf.append("\n\tData Path: ");
		
		if(dataPath != null && dataPath.length()>0) {
			buf.append(dataPath);
		}else {
			buf.append("None");
		}
		
		buf.append("\n\tMessage:   ");
		
		if(content != null && content.length() >=0){
			buf.append("follows. \n\n").append(content);
		}else {
			buf.append("None");
		}
		
		return buf.toString();
		
	}
	
	
	
	
}
