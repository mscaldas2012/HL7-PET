
v1.2.4.2
- Fixed a bug on creating hierarchy and ignoning unknown segments.

v1.2.4.1
- Added a constructor param for disabling hierarchy on BatchValidator and defaulted to false.

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