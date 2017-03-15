/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.dcae.commonFunction;

import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

public class EventProcessor implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);

	private static HashMap<String, String[]> streamid_hash = new HashMap<String, String[]>();
	private JSONObject event = null;

	public EventProcessor() {
		log.debug("EventProcessor: Default Constructor");
		
		String list[] = CommonStartup.streamid.split("\\|");
		for (int i = 0; i < list.length; i++) {
			String domain = list[i].split("=")[0];
			//String streamIdList[] = list[i].split("=")[1].split(",");
			String streamIdList[] = list[i].substring(list[i].indexOf("=") +1).split(",");
			
			log.debug("Domain: " + domain + " streamIdList:" + Arrays.toString(streamIdList));
			streamid_hash.put(domain, streamIdList);
		}
		
	}

	@Override
	public void run() {

		try {
			
			event = CommonStartup.fProcessingInputQueue.take();
			log.info("EventProcessor\tRemoving element: " + event);
			
			//EventPublisher Ep=new EventPublisher();
			while (event != null) {
				// As long as the producer is running we remove elements from the queue.

				//UUID uuid = UUID.fromString(event.get("VESuniqueId").toString());
				String uuid = event.get("VESuniqueId").toString();
				LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid.toString());
				localLC .put ( EcompFields.kBeginTimestampMs, SaClock.now () );
				
				log.debug("event.VESuniqueId" + event.get("VESuniqueId") + "event.commonEventHeader.domain:" + event.getJSONObject("event").getJSONObject("commonEventHeader").getString("domain"));
				String streamIdList[]=streamid_hash.get(event.getJSONObject("event").getJSONObject("commonEventHeader").getString("domain"));
				log.debug("streamIdList:" + streamIdList);
				
				if (streamIdList.length == 0)		{
					log.error("No StreamID defined for publish - Message dropped" + event.toString());
				} 
				
				else {
					for (int i=0; i < streamIdList.length; i++)
					{
						log.info("Invoking publisher for streamId:" + streamIdList[i]);
						this.overrideEvent();
						EventPublisher.getInstance(streamIdList[i]).sendEvent(event);
						
					}
				}
				log.debug("Message published" + event.toString());
				event = CommonStartup.fProcessingInputQueue.take();
				// log.info("EventProcessor\tRemoving element: " + this.queue.remove());
			}
		} catch (InterruptedException e) {
			log.error("EventProcessor InterruptedException" + e.getMessage());
		}

	}

	
	public void overrideEvent()
	{
		//Set collector timestamp in event payload before publish
		final Date currentTime = new Date();
		final SimpleDateFormat sdf =   new SimpleDateFormat("EEE, MM dd yyyy hh:mm:ss z"); 
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		JSONArray additionalParametersarray = new JSONArray().put(new JSONObject().put("collectorTimeStamp", sdf.format(currentTime)));
		JSONObject additionalParameter = new JSONObject().put("additionalParameters",additionalParametersarray );
		JSONObject commonEventHeaderkey = event.getJSONObject("event").getJSONObject("commonEventHeader");
		commonEventHeaderkey.put("internalHeaderFields", additionalParameter);
		event.getJSONObject("event").put("commonEventHeader",commonEventHeaderkey);		
		log.debug("Modified event:" + event);
		
	}
}
