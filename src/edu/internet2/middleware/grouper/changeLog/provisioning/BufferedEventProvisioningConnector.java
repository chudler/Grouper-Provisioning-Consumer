package edu.internet2.middleware.grouper.changeLog.provisioning;

import java.util.ArrayList;
import java.util.List;

/**
 *    Copyright 2011 Campus Crusade for Christ / Nathan Kopp
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
 *  @author Nathan Kopp
 */
public abstract class BufferedEventProvisioningConnector implements EventProvisioningConnector
{
    @ConfigItem
    private int bufferSize;
    
    private List<ChangeEvent> buffer;
    private String consumerName;

    public BufferedEventProvisioningConnector()
    {
        super();
    }
    
    public synchronized boolean dispatchEvent(ChangeEvent event) throws Exception
    {
        buffer.add(event);
        if(buffer.size()==bufferSize)
        {
            flush();
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public void init(String consumerName)
    {
        this.consumerName = consumerName;
        if(buffer==null) buffer = new ArrayList<ChangeEvent>(bufferSize);
    }

    public synchronized void flush() throws Exception
    {
        dispatchEvents(buffer, consumerName);
        buffer.clear();
    }
    
    public void close()
    {
        buffer.clear();
    }

    protected abstract void dispatchEvents(List<ChangeEvent> buffer, String consumerName) throws Exception;
}
