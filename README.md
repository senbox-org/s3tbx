# s3tbx-c2rcc
This is the source distribution of the Case-2 Regional / Coast Colour (C2RCC) Atmospheric Correction (AC) and Inherent Optical Properties (IOP) Processor for MERIS, MODIS and the SeaWiFS Level 1b radiance products.

How to build
------------

Clone or fork the repository at https://github.com/bcdev/s3tbx-c2rcc.

```
> git clone https://github.com/bcdev/s3tbx-c2rcc.git
> cd s3tbx-c2rcc
```

Incremental build:
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

How to install and run as SNAP plugin 
-------------------------------------

Start SNAP (Desktop UI) and find the plugin manager in the main menu at 
> Tools / Plugins

Then 
* select tab *Downloaded*, 
* click button *Add Files* and 
* select the plugin module file `target/nbm/s3tbx-c2rcc-<version>.nbm`. 
* Click *Install*, 
* then *Close* and 
* restart SNAP.

Once the C2RCC processor is installed into SNAP it can be run from the SNAP Desktop UI's main menu at
> Optical / Thematic Water Processing / C2R-CC
  
Or in batch mode using SNAP's `gpt` command-line tool found in `${SNAP_HOME}/bin`:
```
> gpt C2RCC -h
> gpt C2RCC [-Psalinity=<num>]  [-Ptemperature=<num>] [-PuseDefaultSolarFlux=true|false] -t <target-file> <source-file>
```  

Running and debugging the processor code (without plugin installation)
----------------------------------------------------------------------


How to run from the command-line
================================

The C2RCC processor is invoked via the gpt tool which is part of the SNAP installation.

How to run from SNAP Desktop UI
===============================


