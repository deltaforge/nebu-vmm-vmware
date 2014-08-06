
# Nebu VMM extension for VMware

##What is it?

This extension brings VMware support to Nebu. It allows users to perform
automated virtual machine deployment and placement in combination with vCloud
and vSphere.  It provides an API to the Nebu Core. The API it provides is
specified [here](http://api-portal.anypoint.mulesoft.com/nebu/api/nebu-vmm).

##How to run it?

This extension has multiple dependencies. These are all explained below.

###Nebu Common Java
This project uses objects from the Nebu Common library. To run this extension,
the Nebu Common library should be installed with maven. See [this
page](https://github.com/deltaforge/nebu-common-java) for more details on how
to install Nebu Common Java.

###vSphere & vCloud

This extension depends on the use of both vSphere as vCloud. It requires access
to Java libraries for both these systems.

####vCloud

For vCloud, the vCloud Director SDK version 5.1.0 is required. This project
assumes that this jar is installed through maven with the following
specifications:
```xml
<dependency>
  <groupId>com.vmware</groupId>
  <artifactId>vcloud-java-sdk</artifactId>
  <version>5.1.0</version>
</dependency>
```

####vSphere

For vSphere, this project depends on vijava version 5.1. This is an open source
library that works with vSphere. You can find vijava at
[sourceforge](http://vijava.sourceforge.net/).

To work, the vijava jar needs to be installed through maven with the following
specifications:

```xml
<dependency>
  <groupId>com.vmware</groupId>
  <artifactId>vijava</artifactId>
  <version>5.1</version>
</dependency> 
```

Additionally, the VMware extension requires the vSphere account that is used to
have a number of permissions. These are:

| Entity                | Permission name                                |
|:--------------------|:-----------------------------------------------|
|Datastore           | Allocate space                                   |
|Resource           | Migrate powered off virtual machine   |
|Resource           | Migrate powered on virtual machine   |
|Scheduled Task                 | Create tasks                      |
|Scheduled Task                 | Modify task                        |
|Scheduled Task                 | Remove task                     |
|Scheduled Task                 | Run task                            |
|Virtual Machine Interaction | Power off                           |
|Virtual Machine Interaction | Power on                           |
|Datastore cluster                | Configure a datastore cluster          |

###Configuration file

To provide an API to the Nebu Core and to connect to the VMware APIs, a
configuration file is required. An example file is included in the repo called
`config.xml`. This configuration file is self-explanatory. The `key file` field
is not mandatory.

### Can I run it now?

Yes! The repo includes a run script to make things even easier. This script
runs the program through maven. This script assumes that the configuration file
is called `../nebu-vmm-vmware-config.xml`. Please not that this also assumes
that the file is in the directory above the current directory. 

