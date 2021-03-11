Sentinel-3 Toolbox (S3TBX) [![Build Status](https://travis-ci.org/senbox-org/s3tbx.svg?branch=master)](https://travis-ci.org/senbox-org/s3tbx)
==========================

A toolbox for the OLCI and SLSTR instruments on board of ESA's Sentinel-3 satellite.

The project page of SNAP, and the sentinel toolboxes can be found at http://step.esa.int. 
There you can find tutorials, developer guides, a user forum and other interesting things.


Building S3TBX from the source
------------------------------

The following gives a brief introduction how to build the S3TBX.
More information can be found in the [Developer Guide](https://senbox.atlassian.net/wiki/display/SNAP/Developer+Guide).


Download and install the required build tools

* Install Java 8 JDK and set JAVA_HOME accordingly. A distribution of OpenJDK is suggested. 
Several distributions are available, for example
  * [Azul Zulu](https://www.azul.com/downloads/zulu-community)  
  * [AdoptOpenJDK](https://adoptopenjdk.net)   
  * [Amazon Corretto](https://aws.amazon.com/de/corretto)	  
* Install Maven and set MAVEN_HOME accordingly. 
* Install git

Add $JAVA_HOME/bin, $MAVEN_HOME/bin to your PATH.

Clone the S3TBX source code and related repositories into a directory referred to a ${snap} from here on

    cd ${snap}
    git clone https://github.com/senbox-org/s3tbx.git
    git clone https://github.com/senbox-org/snap-desktop.git
    git clone https://github.com/senbox-org/snap-engine.git
    
Build SNAP-Engine:

    cd ${snap}/snap-engine
    mvn install

Build SNAP-Desktop:

    cd ${snap}/snap-desktop
    mvn install

Build Sentinel-3 Toolbox:

    cd ${snap}/s3tbx
    mvn install
   
If unit tests are failing, you can use the following to skip the tests
   
    mvn clean
    mvn install -Dmaven.test.skip=true
	
Setting up IntelliJ IDEA
------------------------

1. Create an empty project with the ${snap} directory as project directory

2. Import the pom.xml files of snap-engine, snap-desktop and s3tbx as modules. Ensure **not** to enable
the option *Create module groups for multi-module Maven projects*. Everything can be default values.

3. Set the used JDK for the main project.

4. Use the following configuration to run SNAP in the IDE:

    **Main class:** org.esa.snap.nbexec.Launcher
    **VM parameters:** -Dsun.awt.nopixfmt=true -Dsun.java2d.noddraw=true -Dsun.java2d.dpiaware=false
    All VM parameters are optional
    **Program arguments:**
    --userdir
    "${snap}/s3tbx/target/userdir"
    --clusters
    "${snap}/s3tbx/s3tbx-kit/target/netbeans_clusters/s3tbx"
    --patches
    "${snap}/snap-engine/$/target/classes;${snap}/s3tbx/$/target/classes"
    **Working directory:** ${snap}/snap-desktop/snap-application/target/snap/
    **Use classpath of module:** snap-main

Enjoy developing!


