# s3tbx-c2rcc
This is the source distribution of the Case-2 Regional / Coast Colour (C2RCC) Atmospheric Correction (AC) and 
Inherent Optical Properties (IOP) Processor for Sentinel-3 OLCI, Sentinel-2 MSI, Landsat-8, MERIS (incl. 4th repro), 
MODIS and the SeaWiFS Level 1C radiance products.

Documentation
-------------
As this processor is still in its pre-release phase and not yet final, the documentation is also very sparse.
Some documentation can be found in the [docs folder](https://github.com/bcdev/s3tbx-c2rcc/tree/master/docs). 

How to build
------------

Make sure you have **[git](https://git-scm.com/)**, 
**[JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)**, and 
**[Maven 3](https://maven.apache.org/)** installed. Make sure Maven find's the JDK by setting the enviromment variable `JAVA_HOME` to the directory where your JDK is installed. 

Clone or fork the repository at https://github.com/bcdev/s3tbx-c2rcc. 
```
> git clone https://github.com/bcdev/s3tbx-c2rcc.git
> cd s3tbx-c2rcc
```

You can update your checked-out sources from the remote repository by running 
```
> git pull --rebase
```

Incremental build with Maven:
```
> mvn package
```

Clean build:
```
> mvn clean package
```  

If you encounter test failures:
```
> mvn clean package -DskipTests=true
```

The build creates a SNAP plugin module file `target/nbm/s3tbx-c2rcc-<version>.nbm`.

How to install and run the processor as SNAP plugin 
---------------------------------------------------

Start SNAP (Desktop UI) and find the plugin manager in the main menu at 
> **Tools / Plugins**

Then 
* select tab **Downloaded**, 
* click button **Add Files** and 
* select the plugin module file `target/nbm/s3tbx-c2rcc-<version>.nbm`. 
* Click **Install**, 
* then **Close** and 
* restart SNAP.

Once the C2RCC processor is installed into SNAP it can be run from the SNAP Desktop UI's main menu at
> **Optical / Thematic Water Processing / C2RCC / \<Sensor\>**
  
Or in batch mode using SNAP's `gpt` command-line tool found in `${SNAP_HOME}/bin`. Depending on the sensor you want 
to execute you can call the following to get help.
```
> gpt c2rcc.<sensor> -h
```
e.g.
```
> gpt c2rcc.meris -h
```  
Available at the time of writing are:
* c2rcc.landsat8
* c2rcc.meris   
* c2rcc.meris4  
* c2rcc.modis   
* c2rcc.msi     
* c2rcc.olci    
* c2rcc.seawifs
* c2rcc.viirs   
 


Modifying, running and debugging the processor code
---------------------------------------------------

This section explains how to run and debug the C2RCC processor code from a Java IDE without having to install the plugin into SNAP.

You will need to install
* SNAP with the Sentinel-3 Toolbox (S3TBX) from http://step.esa.int/main/download/
* IntelliJ IDEA (Community Edition) IDE from https://www.jetbrains.com/idea/download/

Start IDEA and select **File / New / Project from Existing Sources**. Select the `pom.xml` (Maven project file) in the source directory. Leave all default settings as they are and click **Next** until IDEA asks for the JDK. Select the installed JDK from above and finish the dialog.

From the main menu select **Run / Edit Configurations**. In the dialog click the **+** (add) button and select **JAR Application**. Then the settings are as follows:

* **Name**: SNAP Desktop
* **Path to JAR:** `${SNAP_HOME}/snap/snap/core/snap-main.jar`
* **VM options:** `-Xmx4G -Dorg.netbeans.level=INFO -Dsun.java2d.noddraw=true -Dsun.awt.nopixfmt=true -Dsun.java2d.dpiaware=false` 
* **Program arguments:** `--userdir ${C2RCC_HOME}/target/testdir --clusters ${C2RCC_HOME}/target/nbm/netbeans/s3tbx --patches ${C2RCC_HOME}/$/target/classes`
* **Working directory:** `${SNAP_HOME}`

where 

* `${SNAP_HOME}` must be replaced by your SNAP installation directory
* `${C2RCC_HOME}` must be replaced by your C2RCC project directory (where the `pom.xml` is located in)







