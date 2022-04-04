/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.connection.handlers;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;


/**
 * Shell representation of class for student implementation.
 *
 */
public class UpdateTelemetryResourceHandler extends CoapResource
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(UpdateTelemetryResourceHandler.class.getName());
	
	// params
	private IDataMessageListener dataMsgListener = null;
	private ResourceNameEnum resource;
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param resource Basically, the path (or topic)
	 */
	public UpdateTelemetryResourceHandler(ResourceNameEnum resource, IDataMessageListener dataMsgListener)
	{
		this(resource.getResourceName());
		this.resource=resource;
		this.dataMsgListener=dataMsgListener;
	}
	
	/**
	 * Constructor.
	 * 
	 * @param resourceName The name of the resource.
	 */
	public UpdateTelemetryResourceHandler(String resourceName)
	{
		super(resourceName);
	}
	
	
	// public methods
	
	@Override
	public void handleDELETE(CoapExchange context)
	{
		ResponseCode code = ResponseCode.NOT_ACCEPTABLE;
		  
		  context.accept();
		  
		  if (this.dataMsgListener != null) {
		    try {
		      /*String jsonData = new String(context.getRequestPayload());
		      
		      SensorData sensordata =
		        DataUtil.getInstance()
		          .jsonToSensorData(jsonData);
		      
		      
		      this.dataMsgListener.handleSensorMessage(this.resource, sensordata);
		      */
		    //Nothing is deleted but sending the response for wireshark
		      code = ResponseCode.DELETED;
		    } catch (Exception e) {
		      _Logger.warning(
		        "Failed to handle DELETE request. Message: " +
		        e.getMessage());
		      
		      code = ResponseCode.BAD_REQUEST;
		    }
		  } else {
		    _Logger.info(
		      "No callback listener for request. Ignoring DELETE.");
		    
		    code = ResponseCode.CONTINUE;
		  }
		  
		  String msg =
		    "Deleted: " + super.getName();
		  
		  context.respond(code, msg);
	}
	
	@Override
	public void handleGET(CoapExchange context)
	{
	}
	
	@Override
	public void handlePOST(CoapExchange context)
	{
		ResponseCode code = ResponseCode.NOT_ACCEPTABLE;
		  
		  context.accept();
		  
		  if (this.dataMsgListener != null) {
		    try {
		      String jsonData = new String(context.getRequestPayload());
		      
		      SensorData sensordata =
		        DataUtil.getInstance()
		          .jsonToSensorData(jsonData);
		      
		      
		      this.dataMsgListener.handleSensorMessage(this.resource, sensordata);
		      
		      code = ResponseCode.CREATED;
		    } catch (Exception e) {
		      _Logger.warning(
		        "Failed to handle POST request. Message: " +
		        e.getMessage());
		      
		      code = ResponseCode.BAD_REQUEST;
		    }
		  } else {
		    _Logger.info(
		      "No callback listener for request. Ignoring POST.");
		    
		    code = ResponseCode.CONTINUE;
		  }
		  
		  String msg =
		    "POST request handled: " + super.getName();
		  
		  context.respond(code, msg);
	}
	
	@Override
	public void handlePUT(CoapExchange context)
	{
		ResponseCode code = ResponseCode.NOT_ACCEPTABLE;
		  
		  context.accept();
		  
		  if (this.dataMsgListener != null) {
		    try {
		      String jsonData = new String(context.getRequestPayload());
		      
		      SensorData sensordata =
		        DataUtil.getInstance()
		          .jsonToSensorData(jsonData);
		      
		      
		      this.dataMsgListener.handleSensorMessage(this.resource, sensordata);
		      
		      code = ResponseCode.CHANGED;
		    } catch (Exception e) {
		      _Logger.warning(
		        "Failed to handle PUT request. Message: " +
		        e.getMessage());
		      
		      code = ResponseCode.BAD_REQUEST;
		    }
		  } else {
		    _Logger.info(
		      "No callback listener for request. Ignoring PUT.");
		    
		    code = ResponseCode.CONTINUE;
		  }
		  
		  String msg =
		    "PUT data request handled: " + super.getName();
		  
		  context.respond(code, msg);
	}
	
	public void setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
		}
	}
	
}
