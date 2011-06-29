/*
 * @author Nathan Kopp (code copied from Rob Hebron)
 */

package edu.internet2.middleware.grouper.changeLog.esb2.consumer;

import edu.internet2.middleware.grouper.app.loader.GrouperLoaderConfig;
import edu.internet2.middleware.grouper.changeLog.provisioning.ChangeEvent;
import edu.internet2.middleware.grouper.changeLog.provisioning.EventProvisioningConnector;
import edu.internet2.middleware.grouper.util.GrouperUtil;

/**
 * Publishes Grouper events to HTTP(S) server as JSON strings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  @author Rob Hebron
 */
public abstract class EsbPublisherJsonBase implements EventProvisioningConnector
{
    protected String consumerName;
    
    public boolean dispatchEvent(ChangeEvent event)
    {
        ChangeEvents events = new ChangeEvents();
        events.addEsbEvent(event);
        String eventJsonString = GrouperUtil.jsonConvertToNoWrap(events);

        //String eventJsonString = gson.toJson(event);
        // add indenting for debugging
        // add subject attributes if configured

        if (GrouperLoaderConfig.getPropertyBoolean("changeLog.consumer." + consumerName + ".publisher.debug", false))
        {
            eventJsonString = GrouperUtil.indent(eventJsonString, false);
        }
        return dispatchEvent(eventJsonString, consumerName);
    }

    public abstract boolean dispatchEvent(String eventJsonString, String consumerName);

    public void init(String consumerName)
    {
        this.consumerName = consumerName;
    }
  
    public void flush()
    {
    }

}
