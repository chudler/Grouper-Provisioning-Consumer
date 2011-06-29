package edu.internet2.middleware.grouper.changeLog.provisioning;

import edu.internet2.middleware.grouper.app.loader.GrouperLoaderConfig;
import edu.internet2.middleware.grouper.changeLog.ChangeLogConsumerBase;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.changeLog.ChangeLogLabel;
import edu.internet2.middleware.grouper.changeLog.ChangeLogLabels;
import edu.internet2.middleware.grouper.changeLog.ChangeLogProcessorMetadata;
import edu.internet2.middleware.grouper.changeLog.ChangeLogTypeBuiltin;
import edu.internet2.middleware.grouper.util.GrouperUtil;
import edu.internet2.middleware.subject.Subject;

import java.lang.reflect.Field;

import java.lang.reflect.Method;

import java.util.List;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;

/**
 *    Portions Copyright 2011 Campus Crusade for Christ / Nathan Kopp
 *    Portions Copyright Rob Hebron
 *    Portions Copyright Internet 2
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
 *  @author Rob Hebron
 */
public class ProvisioningConsumer extends ChangeLogConsumerBase
{
    /** */
    private EventProvisioningConnector connector;

    /** */
    private static final Log LOG = GrouperUtil.getLog(ProvisioningConsumer.class);

