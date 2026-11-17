#!/usr/bin/env python3
"""Check every entry of an APK against its own central directory record.

The warning this repository kept seeing --

    Malformed ZIP entry: lib/armeabi-v7a/libandroidx.graphics.path.so

is apksig's, and apksig says it when it cannot read an entry back: the name,
the compression method, or a size in the entry's local file header disagrees
with what the central directory says about the same entry. Nothing about the
file itself is wrong when that happens; what is wrong is the pair of headers
written around it, which is why the named library changed from build to build
and why removing the library that was named first never helped.

zipalign does not print that message and never has, so a check that greps its
output for it is a check that can never fire. This is the check that reads what
apksig reads: for every entry, the local file header at the offset the central
directory gives, field by field, and for every stored .so, the page boundary
the platform mmaps it at.

Usage: tools/check-apk-entries.py app-release.apk
Exit status is 0 when every entry agrees with itself and 1 when one does not.
"""

import struct
import sys

PAGE = 16384
LOCAL_HEADER = b"PK\x03\x04"
CENTRAL_HEADER = b"PK\x01\x02"
END_OF_CENTRAL = b"PK\x05\x06"


def check(path):
	with open(path, "rb") as apk:
		data = apk.read()
	end = data.rfind(END_OF_CENTRAL)
	if end < 0:
		return ["the file has no end of central directory record"]
	(offset,) = struct.unpack("<I", data[end + 16:end + 20])

	problems = []
	checked = 0
	at = offset
	while data[at:at + 4] == CENTRAL_HEADER:
		record = struct.unpack("<IHHHHHHIIIHHHHHII", data[at:at + 46])
		method = record[4]
		crc = record[7]
		compressed = record[8]
		uncompressed = record[9]
		name_length = record[10]
		extra_length = record[11]
		comment_length = record[12]
		header_offset = record[16]
		name = data[at + 46:at + 46 + name_length].decode("utf-8", "replace")
		at += 46 + name_length + extra_length + comment_length
		checked += 1

		local = data[header_offset:header_offset + 30]
		if len(local) < 30 or local[:4] != LOCAL_HEADER:
			problems.append("%s: no local file header at offset %d" % (name, header_offset))
			continue
		fields = struct.unpack("<IHHHHHIIIHH", local)
		local_method = fields[3]
		local_crc = fields[6]
		local_compressed = fields[7]
		local_uncompressed = fields[8]
		local_name_length = fields[9]
		local_extra_length = fields[10]
		local_name = data[header_offset + 30:header_offset + 30 + local_name_length]
		if local_name != name.encode("utf-8"):
			problems.append("%s: the local file header names %r" % (name, local_name))
			continue
		if local_method != method:
			problems.append("%s: local compression method %d, central directory %d"
				% (name, local_method, method))
			continue
		if local_compressed != compressed:
			problems.append("%s: local compressed size %d, central directory %d"
				% (name, local_compressed, compressed))
			continue
		if local_uncompressed != uncompressed:
			problems.append("%s: local uncompressed size %d, central directory %d"
				% (name, local_uncompressed, uncompressed))
			continue
		if local_crc != crc:
			problems.append("%s: local CRC %08x, central directory %08x" % (name, local_crc, crc))
			continue
		start = header_offset + 30 + local_name_length + local_extra_length
		if start + compressed > len(data):
			problems.append("%s: the entry runs past the end of the file" % name)
			continue
		# A stored library is mapped out of the APK, so the offset of its data
		# has to be a page boundary -- that is what makes it mmap-able on a
		# 16 KB device rather than copied first.
		if name.startswith("lib/") and name.endswith(".so") and method == 0 and start % PAGE:
			problems.append("%s: stored at offset %d, which is %d bytes past a %d byte page boundary"
				% (name, start, start % PAGE, PAGE))

	return checked, problems


def main(path):
	result = check(path)
	if isinstance(result, list):
		for problem in result:
			print("MALFORMED " + problem)
		return 1
	checked, problems = result
	print("checked %d entries in %s" % (checked, path))
	if not problems:
		print("Every local file header agrees with its central directory record.")
		return 0
	for problem in problems:
		print("MALFORMED " + problem)
	return 1


if __name__ == "__main__":
	if len(sys.argv) != 2:
		print(__doc__)
		sys.exit(2)
	sys.exit(main(sys.argv[1]))
