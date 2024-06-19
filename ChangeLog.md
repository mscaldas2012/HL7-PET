v1.2.9 - 2024-01-30

- DeIdentification: Changed rules from Allowed Values to a "condition" that needs to be met. if an optional conditional statement is present, it will be evaluated to identify if the rule needs to be redacted or not. Condition format supported is : HL7-PATHCOMPARATORVALUE(s) Where: - HL7-PATH is any valid HL7 Path to extract info from message. - Comparator is one of =, !=, IN, !IN - Value(s) is one value or a list of values separated by ; (semicolon)
- Improved the RedactInfo.rule to contain a full description of the redaction. Ex.:
    - Redacted PID-11 with empty value.
    - Redacted PID-13.1 with value 'REDACTED'
    - Redacted PID-3.1 with value 'REDACTED' when PID-3.5 !IN (PI;PT;AN;MB)

v1.2.8 - 2024-01-26
- DeIdentification: Added ability to declare allowed values for a given rule that do not need to be redacted.

v1.2.9 - 2024-01-30
- DeIdentification: Changed rules from Allowed Values to a "condition" that needs to be met.
      if an optional conditional statement is present, it will be evaluated to identify if
      the rule needs to be redacted or not.
      Condition format supported is : 
          HL7-PATH<space>COMPARATOR<space>VALUE(s)
           Where:
              - HL7-PATH is any valid HL7 Path to extract info from message.
              - Comparator is one of =, !=, IN, !IN
              - Value(s) is one value or a list of values separated by ; (semicolon)
- Improved the RedactInfo.rule to contain a full description of the redaction.
    Ex.:
     -  Redacted PID-11 with empty value.
     -  Redacted PID-13.1 with value 'REDACTED'
     -  Redacted PID-3.1 with value 'REDACTED' when PID-3.5 !IN (PI;PT;AN;MB)

v1.2.8 - 2024-01-26
- DeIdentification: Added ability to declare allowed values for a given rule that do not need to be redacted.

v1.2.4.2
- Fixed a bug on creating hierarchy and ignoring unknown segments.

v1.2.4.1
- Added a constructor param for disabling hierarchy on BatchValidator (default to false).

v.1.2.4
- Improved New Line to support \r, \n or both.
- Create Factory method to create Profile object
- Improving Structure Validation to be able to validate Batches.
- Hierarchy building no longer throws exception if does not recognize segment. It simply ignores it.


v.1.2.3.3.
- Disabled hierarchy build-out from Batch, Structure and Rule validation.

v.1.2.3.1
- Added constructor to HL7ParseUtils for non-scala code be able to call it without passing NULL as profile.
- Added getValue(path, removeEmpty) method to keep empty methods if user chooses to (base don transformation requirements)
- Fixed bug to allow Segment Names with Numbers in it (NK1, PV1, etc)

v.1.2.3 - 2020/02/17
- Added ability to handle hierarchy of Segments. Profile is required for HL7ParseUtils.

v1.2.2 - 2019/07/08
- Bug fixes.

v1.2.1 - 2019/07/05
- Added ability to retrieve children segments of a given parent segment (naive method)

v1.1.0 - 2019/03/12
- First Release to maven Central with Parse Utils.