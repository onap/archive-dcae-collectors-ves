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


import java.io.FileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;



public class DmaapPropertyReader {

	private static DmaapPropertyReader instance = null;

	   
	   private static final Logger log = LoggerFactory.getLogger ( DmaapPropertyReader.class );
	   //static private final VESLogger log = VESLogger.getLogger(DmaapPropertyReader.class, VESLogger.VES_AGENT);
	   
       public  HashMap<String, String> dmaap_hash = new HashMap<String, String>();
	   
	   private DmaapPropertyReader(String CambriaConfigFile) {
		   
		   FileReader fr = null;
		    try {
		    	JsonElement root = null;
		    	fr = new FileReader(CambriaConfigFile);
				root = new JsonParser().parse(fr);
				JsonArray jsonObject = (JsonArray) root.getAsJsonObject().get("channels");
				
				  for (int i = 0; i < jsonObject.size(); i++) {
					  log.debug("TOPIC:" + jsonObject.get(i).getAsJsonObject().get("cambria.topic") +
							    " HOST-URL:" + jsonObject.get(i).getAsJsonObject().get("cambria.url") + 
							    " HOSTS:" + jsonObject.get(i).getAsJsonObject().get("cambria.hosts") + 
							    " PWD:" + jsonObject.get(i).getAsJsonObject().get("basicAuthPassword") + 
							    " USER:" + jsonObject.get(i).getAsJsonObject().get("basicAuthUsername") + 
							    " NAME:" + jsonObject.get(i).getAsJsonObject().get("name") );
					  
					  String convertedname = jsonObject.get(i).getAsJsonObject().get("name").toString().replace("\"","");
					  dmaap_hash.put(convertedname + ".cambria.topic", jsonObject.get(i).getAsJsonObject().get("cambria.topic").toString().replace("\"","") );
					  
					  if (jsonObject.get(i).getAsJsonObject().get("cambria.hosts") != null)
					  {
						  dmaap_hash.put(convertedname + ".cambria.hosts", jsonObject.get(i).getAsJsonObject().get("cambria.hosts").toString().replace("\"","") );
					  }
					  if (jsonObject.get(i).getAsJsonObject().get("cambria.url") != null)
					  {
						  dmaap_hash.put(convertedname + ".cambria.url", jsonObject.get(i).getAsJsonObject().get("cambria.url").toString().replace("\"","") );
					  }
					  if (jsonObject.get(i).getAsJsonObject().get("basicAuthPassword") != null)
					  {
						  dmaap_hash.put(convertedname + ".basicAuthPassword", jsonObject.get(i).getAsJsonObject().get("basicAuthPassword").toString().replace("\"","") );
					  }	  
					  if (jsonObject.get(i).getAsJsonObject().get("basicAuthUsername") != null)
					  {
						  dmaap_hash.put(convertedname+ ".basicAuthUsername", jsonObject.get(i).getAsJsonObject().get("basicAuthUsername").toString().replace("\"","") );
					  }

				}
			} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e1) {
				e1.printStackTrace();
				log.error("Problem loading Dmaap Channel configuration file: " +e1.toString());
			}
		    finally {
		    	if (fr != null) {
		    		try {
		    				fr.close();
		    			} catch (IOException e) {
		    				log.error("Error closing file reader stream : " +e.toString());
		    			}
		    	}
		    }
		   	
			
	   }
	   
	 

	   public static synchronized DmaapPropertyReader getInstance(String ChannelConfig){
	       if (instance == null) {
	           instance = new DmaapPropertyReader(ChannelConfig);
	       }
	       return instance;
	   }
	   
	   
	   public String getKeyValue(String HashKey){
	       return this.dmaap_hash.get(HashKey);
	   }
}
