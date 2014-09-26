import fileinput
import json
import os
import subprocess
import sys
from xml.dom import minidom


def main(source_path, target_path):
  # Get sdk path by location of 'aidl'.
  p = subprocess.Popen(['which', 'aidl'], stdout=subprocess.PIPE, 
                                          stderr=subprocess.PIPE)
  aidl_path, err = p.communicate()
  sdk_path = os.path.dirname(os.path.dirname(os.path.dirname(aidl_path)))
  if not os.path.isdir(sdk_path):
    return 1
  version_file_path = os.path.join(sdk_path,
      ('extras/google/google_play_services/libproject/'
       'google-play-services_lib/res/values/version.xml'))
  if not os.path.isfile(version_file_path):
    return 1
  xmldoc = minidom.parse(version_file_path)
  version_node = None
  for node in xmldoc.getElementsByTagName('integer'):
    for attr in node.attributes.items():
      if 'google_play_services_version' == attr[1]:
        version_node = node
        break
  if version_node is None:
    return 1

  version = None
  for node in version_node.childNodes:
    if node.TEXT_NODE == node.nodeType:
      version = node.data
      break

  source_file = open(source_path)
  target_file = open(target_path, 'w')
  for line in source_file:
    line = line.replace('_GOOGLE_PLAY_SERVICES_LIB_VERSION_', version)
    target_file.write(line)
  source_file.close()
  target_file.close()
  return 0


if __name__ == '__main__':
  if len(sys.argv) != 3:
    print "ERROR: Please input source path and target path."
    sys.exit(1)
  sys.exit(main(sys.argv[1], sys.argv[2]))
