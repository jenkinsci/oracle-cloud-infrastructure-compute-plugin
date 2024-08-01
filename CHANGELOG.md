# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## 1.0.18 - July 2024

### Added
- JDK 11
- Credential binding so BaremetalCloudCredentials can be used in jenkinsfiles


### Fixed
- Do not bring agent online if init script fails with non-zero return code
- Use login shell for non opc user
- Several dependencies updated to date
- Removed prototype usage in jelly scripts
- Avoid reloading instance templates in template monitor

## 1.0.17 - May 2023

### Added

- OCI Java SDK 2.46.0
- Credentials Plugin 2.6.1.1
- Secure verification strategy for SSH Connection.
- Custom Java options for agent.
- Remoting options for debugging and caching.


### Fixed

- Exclude jersey inside OCI SDK to prevent conflicts with plugins using jersey2.
- Fix hangs when deleting slave.jar on Windows agents.
- Improved logging on instance failure.

## 1.0.16 - May 2022

### Added

- OCI Java SDK 2.27.0
- Switch between multiple templates for provisioning.
- Template Retry option
- Logs for monitoring instances limit
- No deletion of stopped instances upon failures


### Fixed

- Auto Image update option breaks the cloud config.
- NPE in SSHComputerLauncher when InitScript is empty.
- 'Delete Agent' redirect to homepage.

## 1.0.15 - November 2021

### Added

- OCI Java SDK 2.5.1
- Credentials Plugin 2.3.19
- Custom Agent User


### Fixed

- Error reporting - print original stack trace.
- Details on Instance Principals option.

## 1.0.14 - April 2021

### Added

- OCI Java SDK 1.36.0

### Fixed

- Instance Termination if Jenkins tries to reconnect node while its Stopping
- Terminating log string is ambiguous



## 1.0.13 - February 2021

### Added

- OCI Java SDK 1.30.0
- Masking of OCI Credentials
- Explicit multiple Instance Provisioning

### Fixed

- Stop/Start Node's name not matching Instance's name
- Stop/Start Option not affecting already created Nodes



## 1.0.12 - January 2021

### Added

- OCI Java SDK 1.29.0
- Export Jenkins Variables to Init Script
- **Memory in GBs** option for Flex Shapes
- Network Security Groups Compartment option

### Fixed

- java.lang.NumberFormatException: For input string: ""
- NPE in SDKBaremetalCloudClient.getTenant() when using Instance Principals
- Only the last Tag is set when setting more than one Tag



## 1.0.11 - November 2020

### Added

- OCI Java SDK 1.26.0
- Tags Template option
- Custom Instance Name Prefix option
- root compartment added to Compartment list
- Images added to the Stop/Start filter



## 1.0.10 - September 2020

### Fixed

- Fix java.util.NoSuchElementException: No value present



### 1.0.9 - September 2020

### Added

- Network **Subnet Compartment** field.
- **Network Security Groups** field.
- **Identical Named Images** checkbox to automatically select the newest Image if multiple Images exist with same name.
- **Stop on Idle Timeout** checkbox so an instance is stopped and not terminated when the Idle timeout expires.
- Log if an Instance was created via Jenkins Job label or via Jenkins Nodes.

## 1.0.8 - July 2020

### Added

- OCI Java SDK 1.19.2
- Support for E3/Flex Compute Shapes. **Note:** After upgrade please check all OCI Cloud values are OK in Manage Jenkins > Manage Nodes and Clouds > Configure Clouds. Then Click **Save**.
- Improvements in Instance Termination behavior.

### Fixed

- Fix in Exception handling to avoid dangling instances.

## 1.0.7 - June 2020

### Added

- OCI Java SDK 1.17.4

## 1.0.6 - September 2019

### Added

- OCI API keys and SSH Keys are now defined in Jenkins Credentials. **Note:** if upgrading you need to update the values in your existing Cloud configuration(s).
- Support for Instance Principals and calling services from an Instance. See [Calling Services from an Instance](https://docs.cloud.oracle.com/iaas/Content/Identity/Tasks/callingservicesfrominstances.htm) documentation for additional information.
- OCI Java SDK 1.7.0

## 1.0.5 - July 2019

### Added
- Jenkins Master's IP Address to the Instance Names in OCI i.e. jenkins-**12.191.12.125**-182258c6-7dc7-4d8c-acce-1a292a56cfaa.
- Regional subnets in the Virtual Networking service support.
- OCI Java SDK 1.5.11

### Changed
- Default values for **Instance Creation Timeout** and **Instance SSH Connection Timeout** to 900.

### Fixed
- OCI Slaves removing from Jenkins when Jenkins loses Network Connectivity.


## 1.0.4 - November 2018
### Fixed
- Compartments listed are no longer limited to 25 values.
- Child Compartments are now visible in Compartments.

### Added
- Template Instance Cap functionality. Instance Cap can now be placed at Template level.
- "Virtual Cloud Network Compartment" Drop Down in Template configuration to access Network resources in separate compartments. Improves Template loading performance. **Note:** if upgrading from v1.0.3 (or earlier) and the Networks resources is in a separate compartment than the default Compartment, you may have to update the values in your existing Template configuration.

## 1.0.3 - October 2018
### Fixed
- Fix "I/O error in channel oci-compute" java.io.EOFException severe messages in Jenkins log. 
- Fix issue where some values fail due to OCI API limit being exceeded with large number of Templates.

### Changed
- Plugin Description seen in Plugin's Available screen.

### Added
- "Max number of async threads" Field in Cloud configuration. Allows user to specify the max number of async threads to use when loading Templates configuration.
- "Image Compartment" Drop Down in Template configuration for images in separate compartments. **Note:** if upgrading from v1.0.2 (or earlier) and the Images are in a separate compartment than the default Compartment, you may have to update the values in your existing Template configuration.


## 1.0.2 - June 2018
### Fixed
- Instance cap can no longer be exceeded
- Fix error on Node Configuration Screen

### Changed
- Subnets now filtering by Availability Domain
- Use Jenkins HTTP proxy configuration for OCI API calls
- Prevent termination of temporarily offline Agents

### Added
- Faster loading of Cloud and Template configuration options in Jenkins Configure screen
- Better error description for remote machine with no Java installed
- "Name" and "Number of Executors" reconfiguration options in the Nodes > Configure Screen

## 1.0.1 - April 2018
### Fixed

- Idle Termination Minutes. 0 now working as expected and Instance will not Terminate.

- Fixed broken links in Plugin Help options.


- Fixed "unexpected stream termination" issue which removes HTTP Proxy for ssh connection to agents.
- ssh credentials are now encrypted in Jenkins config file.


### Changed
- Shorten Compartment Drop-Down names and removed bloated bracket content.

### Added
- Ability to access Images, Virtual Cloud Network, and Subnet items from separate Compartments.

- Checkbox to attach Public IP to Instances. If this option is unchecked, only the private IP is assigned. 


- Checkbox to use Public IP to ssh to instances. If this Option is unchecked, the Plugin will connect to the private IP of the instance. 

## 1.0.0 - December 2017
### Added
- Initial Release
- Support added for OCI resource allocation via Jenkins plugin
