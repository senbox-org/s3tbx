# s3tbx-c2rcc
This is the source distribution of the Case-2 Regional / Coast Colour (C2R-CC) Atmospheric Correction (AC) and Inherent Optical Properties (IOP) Processor for MERIS, MODIS and the SeaWiFS Level 1b radiance products.

How to build
------------

Make sure you have **git**, **JDK 1.8**, and **Maven 3** installed. Make sure Maven find's the JDK by setting the enviromment variable `JAVA_HOME` to the directory where your JDK is installed. 

Clone or fork the repository at https://github.com/bcdev/s3tbx-c2rcc. 
```
> git clone https://github.com/bcdev/s3tbx-c2rcc.git
> cd s3tbx-c2rcc
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
> **Optical / Thematic Water Processing / C2R-CC**
  
Or in batch mode using SNAP's `gpt` command-line tool found in `${SNAP_HOME}/bin`:
```
> gpt C2RCC -h
> gpt C2RCC [-Psalinity=<num>]  [-Ptemperature=<num>] [-PuseDefaultSolarFlux=true|false] -t <target-file> <source-file>
```  

Modifying, running and debugging the processor code
---------------------------------------------------

This section explains how to run and debug the O2R-CC processor code from a Java IDE without having to install the plugin into SNAP.

You will need to install
* SNAP with the Sentinel-3 Toolbox (S3TBX) from http://step.esa.int/main/download/
* IntelliJ IDEA (Community Edition) IDE from https://www.jetbrains.com/idea/download/

Start IDEA and select **File / New / Project from Existing Sources**. Select the `pom.xml` (Maven project file) in the source directory. Leve all default settings as they are and click **Next** until IDEA asks for the JDK. Select the installed JDK from above and finish the dialog.

From the main menu select **Run / Edit Configurations**. In the dialog click the **+** (add) button and select **JAR Application**. Then the settings are as follows:

* **Name**: SNAP Desktop
* **Path to JAR:** `${SNAP_HOME}/snap/snap/core/snap-main.jar`
* **VM options:** `-Xmx4G -Dorg.netbeans.level=INFO -Dsun.java2d.noddraw=true -Dsun.awt.nopixfmt=true -Dsun.java2d.dpiaware=false` 
* **Program arguments:** `--userdir ${C2RCC_HOME}/target/testdir --clusters ${C2RCC_HOME}/target/nbm/netbeans/s3tbx --patches ${C2RCC_HOME}/$/target/nbm/netbeans/s3tbx`
* **Working directory:** `${SNAP_HOME}`

where 

* `${SNAP_HOME}` must be replaced by your SNAP installation directory
* `${C2RCC_HOME}` must be replaced by your C2R-CC project directory (where the `pom.xml` is located in)







