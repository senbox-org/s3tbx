Sentinel-3 Toolbox Release Notes
================================

Changes in S3TBX 9.0.1
----------------------
### Improvement

    [SIIITBX-413] C2RCC for MSI should make use of provided ECMWF data


Changes in S3TBX 9.0.0
----------------------

### Bug

    [SIIITBX-416] Clear flag is not considered for Landsat Collection 2 OLI data
    [SIIITBX-411] Landsat 9 L1 data cannot be opened
    [SIIITBX-402] The help generation shows errors during the build process
    [SIIITBX-401] Rayleigh Correction fails if band Oa11_radiance is included for correction
    [SIIITBX-397] Fixes for seadas reader fo PACE HICO and MERIS data L1B/L2
    [SIIITBX-395] Metadata value SCENE_LOWER_RIGHT_LONGITUDE is not correct for ALOS
    [SIIITBX-394] AATSR data of 4th reprocessing does not provide geo-location information
    [SIIITBX-388] Extent of OLCI scene is wrong after resampling
    [SIIITBX-386] Fill value is not considered for Landsat Level-2 data
    [SIIITBX-383] Update information regarding the structure of ancillary directory
    [SIIITBX-381] NPE in SeadasFileReader can occur for VIIRS L1B files due to null attributes
    [SIIITBX-380] O2AHarmonisation cannot use OLCI subset products
    [SIIITBX-373] Location information of SLSTR L2 WST data since 2019 behaves strange
    [SIIITBX-365] S3 scenes close to the pole might be wrongly reprojected
    [SIIITBX-364] S3 data containing gaps not correctly handled

### New Feature

    [SIIITBX-409] Support Level 1 of Landsat TM, MSS and ETM+ from USGS Collection2    
    [SIIITBX-408] Updated C2RCC for changes in Landsat8 Col2 data
    [SIIITBX-406] Support Level 1 of Landsat8 Collection 2
    [SIIITBX-405] Support Level 2 of Landsat8 Collection 2
    [SIIITBX-404] Support USGS Landsat Collection 2
    [SIIITBX-398] SeaDAS 3D reader which can output 2D bands for each wavelength in the 3D input file
    [SIIITBX-392] S3 data to use pixel-based geo-location information by default
    [SIIITBX-389] Support NASA distributed MERIS and OLCI files
    [SIIITBX-385] Support for new version of OLCI L2 water
    [SIIITBX-384] Support for Hawkeye data
    [SIIITBX-375] Extend C2RCC S2 with new neural nets
    [SIIITBX-368] Support MetOp-C data
    [SIIITBX-367] MERIS operators shall be able to use data from 4th reprocessing
    [SIIITBX-362] Support S3 QWG recommended flags
    [SIIITBX-361] OLCI Anomaly Detection operator
    [SIIITBX-359] Tutorial on Rayleigh Correction
    [SIIITBX-354] New S3 A/B Harmonisation
    [SIIITBX-353] New Dark Object Subtraction (DOS) operator
    [SIIITBX-324] Support AATSR 4th reprocessing

### Improvement

    [SIIITBX-415] Rename FUB Water processor
    [SIIITBX-414] Autodetection of repro version failed without mentioning the reason
    [SIIITBX-407] Enhance Landsat L1/Col2 support
    [SIIITBX-403] Update reader according PB2.70-A/1.57-B (OGVI->GIFAPAR)
    [SIIITBX-399] Update help pages for SeaDAS reader
    [SIIITBX-393] Implement OLCI anomaly detection OP S3MPC requests
    [SIIITBX-390] FU operator to optionally compute dominant wavelength
    [SIIITBX-378] Help pages for reader shall be moved to S3TBX
    [SIIITBX-366] Update O2 Harmonisation for OLCI A/B
    [SIIITBX-363] Use time_coordinates.nc file of Sentinel-3 data
    [SIIITBX-355] Update FU operator for S2A/B and enhanced documentation
    [SIIITBX-339] Update OLCI O2A Harmonisation

### Requirement

    [SIIITBX-121] MERIS processors need to be updated for the 4th reprocessing

### Task

    [SIIITBX-360] Check for GeoCoding and reprojection for S3 data at pols and ant-meridian
    [SIIITBX-336] Document how to collocate S3 with S1 and S2                  

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10200&version=12706)

Changes in S3TBX 8.0.0
----------------------

### Bug

    [SIIITBX-085] - Proba-V reader should use original band names from metadata
    [SIIITBX-306] - SLSTR L1 product oblique view bands after applying Reprojection are not shown correctly
    [SIIITBX-328] - Rad2Refl Operator cannot handle latest SLSTR input products
    [SIIITBX-329] - Setting pins on OLCI RGB after reprojection is not working
    [SIIITBX-332] - SLSTR L2 FRP products cannot be read
    [SIIITBX-341] - The 'Use pixel-based GeoCoding' is not considered for SLSTR products when opened on fixed resolution
    [SIIITBX-342] - Reading OLCI L1 products in SAFE format is slow
    [SIIITBX-344] - Wrong band index in Rayleigh correction for MERIS

