=======================================
Instructions
=======================================
1) Compile and craete a jar file
2) Copy the jar to .../grouper/lib/custom
3) Edit .../grouper/conf/grouper-loader.properties and add the following lines:
      changeLog.consumer.<connector-name>.quartzCron = 0 * * * * ?
      changeLog.consumer.<connector-name>.class = edu.internet2.middleware.grouper.changeLog.provisioning.ProvisioningConsumer
      changeLog.consumer.<connector-name>.elfilter = event.eventType eq 'MEMBERSHIP_DELETE' \|\| event.eventType eq 'MEMBERSHIP_ADD'
      changeLog.consumer.<connector-name>.connector.class = <your-connector-class>
      changeLog.consumer.<connector-name>.connector.<prop1-name> = <prop1-value>
      changeLog.consumer.<connector-name>.connector.<prop2-name> = <prop2-value>
      changeLog.consumer.<connector-name>.connector.<prop3-name> = <prop3-value>
              :

Example:
      changeLog.consumer.<connector-name>.quartzCron = 0 * * * * ?
      changeLog.consumer.<connector-name>.class = edu.internet2.middleware.grouper.changeLog.provisioning.ProvisioningConsumer
      changeLog.consumer.<connector-name>.elfilter = event.eventType eq 'MEMBERSHIP_DELETE' \|\| event.eventType eq 'MEMBERSHIP_ADD'
      changeLog.consumer.<connector-name>.connector.class = org.ccci.idm.groupersiebelpc.SiebelConnector
      changeLog.consumer.<connector-name>.connector.username = myusername
      changeLog.consumer.<connector-name>.connector.password = my-password
      changeLog.consumer.<connector-name>.connector.url = <siebel-url-here>


4) Run grouper loader with the following command:
      gsh -loader

