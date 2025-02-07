# Example Product Catalog component

This is an example implementation of a [TM Forum Product Catalog Management](https://www.tmforum.org/oda/directory/components-map/core-commerce-management/TMFC001) component.


In its **core function** it implements:
* The *mandatory* TMF620 Product Catalog Management Open API. 
* The *optional* TMF671 Promotion Management Open API.
* the *optional* dependency on one or more downstream TMF620 Product Catalog Management Open APIs to support federated product catalog scenarios.

In its **management function** it implements:
* Am *optional* metrics API supporting the open metrics standard (formerly the prometheus de-facto standard)
* Outbound Open Telemetry events.

In its **security function** it implements:
* The *mandatory* TMF669 Party Role Management Open API.


The implementation consists of 7 microservices:
* a partyRole microservice to implemnt the TMF669 Party Role Management Open API.
* roleInitialization microservice that bootstraps the initial Party Role interface. This is depoyed as a Kubernetes Job that runs once when the component is initialised.
* a productCatalog microservice to implement the TMF620 Product Catalog Management Open API.
* a promotionManagement microservice to implement the TMF671 Promotion Management Open API.
* a openMetrics microservice that implements the open metrics API.
* a productCatalogInitialization microservice that registers the metrics microservice as a listener for product catalog create/update/delete business events.  This is depoyed as a Kubernetes Job that runs once when the component is initialised.
* a simple deployment of a mongoDb. This is deployed as a Kubernetes Deployment with a PersistentVolumeClaim.


This reference component is intended to be used as a showcase for the ODA Component model, and to be used for testing the ODA Canvas. It is not intended for production deployments.


## Installation

Install this component (assuming the kubectl config is connected to a Kubernetes cluster with an operational ODA Canvas) using:
```
helm install r1 .\productcatalog -n components
```

You can test the component has deployed successfully using
```
kubectl get components -n components
```

You should get an output like 
```
NAME                          DEPLOYMENT_STATUS
r1-productcatalogmanagement   Complete
```

(The DEPLOYMENT_STATUS will cycle through a number of interim states as part of the deployment). 
If the deployment fails, refer to the [Troubleshooting-Guide](https://github.com/tmforum-oda/oda-ca-docs/tree/master/Troubleshooting-Guide).

 
## Configuration
You can configure the following aspects of the component:
- OpenTelemetry tracing and metrics
  - Any OTL endpoint with HTTP traces will do. By default, the component is configured to send traces to the Datadog agent.
- MongoDB Database connection

You can do that  by changing the values in the values.yaml file, or by setting the values on the command line when you install the component using the --set parameter.

relevant variables:

| Variable Name    	                           | Default                          	                               | Explanation                                                                                	                                                                                                  |
|----------------------------------------------|------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `mongodb.port`     	                         | 27017                            	                               | the port to connect to the mongodb instance the Host will be derived from the Release name 	                                                                                                  |
| `mongodb.database` 	                           | tmf                              	                               | the database name to connect to the mongodb instance                                       	                                                                                                  |
| `api.image`        	                           | csotiriou/productcatalogapi:0.10 	                               | The image for the implementation of the main api microservice                              	                                                                                                  |
| `api.otlp.console.enabled`        	            | false 	                                                          | Whether OpenTelemetry traces will be recorded in the console instead of being sent to the collector                              	                                                            |
| `api.otlp.protobuffCollector.enabled`        	 | true 	                                                           | Whether OpenTelemetry traces will be recorded in the OTL Collector instead of the console. Does not work if `api.otlp.console.enabled` is `true`                                              |
| `api.otlp.protobuffCollector.url`        	     | http://datadog-agent.default.svc.cluster.local:4318/v1/traces 	  | The host of the OTL Collector. Only used if `api.otlp.protobuffCollector.enabled` is `true`. By default it's set to the url of the collector. However, any OTL collector endpoint will suffice |
| `partyrole.image`        	                     | The image for the implementation of the partyrole microservice 	 | |

Note that in the above configuration, MongoDB configuration is shared among the partyrole and the main microservice. The host of the MongoDB database is set automatically, since it depends on the release name (it's being installed along the rest of the microservices inside the cluster).