### Task

    [SIIITBX-285] - Obsolete and not working tutorial should be removed from web
    [SIIITBX-288] - Recommended graphs shall be available in GraphBuilder
    [SIIITBX-347] - Remove chris-reader from S3TBX

### Improvement

    [SIIITBX-179] - Meris l2Auxdata module is logging to much
    [SIIITBX-267] - SPOT VGT product reader shall support VGT P collection 3 products
    [SIIITBX-283] - Update operators according to doExecute() changes
    [SIIITBX-289] - All help material shall be revised
    [SIIITBX-290] - Dependencies to 3rd-party libraries shall be cleaned up
    [SIIITBX-330] - Provide better names vor conversion parameter for TSM and CHL
    [SIIITBX-345] - Include support for PACE and Dscovr/Epic to SeadasProductReader

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10200&version=12705)


Changes in S3TBX 7.0.4
--------------------

### Improvement

    [SIIITBX-332] - SLSTR L2 FRP products cannot be read

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10200&version=12761)

Changes in S3TBX 7.0.3
--------------------

### Bug

    [SIIITBX-328] - Rad2Refl Operator cannot handle latest SLSTR input products

### Improvement

    [SIIITBX-267] - SPOT VGT product reader shall support VGT P collection 3 products

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10200&version=12755)


Changes in S3TBX 7.0.2
--------------------

### Bug

    [SIIITBX-301] - SLSTR L2 LST products can not be read
    [SIIITBX-302] - SLSTR L2 WST products can not be read
    [SIIITBX-303] - AATSR products from 4th Reprocessing can not be read
    [SIIITBX-308] - KLM AVHRR reader channel 3a/3b detection not correct

### Improvement

    [SIIITBX-305] - Description of copied masks should not be altered

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10200&version=12754)


Changes in S3TBX 7.0.1
--------------------

### Bug

    [SIIITBX-176] - Rayleigh Correction expects tie-point grids to be present
    [SIIITBX-270] - Wrong bands used for computing the Rhow_OOS
    [SIIITBX-273] - Rayleigh correction does not compute correct values
    [SIIITBX-300] - PPE operator does not copy the masks of the source

### New Feature

    [SIIITBX-249] - Coefficient have change for SLSTR S5 and S6
    [SIIITBX-295] - Support new version of SLSTR L1 RBT
    [SIIITBX-296] - Add Reader for SLSTR L2 FRP products
    [SIIITBX-297] - Add Reader for SLSTR SYN L2 products

### Improvement

    [SIIITBX-172] - Use new SRF for Sentinel-2 in Rayleigh Correction processor
    [SIIITBX-269] - OOS thresholds should have different default values
    [SIIITBX-272] - TSM computation shall use new equation

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10200&version=12742)

Changes in S3TBX 7.0.0
----------------------

    [SIIITBX-265] Include new neural nets for OLCI in C2RCC
    [SIIITBX-263] Include OLCI Harmonisation operator in S3TBX
    [SIIITBX-260] SMAC changes source product while processing
    [SIIITBX-259] SMAC gives different results for two runs
    [SIIITBX-258] Add Reader Tests for S3B
    [SIIITBX-257] SMAC shall be tested
    [SIIITBX-256] Aatsr.SST shall be tested
    [SIIITBX-255] ARC.SST shall be tested
    [SIIITBX-254] FUB.Water shall be tested
    [SIIITBX-253] MphChl shall be tested
    [SIIITBX-252] FuClassification shall be tested
    [SIIITBX-251] Provide gpf tests for more operators
    [SIIITBX-250] Remove Idepix from the S3-Toolbox
    [SIIITBX-247] PDU Stitching may fail due to missing elements
    [SIIITBX-246] Create reader tests for Sentinel-3 data
    [SIIITBX-244] Colours of Idepix masks are random 
    [SIIITBX-233] Source product of ARC SST processor is wrongly named AATSR
    [SIIITBX-231] Add graph tests to the snap-gpt-tests project
    [SIIITBX-220] Provide default graphs for S3TBX use-cases
    [SIIITBX-205] Integrated software building, testing and QA scheme
    [SIIITBX-204] Adapt PDU Stitching Op to SLSTR format change for the F1 channel
    [SIIITBX-203] Adapt reader to SLSTR format change for the F1 channel
    [SIIITBX-197] Update Reader for AATSR data in SAFE format
    [SIIITBX-194] Implement PPE-processor
    [SIIITBX-169] Rayleigh correction operator fails in case of extreme sun angles

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our 
[issue tracking system](https://senbox.atlassian.net/secure/ReleaseNote.jspa?projectId=10200&version=12301)



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

