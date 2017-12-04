# Copyright (c) 2017 Software AG, Darmstadt, Germany and/or its licensors 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
from pysys.basetest import BaseTest
from apama.correlator import CorrelatorHelper

class PySysTest(BaseTest):

	def execute(self):
		import shutil
		for f in ['input.txt']:
			shutil.copyfile(self.input+'/'+f, self.output+'/'+f)
		
		# Can specify which port the correlator runs on using '-X CORR_PORT=15903' on the
		# PySys command line else it will be randomly allocated
		correlator = CorrelatorHelper(self, name='testcorrelator', port=self.CORR_PORT if hasattr(self, 'CORR_PORT') else None)
		correlator.start(arguments=['--connectivityConfig', self.input+'/connectivity.yaml'])
		correlator.receive('received.evt', channels=['received'])
		correlator.injectMonitorscript(self.project.APAMA_HOME+'/monitors/ConnectivityPluginsControl.mon')
		correlator.injectMonitorscript(self.project.APAMA_HOME+'/monitors/ConnectivityPlugins.mon')
		correlator.injectMonitorscript('Test.mon')

		# wait until the output is on disk
		self.waitForSignal('received.evt', expr="final message")
		
	def validate(self):
		self.assertGrep('testcorrelator.out', expr=' (ERROR|FATAL) ', contains=False)
		self.assertDiff('received.evt', 'ref-received.evt')
