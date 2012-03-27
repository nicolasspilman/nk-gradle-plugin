/*
 * Copyright 2011 Brian Sletten
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.bosatsu.util.netkernel

import groovy.xml.MarkupBuilder;
import groovy.xml.StreamingMarkupBuilder;
import groovy.xml.XmlUtil;

class ModuleHelper {

   def getModuleInfo(def moduleFile) {
      new XmlSlurper().parse(moduleFile)
   }

   def getModuleArchiveName(def moduleFile) {
      def moduleInfo = getModuleInfo(moduleFile)

      def moduleName = moduleInfo.meta.identity.uri.text()
      def moduleVersion = moduleInfo.meta.identity.version.text()
      def fileName = moduleName.replaceAll(':', '.')

      "${fileName}-${moduleVersion}.jar"
   }

   def updateModuleVersion(def moduleFile, String version) {
      def module = getModuleInfo(moduleFile)
      module.meta.identity.version = version
      StreamingMarkupBuilder builder = new StreamingMarkupBuilder()
      def output = builder.bind {/* mkp.yieldUnescaped*/ mkp.yield module }
      def fileContents = XmlUtil.serialize(output.toString())
      
      // Update module xml with build version
      moduleFile.withWriter { writer ->
         writer.write(fileContents)
      }
   }
}