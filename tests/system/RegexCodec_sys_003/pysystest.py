__pysys_title__   = r""" Configuration error messages and validation """
#                        ================================================================================

__pysys_purpose__ = r""" Demonstrates that the plug-in fails to start with an appropriate error message if the config is invalid.
	"""

__pysys_authors__ = "sample"

# Copyright (c) 2017, 2021 Cumulocity GmbH, Duesseldorf, Germany and/or its affiliates and/or their licensors. 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

from pysys.basetest import BaseTest
from apama.correlator import CorrelatorHelper

class PySysTest(BaseTest):

	testVectors = [
		# (each expected value is a regex, so use * and . to cope with any regex characters like () and [])
		('missing-pattern', "Missing value for 'regex' in config"),
		('bad-pattern', "testChain.*RegexCodec.*Unclosed group"),
		('wrong-type-keys', "Wrong type for List value of 'keys' in config .*Map"),
		('unexpected-config-option', "Found unexpected items in config: .*invalid-config-option"),
	]
	
	def execute(self):
		for t, _ in self.testVectors:
			# Can specify which port the correlator runs on using '-X CORR_PORT=15903' on the
			# PySys command line else it will be randomly allocated
			correlator = CorrelatorHelper(self, name='correlator-'+t, port=self.CORR_PORT if hasattr(self, 'CORR_PORT') else None)
			correlator.start(arguments=['--connectivityConfig', self.input+'/'+t+'.yaml'], 
				waitForServerUp=False, ignoreExitStatus=True)
			correlator.process.wait(30)
		
	def validate(self):
		for t, expected in self.testVectors:
			self.assertGrep('correlator-'+t+'.out', expr=' ERROR .*'+expected, abortOnError=False)
