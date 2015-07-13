# s3tbx-c2rcc
This is the source distribution of the Case-2 Regional / Coast Colour (C2RCC) Atmospheric Correction (AC) and Inherent Optical Properties (IOP) Processor for MERIS, MODIS and the SeaWiFS Level 1b radiance products.

How to build
============

Clean build

  mvn clean compile


How to install as SNAP plugin 
=============================

Once the C2RCC processor is installed into SNAP it can be run from the SNAP Desktop UI
  Menu / Optical / Thematic Water Processing / C2R-CC
  
Or in batch mode using SNAP's `gpt` command-line tool found in `${SNAP_HOME}/bin`:
  gpt C2RCC [-Psalinity=<num>]  [-Ptemperature=<num>] [-PuseDefaultSolarFlux=true|false] -t <target-file> <source-file>
  

Running and debugging the processor
===================================


How to run from the command-line
================================

The C2RCC processor is invoked via the gpt tool which is part of the SNAP installation.

How to run from SNAP Desktop UI
===============================


