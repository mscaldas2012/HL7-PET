This project holds several utility classes and applications 
to help with development and data preparation

Here's the list of available tools:<BR>
(More to Come, this file should be updated as new features become available.)

* HL7FileUtils:<BR>
        - this class provides several utilities to handl HL7 Messages
   * A method to split several MSH messages in one file into it's own files.
   * A method to generate clean text files (No HL7 markup) from OBX fields.
   
* DeIdentifier:<BR>
        - This class provides functionality to replace entire lines within
        a HL7 message with a version that is de-identified.
      
* FileUtils:<BR>
    Simple class to help read and write files
* CSVReader:<BR>
    Simple class to read comma-delimited files (or any delimiter for that matter)
* ConsoleProgress:<BR>
    Class that shows dots in the console as feedback for the user that the app still working.       