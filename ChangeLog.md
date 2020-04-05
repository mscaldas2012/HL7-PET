

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