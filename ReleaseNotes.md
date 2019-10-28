Sentinel-3 Toolbox Release Notes
================================

Changes in S3TBX 7.0.0
--------------------

* [SIIITBX-265] Include new neural nets for OLCI in C2RCC
* [SIIITBX-263]	Include OLCI Harmonisation operator in S3TBX
* [SIIITBX-260]	SMAC changes source product while processing
* [SIIITBX-259]	SMAC gives different results for two runs
* [SIIITBX-258]	Add Reader Tests for S3B
* [SIIITBX-257]	SMAC shall be tested
* [SIIITBX-256]	Aatsr.SST shall be tested
* [SIIITBX-255]	ARC.SST shall be tested
* [SIIITBX-254]	FUB.Water shall be tested
* [SIIITBX-253]	MphChl shall be tested
* [SIIITBX-252]	FuClassification shall be tested
* [SIIITBX-251]	Provide gpf tests for more operators
* [SIIITBX-250]	Remove Idepix from the S3-Toolbox
* [SIIITBX-247]	PDU Stitching may fail due to missing elements
* [SIIITBX-246]	Create reader tests for Sentinel-3 data
* [SIIITBX-244]	Colours of Idepix masks are random 
* [SIIITBX-233]	Source product of ARC SST processor is wrongly named AATSR
* [SIIITBX-231]	Add graph tests to the snap-gpt-tests project
* [SIIITBX-220]	Provide default graphs for S3TBX use-cases
* [SIIITBX-205]	Integrated software building, testing and QA scheme
* [SIIITBX-204]	Adapt PDU Stitching Op to SLSTR format change for the F1 channel
* [SIIITBX-203]	Adapt reader to SLSTR format change for the F1 channel
* [SIIITBX-197]	Update Reader for AATSR data in SAFE format
* [SIIITBX-194]	Implement PPE-processor
* [SIIITBX-169]	Rayleigh correction operator fails in case of extreme sun angles


Changes in S3TBX 6.0.6
--------------------

* [SIIITBX-241] Sensing time for MODIS L2 depends on local time zone
* [SIIITBX-240] Sensing time for Alos/Prism is not correct
* [SIIITBX-239] Module updates shall come with release notes / changelog
* [SIIITBX-237] Release module update S3TBX 6.0.6
* [SIIITBX-236] New S3 VG1 data can't be read 
* [SIIITBX-235] Update Landsat8 reader tests according to flag changes
* [SIIITBX-234] Update C2RCC according to changed Landsat-8 flags
* [SIIITBX-232] Cannot read SYN VG1 products
* [SIIITBX-227] Support for Landsat products processed by ESA
* [SIIITBX-226] AVHRR-FRAC reader geocoding is incorrect
* [SIIITBX-223] Rayleigh operator states it needs S2 L1b but actually it is L1C
* [SIIITBX-206] S3 products shall not contain NetCDF metadata elements
* [SIIITBX-192] Not able to create subset from SLSTR L2 data


Changes in S3TBX 6.0.5
--------------------

* [SIIITBX-228] Reference the ATBD for MERIS 4th reprocessing in the help
* [SIIITBX-195] Make user options for Idepix VIIRS more convenient
* [SIIITBX-193] SLSTR Stitching operator does not work from command line


Changes in S3TBX 6.0.4
--------------------

* [SIIITBX-185] Rad2Refl produces NaN if radiance is zero
* [SIIITBX-184] FUB Water shall not retrieve full input image if only subset is requested


Changes in S3TBX 6.0.3
--------------------

* [SIIITBX-230] SLSTR calibration factors shall only be applied if enabled
* [SIIITBX-229] Update help regarding s3tbx.properties file


Changes in S3TBX 6.0.2
--------------------

**Not released**


Changes in S3TBX 6.0.1
--------------------

* [SIIITBX-182] Allow setting calibration factors for Sentinel-3 L1B
* [SIIITBX-180] In certain circumstances the tie-points and bands are swapped in SLSTR L2 LST data
* [SIIITBX-178] OLCI PixelGeoCoding might give invalid pixel positions on request although latitude and longitude bands contain valid data
* [SIIITBX-175] Add reference to new remote sensing article
* [SIIITBX-174] C2RCC: Default value 'Threshold rtosa OOS' should be set to 0.05
* [SIIITBX-173] C2RCC: For Landsat8 and Landsat7 the threshold for cloud test is used with wrong transmittance wavelength

Changes in S3TBX 6.0
--------------------

### New Features and Important Changes
* **C2RCC processor integrated** - The _Case-2 Regional / CoastColour_ processor. 
It is a processor for retrieving water constituents in coastal zones. Supports several sensors. 
Starting from Sentinel-3 OLCI, Sentinel-2 MSI to Landsat-8 OLI and the heritage sensor MERIS. 
Also supported are MODIS, VIIRS and SeaWiFS.
* **Support Rayleigh Correction of S2-MSI** - The _Rayleigh Correction_ processor has been extended to support the S2-MSI data. Now it supports MERIS, 
OLCI and MSI. 
* **Support Rad2Refl for SLSTR** - The _Radiance-to-Reflectance_ processor can now convert also Sentinel-2 SLSTR data from radiance to reflectance.
* **Support Landsat data from dataset 'Collection 1'** - Landsat data from the 'Collection 1 can now be read'
* **FU Classification extended for S2-MSI, MODIS500 and CZCS** - The _FU Classification_ can now be used for the S2-MSI, CZCS and MODIS in 500 meter 
resolution.

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our 
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10200&version=12207)


