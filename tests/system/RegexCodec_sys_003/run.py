# Sample PySys testcase
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
