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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.net.MalformedURLException;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.cambria.client.CambriaClientBuilders;


public class EventPublisher {

	private static EventPublisher instance = null;
	private static CambriaBatchingPublisher pub = null;
	
	private String streamid = "";
	private static Logger log = LoggerFactory.getLogger(EventPublisher.class.getName());
	
	

	private EventPublisher(String CambriaConfigFile, String newstreamid) {
		
		this.streamid = newstreamid;
		try { 
			String basicAuthUsername = DmaapPropertyReader.getInstance(CambriaConfigFile).getKeyValue(streamid+".basicAuthUsername");
			if (basicAuthUsername != null)
			{
				//log.debug(streamid+".cambria.url"  + streamid+".cambria.topic");
				log.debug("URL:" + DmaapPropertyReader.getInstance(CambriaConfigFile).getKeyValue(streamid+".cambria.url") + "TOPIC:" + DmaapPropertyReader.getInstance(CambriaConfigFile).getKeyValue(streamid+".cambria.topic") +  "AuthUser:" + DmaapPropertyReader.getInstance(CambriaConfigFile).getKeyValue(streamid+".basicAuthUsername") +  "Authpwd:" + DmaapPropertyReader.getInstance(CambriaConfigFile).getKeyValue(streamid+".basicAuthPassword"));
		
				pub = new CambriaClientBuilders.PublisherBuilder ()
				 .usingHosts (DmaapPropertyReader.getInstance(CambriaConfigFile).dmaap_hash.get(streamid+".cambria.url"))
				 .onTopic (DmaapPropertyReader.getInstance(CambriaConfigFile).dmaap_hash.get(streamid+".cambria.topic"))
				 .usingHttps()
				 .authenticatedByHttp ( DmaapPropertyReader.getInstance(CambriaConfigFile).dmaap_hash.get(streamid+".basicAuthUsername"), DmaapPropertyReader.getInstance(CambriaConfigFile).dmaap_hash.get(streamid+".basicAuthPassword") )
				 .build ();
			} 
			else
			{
				//log.debug(streamid+".cambria.url"  + streamid+".cambria.topic");
				log.debug("URL:" + DmaapPropertyReader.getInstance(CambriaConfigFile).getKeyValue(streamid+".cambria.url") + "TOPIC:" + DmaapPropertyReader.getInstance(CambriaConfigFile).getKeyValue(streamid+".cambria.topic"));
				
				
				pub = new CambriaClientBuilders.PublisherBuilder ()
						 .usingHosts (DmaapPropertyReader.getInstance(CambriaConfigFile).dmaap_hash.get(streamid+".cambria.hosts"))
						 .onTopic (DmaapPropertyReader.getInstance(CambriaConfigFile).dmaap_hash.get(streamid+".cambria.topic"))
						 .build ();
							
			}
		}
		catch(GeneralSecurityException | MalformedURLException e ) {
			log.error("CambriaClientBuilders connection exception : " + e.getMessage());
		} 
		catch(Exception e) {
			log.error("CambriaClientBuilders connection exception : " + e.getMessage());
		}
		         
	}
	
	public static synchronized EventPublisher getInstance( String CambriaConfigFile, String streamid){
	       if (instance == null) {
	           instance = new EventPublisher(CambriaConfigFile, streamid);
	       }
	       return instance;
	      
		}
	
	public synchronized void sendEvent(String event, String newstreamid ) {
	
		//Check if streamid changed
		if(! newstreamid.equals(this.streamid)) { 
			closePublisher();
			instance = new EventPublisher (CommonStartup.cambriaConfigFile,  newstreamid);
		}


		try {
			int pendingMsgs = pub.send("MyPartitionKey", event.toString());
			
			if(pendingMsgs > 100) {
				log.info("Pending Message Count="+pendingMsgs);
			}
		
			CommonStartup.oplog.info ("Event Published:" + event);
		} catch(IOException ioe) {
			log.error("Unable to publish event:" + event + " Exception:" + ioe.toString()); 
		}

		
		
		
	}


    public synchronized void closePublisher() {
		
		try { 
			final List<?> stuck = pub.close(20, TimeUnit.SECONDS);
			if ( stuck.size () > 0 ) { 
				log.error(stuck.size() + " messages unsent" ); 
			}
		}
		catch(InterruptedException ie) {
			log.error("Caught an Interrupted Exception on Close event");
		}catch(IOException ioe) {
			log.error("Caught IO Exception: " + ioe.toString()); 
		}
		
	}
}
