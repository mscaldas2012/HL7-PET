@echo off

if [%1]==[] goto usage

java -jar hl7-utils.jar %1
@echo .
@echo Remember - the new file was created at the same location as the file above.

goto :eof

:usage
@echo Usage: %0 ^<path-to-content-file^> [^<path-to-rules-file^>]
@echo where:
@echo    ^<path-to-content-file^> should be the path to the file you want to remove PII data.
@echo    ^<path-to-rules-file^> (optional) if using a different file for rules, you can
@echo        specify its path as a second parameter
@echo .
@echo Remember - the new file will be created at the same location as the file above.
@Echo (the new file will have the same name with "_deidentified" sufix appended to it.

:eof

exit /B 1



