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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventProcessor implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);
	private JSONObject event = null;

	public EventProcessor() {
		log.debug("EventProcessor: Default Constructor");
	}

	@Override
	public void run() {

		try {
			event = CommonStartup.fProcessingInputQueue.take();
			log.info("EventProcessor\tRemoving element: " + event);

			while (event != null) {
				// As long as the producer is running,
				// we remove elements from the queue.

				// log.info("EventProcessor\tRemoving element: " +
				// this.queue.remove());

				if (CommonStartup.streamid == null) {
					log.error("No StreamID defined for publish - Message dropped" + event.toString());
				} else {
					EventPublisher.getInstance(CommonStartup.cambriaConfigFile, CommonStartup.streamid)
							.sendEvent(event.toString(), CommonStartup.streamid);
				}
				log.debug("Message published" + event.toString());
				event = CommonStartup.fProcessingInputQueue.take();
			}
		} catch (InterruptedException e) {
			log.error("EventProcessor InterruptedException" + e.getMessage());
		}

	}

}
