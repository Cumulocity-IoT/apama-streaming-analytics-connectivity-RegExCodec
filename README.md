# apama-streaming-analytics-connectivity-RegExCodec
Java based Connectivity Codec for performing regular expression operations on messages for use with [Apama](http://www.apamacommunity.com/).

## Description
Applies a regular expression on the given event map data. For more information on the Apama Connectivity Framework, as well as Apama in general, please see [the community website](http://www.apamacommunity.com/). Furthermore, if you wish to examine this plugin in more detail, [a blog describing it also exists](http://www.apamacommunity.com/creating-your-own-regex-plug-in/).

## Set-up
First, ensure you have an install of the Apama engine; a free edition is available at [the community website](http://www.apamacommunity.com/). This plugin assumes the user has familiarity with the basic structure of the install, more information of which can also be found on the community site.

Running and building of the sample requires access to the Correlator and Apama command line tools.

To ensure that the environment is configured correctly for Apama, all the commands below should be executed from an Apama Command Prompt, or from a shell or command prompt where the bin\apama_env script has been run (or sourced on Unix).

### To build
The RegEx codec is most easily built with the Apache ANT tool from the directory containing 'build.xml':

> ant
 
But if you do not have access to ANT, it will need to be built manually:

For Linux:
> mkdir build_output
> javac -cp $APAMA_HOME/lib/connectivity-plugins-api.jar -d build_output src/com/softwareag/samples/*.java
> jar -cf build_output/regex-codec.jar -C build_output .
> cp build_output/regex-codec.jar $APAMA_WORK/lib/

For Windows:
> mkdir build_output
> javac -cp %APAMA_HOME%/lib/connectivity-plugins-api.jar -d build_output src/com/softwareag/samples/*.java
> jar -cf build_output/regex-codec.jar -C build_output .
> copy build_output\regex-codec.jar %APAMA_WORK%\lib\
  
A successful build will produce output files for the RegEx codec:

	build-output/regex-codec.jar

These should have already been copied to APAMA_WORK/lib where the correlator will load them from.

To run the sample, you will also need to have built the [File Transport](https://github.com/SoftwareAG/apama-streaming-analytics-connectivity-FileTransport) to create a full connectivity chain.

## Running the sample
You can either run the sample via the [Pysys](https://sourceforge.net/projects/pysys/files/pysys/) framework by invoking the tests, or by passing the yaml Connectivity configuration file to the Correlator.

When run, the sample creates a connectivity plugin chain.  The chain will have the correlator at one end and to access the 'ouside world', a plugin chain must end with a Transport.  For this sample we use the File Transport which can read in data from a file to be passed towards the host correlator, or write data out to a file that has come from the host correlator.  Between the File Transport and the correlator is the RegEx Plugin which uses regular expressions to perform replacements on payload fields.

To run via [Pysys](https://sourceforge.net/projects/pysys/files/pysys/), go to the tests directory and invoke the command: 
  
  pysys run

You can then inspect the output within the individual tests output directory

Should you wish to run directly

1. Use example files found in the test directory RegEx:

> cd RegEx/tests/system/RegexCodec_sys_001/Input

2. Start the Apama Correlator specifying the connectivity config file

> correlator --connectivityConfig connectivity.yaml

3. Inject the Connectivity plugins support monitor:

> (Linux) engine_inject $APAMA_HOME/monitors/ConnectivityPluginsControl.mon $APAMA_HOME/monitors/ConnectivityPlugins.mon

> (Windows) engine_inject %APAMA_HOME%/monitors/ConnectivityPluginsControl.mon %APAMA_HOME%/monitors/ConnectivityPlugins.mon

4. Inject the Test monitor:

> engine_inject Test.mon

The connectivity.yaml file describes the chain of plugins that this test uses and the regular expression and replacement to be applied.

### Sample Output

The EPL application is sending out events to the connectivity chain, which the correlator presents as maps to the chain. The RegEx codec sees these expressions and applies them as transforms on the payload strings.

Running the sample will produce one output file:

	output.txt

When compared to input.txt, you'll notice that any numerical value within that was replaced with the string '<number>'. This transformation was defined in the .yaml file. If you run the test via the pysys framework, you can see the Apama events themselves, showing the input and the output value together. 
 
# Authors
[Callum Attryde](mailto:Callum.Attryde@softwareag.com)

[John Heath](mailto:John.Heath@softwareag.com)

## License
Copyright (c) 2017-2019 Software AG, Darmstadt, Germany and/or its licensors

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
file except in compliance with the License. You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the
License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. 
See the License for the specific language governing permissions and limitations under the License.

______________________
These tools are provided as-is and without warranty or support. They do not constitute part of the Software AG product suite. Users are free to use, fork and modify them, subject to the license agreement. While Software AG welcomes contributions, we cannot guarantee to include every contribution in the master project.
_____________
Contact us at [TECHcommunity](mailto:technologycommunity@softwareag.com?subject=Github/SoftwareAG) if you have any questions.