Changes in S3TBX 5.0
--------------------

### New Features and Important Changes
* **AATSR Regridding Tool**
AATSR data is acquired with a conical scanning geometry. To display the acquisitions as a raster image the raw data is 
transformed into a gridded L1 TOA product at a resolution of 1 km. This "gridding" modifies the exact pixel position and resolution. 
This Tool allows the “re-gridding” of the AATSR data back into their original instrument geometries. 
* **Rayleigh Correction Processor for OLCI and MERIS**
This new operator allows the correction of the rayleigh scattering for OLCI and MERIS.
* **OWT Processor**
This processor, developed by Timothy Moore (University of New Hampshire), calculates optical water types. The classification is 
based on atmospherically corrected reflectances.
* **FUB Processor**
This processor retrieves case II water and atmospheric properties for MERIS. It has been developed by Thomas Schroeder and 
Michael Schaale from Freie Universitaet Berlin.
* **PROBA-V Toolbox**
This new toolbox is intended for the exploitation of PROAB-V data. Therefore the PROBA-V reader has been moved from the 
Sentinel-3 Toolbox into the PROBA-V Toolbox. If you still need the reader you have to install the toolbox. 
* **IDEPIX extended for more sensors**
The pixel identification tool IDEPIX has been extended to support more sensors. Among the supported sensors are now: 
MERIS, OLCI, VIIRS, PROAB-V, SeaWiFS and SPOT-VGT

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our 
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10200&version=11501)



Changes in S3TBX 4.0
--------------------

### New Features and Important Changes
* New colour classification based on the Forel–Ule scale has been implement for OLCI, MERIS, 
  SeaWiFS and MODIS. Thanks to Hendrik Jan van der Woerd and Marcel R. Wernand from the Royal 
  Netherlands Institute for Sea Research (NOIZ) for the algorithm and the validation. 
* The Sentinel-3 Reader have been improved according errors and format changes have been adapted.
* The fractional Land/Water Mask operator has been moved into SNAP because of its general usability.          

### Solved issues
#### Bugs
    [SIIITBX-096] - Read SLSTR L2 WCT oblique view bands
    [SIIITBX-097] - Subset of SeaDas L2 files not correct
    [SIIITBX-099] - Silent error on product type null or empty
    [SIIITBX-102] - Apply solar illumination correction factors to SPOT VGT P products
    [SIIITBX-108] - Reading from NetCDF file is not snychronised in all cases in OLCI reader
    [SIIITBX-112] - Reprojecting SLSTR L1B products with tie-point geo-codings creates shifts within images
    [SIIITBX-113] - S3 SLSTR WST should not use valid mask for its geo-coding

#### New Feature
    [SIIITBX-114] - Integrate the colour classification based on discrete Forel–Ule scale

#### Task
    [SIIITBX-107] - Move Land/water mask operator into SNAP

#### Improvement
    [SIIITBX-098] - Rad2Refl operator is slow
    [SIIITBX-100] - LandsatReader should not search mtl file if it is already specified
    [SIIITBX-104] - Cloud operator should consistently use the system logger


Changes in S3TBX 3.0
--------------------

### New Features and Important Changes
* The Idepix Processor provides a pixel classification into properties such as clear/cloudy, land/water, snow, ice etc. The processing 
options/parameters as well as the underlying classification algorithms are instrument-dependent. The Idepix Processor provided with the current 
SNAP version supports MODIS and Landsat-8.
* The ARC Processor is aimed to enable the user to calculate the sea-surface temperature and Saharan Dust Index from (A)ATSR brightness temperatures.
* The new SLSTR L1B PDU Stitching Tool stitches multiple SLSTR L1B product dissemination units (PDUs) of the same orbit to a single product.
* A new client tool has been developed for accessing online in-situ databases. In the current version this tool has the purpose of a demonstrator 
and is limited in functionality. Currently the In-Situ Client gives limited access to the [MERMAID In-Situ Database](http://mermaid.acri.fr/home/home.php)
hosted by ACRI-ST. Two datasets are available, namely BOUSSOLE and AERONET-OC.
* The Fractional Land/Water Mask Processor creates a new product based on the source product and computes a land/water mask. For each pixel, 
it contains the fraction of water; a value of 0.0 indicates land, a value of 100.0 indicates water, and every value in between indicates 
a mixed pixel.

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our 
[issue tracking system](https://senbox.atlassian.net/issues/?filter=11509)

# Release notes of former versions

* [Resolved issues in version 2.x](https://senbox.atlassian.net/issues/?filter=11508)
* [Resolved issues in version 2.0](https://senbox.atlassian.net/issues/?filter=11507)
* [Resolved issues in version 2.0 beta](https://senbox.atlassian.net/issues/?filter=11506)
* [Resolved issues in version 1.0.1](https://senbox.atlassian.net/issues/?filter=11505)

