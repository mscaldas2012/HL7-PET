# HL7-PET

# Introduction

This library is built in scala and the main purpose is to (**P**)arse, (**E**)xtract, (**T**)ransform
and Validate HL7 v2 messages.

HL7 profiles can be defined in JSON and loaded to the appropriate classes to be used for better understanding.
By default, if not specified, the current code loads the <code>PhinGuideProfile.json</code> with the <code>DefaultFieldsProfile.json</code>

The validation is still a work in puprogress. But prime time, you can debatch messages and use the extractor
to retrieve values from specific fields of a given HL7 message natively, i.e., without having to transform
the raw pipe-delimited message into another object oriented format. Because its using scala under the hood,
this approach seems to be working very effectively and can return values very fast.

# Build Instructions
## Prerequisites
- Java Development Kit (JDK) 11
- Scala (Version) 2.13.10
- SBT (Scala Build Tool)
- GitHub Personal Access Token
  
## Installation
- Clone the repository: https://github.com/CDCgov/hl7-pet.git
- Create GITHUB_TOKEN from your personal GithHub Settings>Developer Settings->Personal Access Tokens>Generate New Token(with any scope), set it as environment variable and update the build.sbt file accordingly
- Build the project
- from sbt terminal run 'package' command for jar file to be generated
  

# Extracting Values from a HL7 message.

For a code example on how to initialize the parser, look intot he ExampleApp under test/scala. 
that code loads a demonstration file and allows the user to type paths to retrieve values on the command line.

The path follows this schema: <code>SEG[index]-FIELD[index].COMPONENT.SUBCOMPONENT</code>

where:

* **SEG** is the three-letter semgent name. SEG is the only required piece of a valid path.
  * Ex.: MSH, PID, OBR, OBX, PV1, etc...
* **SEG[index]** specifies a specific segment we are interested in, in case the segment is a repeating one.
 <br>Ex.: 
    * OBR[1], retrieves the first occurrence of an OBR
    * OBR[2], retrieves the second occurrence of an OBR.
    
segment index can have the format of a Filter criteria in the following format: 
<code>@Field.Component.subcomponent='VALUE'</code> (Only **@Field** is required) 
<br>Ex.:
    * OBR[@4.1='698775-3'], retrieves any OBR segment where the 1st component of the 4th field has a value of
    698775-3. 
    * OBX[@4='2'], retrieves any OBX segment where the 4th field has a value of 2.
    
* **FIELD** is the number of the field we are interested in.
* **FIELD[index]** is for repeating fields, where we are interested on a specific field repetition. 
This index only accepts a integer.
<BR>Ex.: 
  * MSH[1]-12, retrieves the value of the 12th field of the MSH segment.
  * MSH[1]-21[3], retrieves the value of the 3rd entry for MSH field 21 (Message Profile Identifier)   

* **COMPONENT**, specifies a specific component when the field can be divided further into components.
<br> Ex.:
  * MSH[1]-21[3].1, retrieves the first component of the 3rd entry of MSH 21.

* **SUBCOMPONENT**, specifies a specific subcomponent when the component can be further divided.

Note: The parser is very lenient in the sense that it tries its best to retrieve a value and not crash.
If the path is mispelled or does not follow the appropriate syntax, it usually simply returns an empty result.
Also, it always considers that a field has at least one compoenent and one subcomponent.
<br>Ex.:
  * MSH[1]-12[1].1.1 is the same as MSH-12, because there's only one MSH, field 12 is a non repeating
  field and you can't break down field 12 into components and subcomponents. The first path is a
  very specific path and in case MSH-12 has some weird values, or is repeated, that specification will
  avoid retrieving other values.
  
# Validation

HL7-PET can also perform structure validation. See #StructureValidator for more info. StructureValidatorTests
runs a simple structure validation with the profiles provided (based on ORU 2.5.1 messages)

Structure validation cvan perform type, cardinality, usage and other types of structure validation.
  
  
### Restrictions

Currently, HL7 pet has some restrictions:
* It only supports messages where MSH-1 is defined as a pipe ('|') and MSH-2 as ^~\&
* The parser does not understand escaped characters as of the current version (1.2.2)
* Segment filters used for index only supports a single equality comparison to a single string. 
<BR>Ex.: MSH[@21.1='VALUE']. 

### Other uses

This project holds several utility classes and applications 
to help with development and data preparation

Here's the list of available tools:<BR>
(More to Come, this file should be updated as new features become available.)

* HL7FileUtils:<BR>
        - this class provides several utilities to handle HL7 Messages
   * A method to split several MSH messages in one file into it's own files.
   * A method to generate clean text files (No HL7 markup) from OBX fields.
   
* DeIdentifier:<BR>
        - This class provides functionality to replace entire lines within
        a HL7 message with a version that is de-identified.
      
* BatchValidator:<BR>
        - This class can validate BATCH segments (FHS, BHS, BTS, FTS) to make sure they follow
        very specific rules needed for a specific project. If another project needs different 
        rules for batch validation, This will have to be modified, or enhanced to be configuration
        driven. (Currently they are hardcoded)      
   * A Batched message must contain all 4 Batching segments - FHS, BHS, BTS, FTS) and they can only 
   be present once.
   * The count of FTS-2 must be 1
   * The count on BTS-2 must be the exact numbers of Messages on the entire batch
   * FHS must be the first segment on the message
   * BHS must be the second segment on the message
   * BTS must be the penultimate segment on the message
   * FTS must be the last segment on the message.
      
      
* FileUtils:<BR>
    Simple class to help read and write files
* CSVReader:<BR>
    Simple class to read comma-delimited files (or any delimiter for that matter)
* ConsoleProgress:<BR>
    Class that shows dots in the console as feedback for the user that the app still working.       
