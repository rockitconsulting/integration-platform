[![N|Solid](http://www.rockit.consulting/images/logo-fixed.png)](http://www.rockit.consulting)

# IIB-Scheduler

The IBB-Scheduler application provides the functions of a cron pattern to be used.

## CronManager.msgflow

![IIB-Scheduler](https://raw.githubusercontent.com/rockitconsulting/integration-platform/master/IIB-Scheduler/IIB-Scheduler/docs/img/Scheduler.PNG?raw=true)


## Config.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<p:schedulerMessage xmlns:p="http://rockit.com/scheduler" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://rockit.com/scheduler com/rockit/ip/scheduler/schedulerMessage.xsd ">
	<schedulerRecord>
		<schedulerId>IdTest</schedulerId>
		<brokerName>xyz</brokerName>
		<cronPattern>* 00 09 01 01 *</cronPattern>
		<queueName></queueName>	
	</schedulerRecord>	
	<schedulerRecord>
		<schedulerId>Id1</schedulerId>
		<brokerName>xyz</brokerName>
		<cronPattern>0-59 0-59 0-23 * * *</cronPattern>
		<queueName></queueName>
	</schedulerRecord>
	<schedulerRecord>
		<schedulerId>Id2</schedulerId>
		<brokerName>xyz</brokerName>
		<cronPattern>0 0 8-10 * * *</cronPattern>
		<queueName></queueName>	
	</schedulerRecord>
</p:schedulerMessage>
```