    /**
     * @see ChangeLogConsumerBase#processChangeLogEntries(List, ChangeLogProcessorMetadata)
     */
    @Override
    public long processChangeLogEntries(List<ChangeLogEntry> changeLogEntryList, ChangeLogProcessorMetadata changeLogProcessorMetadata)
    {
        String consumerName = changeLogProcessorMetadata.getConsumerName();
        long lastProcessedId = -1;

        try
        {
            loadAndInitConnectorIfNecessary(consumerName);

            for (ChangeLogEntry changeLogEntry : changeLogEntryList)
            {
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("Processing event number " + changeLogEntry.getSequenceNumber());
                }

                ChangeEvent event = createEventFromLogEntry(changeLogEntry, consumerName);

                boolean shouldDispatchEvent = runElFilter(consumerName, event);

                if (shouldDispatchEvent)
                {
                    if(dispatchEvent(changeLogProcessorMetadata, consumerName, event))
                    {
                        lastProcessedId = event.getSequenceNumber();
                    }
                }
            }
            
            connector.flush();
            // after a successful flush, mark the last one in the list as processed
            lastProcessedId = changeLogEntryList.get(changeLogEntryList.size()-1).getSequenceNumber();
            
            if (lastProcessedId == -1)
            {
                throw new RuntimeException("Couldn't process any records");
            }
            return lastProcessedId;
        }
        catch (Exception e)
        {
            if (lastProcessedId == -1)
            {
                throw new RuntimeException("Couldn't process any records", e);
            }
            LOG.error("Error processing ChangeLogEntry " + lastProcessedId, e);
            changeLogProcessorMetadata.registerProblem(e, "Error processing record " + lastProcessedId, lastProcessedId);
            return lastProcessedId;
        }
        finally
        {
            this.connector.close();
        }
    }

    /**
     * Dispatch a single event using the connector.
     *
     * @param changeLogProcessorMetadata
     * @param consumerName
     * @param event
     */
    private boolean dispatchEvent(ChangeLogProcessorMetadata changeLogProcessorMetadata, String consumerName, ChangeEvent event) throws Exception
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Processing event " + event.getSequenceNumber());
        }
        
        if(connector.dispatchEvent(event))
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Prodcessed all events through " + event.getSequenceNumber());
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * If necessary, instantiate the connector class.
     * Initialize the connector.
     *
     * @param consumerName
     */
    private void loadAndInitConnectorIfNecessary(String consumerName) throws Exception
    {
        if (this.connector == null)
        {
            String theClassName = GrouperLoaderConfig.getPropertyString("changeLog.consumer." + consumerName + ".connector.class");
            Class<?> theClass = GrouperUtil.forName(theClassName);
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Creating instance of class " + theClass.getCanonicalName());
            }
            connector = (EventProvisioningConnector)GrouperUtil.newInstance(theClass);
            setConfigProperties(consumerName);
        }
        connector.init(consumerName);
    }

    private void setConfigProperties(String consumerName) throws IllegalAccessException
    {
      Class<?> currentClass = connector.getClass();
      while(!currentClass.getName().startsWith("java.lang"))
      {
          for(Field f : currentClass.getDeclaredFields())
          {
              f.setAccessible(true);
              if(f.getAnnotation(ConfigItem.class)!=null)
              {
                    String configName = "changeLog.consumer." + consumerName + ".connector."+f.getName();
                    String configValue = GrouperLoaderConfig.getPropertyString(configName);
                  if(configValue!=null) throw new RuntimeException("Configuration not found: "+configName);
                  f.set(connector, typeCast(configValue, f.getType()));
              }
          }
          currentClass = currentClass.getSuperclass();
      }
    }
    
    public Object typeCast(String str, Class<?> type)
    {
        if(type.isAssignableFrom(String.class))
        {
            return str;
        }
        else if(type.isAssignableFrom(Long.class))
        {
            return Long.valueOf(str);
        }
        else if(type.isAssignableFrom(Integer.class))
        {
            return Integer.valueOf(str);
        }
        throw new RuntimeException("Type "+type.getName()+" is not supported for @ConfigItem");
    }


    /**
     * Convert the ChangeLogEntry to a ChangeEvent
     *
     * @param changeLogEntry
     * @return
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private ChangeEvent createEventFromLogEntry(ChangeLogEntry changeLogEntry, String consumerName) throws IllegalAccessException, ClassNotFoundException
    {
        ChangeEvent event = new ChangeEvent();
        event.setSequenceNumber(changeLogEntry.getSequenceNumber());
        //if this is a group type add action and category

        // copy all values from the ChangeLogEntry to the POJO ChangeEvent
        for (ChangeLogTypeBuiltin logEntryType : ChangeLogTypeBuiltin.values())
        {
            if (changeLogEntry.equalsCategoryAndAction(logEntryType))
            {
                String typeStr = logEntryType.toString();
                if (LOG.isDebugEnabled())
                    LOG.debug("Event is " + logEntryType.toString());
                try
                {
                    event.setEventType(ChangeEvent.ChangeEventType.valueOf(typeStr).name());
                    //Class labels = (Class)ChangeLogLabels.class.getField(typeStr).get(null);
                    Class labels = Class.forName("edu.internet2.middleware.grouper.changeLog.ChangeLogLabels$" + typeStr);
                    for (Field f : labels.getFields())
                    {
                        String value = this.getLabelValue(changeLogEntry, (ChangeLogLabel)f.get(null));
                        String setterName = "set" + Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1);
                        try
                        {
                            Method setter = event.getClass().getMethod(setterName, String.class);
                            setter.invoke(event, value);
                        }
                        catch (Exception e)
                        {
                            LOG.info("Field " + f.getName() + " not supported by class " + event.getClass().getSimpleName());
                        }
                    }
                }
                catch (IllegalArgumentException e)
                {
                    LOG.info("Unsupported event " + typeStr + ", " + event.getSequenceNumber());
                }

            }
        }
        
        event = addSubjectAttributesIfNecessary(consumerName, event);
      
        return event;
    }

    /**
     *
     * @param changeLogEntry
     * @param changeLogLabel
     * @return label value
     */
    private String getLabelValue(ChangeLogEntry changeLogEntry, ChangeLogLabel changeLogLabel)
    {
        try
        {
            return changeLogEntry.retrieveValueForLabel(changeLogLabel);
        }
        catch (Exception e)
        {
            //cannot get value for label
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Cannot get value for label: " + changeLogLabel.name());
            }
            return null;
        }
    }

    /**
     * Check the configuration and maybe add subject the attributes to the ChangeEvent
     *
     * @param consumerName
     * @param event
     * @return
     */
    private ChangeEvent addSubjectAttributesIfNecessary(String consumerName, ChangeEvent event)
    {
        if (!GrouperLoaderConfig.getPropertyString("changeLog.consumer." + consumerName + ".connector.addSubjectAttributes", "").equals(""))
        {
            event = this.addSubjectAttributes(event, GrouperLoaderConfig.getPropertyString("changeLog.consumer." + consumerName + ".connector.addSubjectAttributes"));
        }
        return event;
    }

    /**
     * Add subject attributes to event
     * @param esbEvent
     * @param attributes (comma delimited)
     * @return esbEvent
     */
    private ChangeEvent addSubjectAttributes(ChangeEvent esbEvent, String attributes)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Adding subject attributes to event");
        }
        Subject subject = esbEvent.retrieveSubject();
        if (subject != null)
        {
            String[] attributesArray = attributes.split(",");
            for (int i = 0; i < attributesArray.length; i++)
            {
                String attributeName = attributesArray[i];
                String attributeValue = subject.getAttributeValueOrCommaSeparated(attributeName);
                if (GrouperUtil.isBlank(attributeValue))
                {
                    if (StringUtils.equals("name", attributeName))
                    {
                        attributeValue = subject.getName();
                    }
                    else if (StringUtils.equals("description", attributeName))
                    {
                        attributeValue = subject.getDescription();
                    }
                }
                if (!StringUtils.isBlank(attributeValue))
                {
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Adding subject attribute " + attributeName + " value " + attributeValue);
                    }
                    esbEvent.addSubjectAttribute(attributeName, attributeValue);
                }
            }
        }
        return esbEvent;

    }

    /**
     * Load and process the EL filter, and determine if this event passes the filter
     *
     * @param consumerName
     * @param event
     * @return true if this event passes, false otherwise
     */
    private boolean runElFilter(String consumerName, ChangeEvent event)
    {
        String elFilter = GrouperLoaderConfig.getPropertyString("changeLog.consumer." + consumerName + ".elfilter", "");
        boolean shouldProcessEvent = false;
        if (StringUtils.isBlank(elFilter))
        {
            shouldProcessEvent = true;
            if (LOG.isDebugEnabled())
            {
                LOG.debug("No filter configured, event " + event.getSequenceNumber() + " processed");
            }
        }
        else if (matchesFilter(event, elFilter))
        {
            shouldProcessEvent = true;
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Event " + event.getSequenceNumber() + " matches filter " + elFilter + ", processing");
            }
        }
        else
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Event " + event.getSequenceNumber() + " does not match consumer filter " + elFilter);
            }
        }
        return shouldProcessEvent;
    }
  
    /**
     * see if the esb event matches an EL filter.  Note the available objects are
     * event for the EsbEvent, and grouperUtil for the GrouperUtil class which has
     * a lot of utility methods
     * @param filterString
     * @param esbEvent
     * @return true if matches, false if doesnt
     */
    public static boolean matchesFilter(ChangeEvent esbEvent, String filterString)
    {
        JexlEngine jexl = new JexlEngine();
        Expression e = jexl.createExpression(filterString);
        JexlContext jc = new MapContext();
        jc.set("event", esbEvent);
        jc.set("grouperUtil", new GrouperUtil());
        return (Boolean)e.evaluate(jc);
    }
}
